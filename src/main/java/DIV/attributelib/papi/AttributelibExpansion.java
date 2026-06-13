package DIV.attributelib.papi;

import DIV.attributelib.api.AttributeType;
import DIV.attributelib.core.AttributeEngine;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PlaceholderAPI 連携(PAPI 導入時のみ登録される)。
 * スコアボード・TAB・ホログラム等の表示系プラグインから属性値を参照できる。
 *
 * <pre>
 * %attributelib_&lt;属性&gt;%        最終値(％表示属性は「25%」形式、それ以外は #.##)
 * %attributelib_raw_&lt;属性&gt;%    最終値の生の数値
 * %attributelib_base_&lt;属性&gt;%   基礎値
 * %attributelib_cond_&lt;条件&gt;%   条件の成否("true"/"false")
 * </pre>
 *
 * 属性・条件の指定は {@code ns:id} 形式、または名前空間省略
 * (attributelib → minecraft の順で解決。例: {@code crit_chance}、{@code max_health})。
 *
 * <p><b>スレッド対応</b>: TAB などはプレースホルダーを非同期スレッドから更新するが、
 * attributelib はメインスレッド専用。そこで非同期からの要求にはキャッシュ値を即返し、
 * 値が古い({@link #REFRESH_INTERVAL_MS} 超過)ときだけメインスレッドへ再計算を予約する。
 * 同一キーの予約は1つに重複排除されるため、外部表示プラグインが高頻度ポーリングしても
 * メインスレッドのタスクが氾濫しない。{@code params} は外部入力なので、暴走時の
 * バックストップとしてキャッシュ件数に上限を設ける。</p>
 */
public final class AttributelibExpansion extends PlaceholderExpansion implements Listener {

    private static final DecimalFormat FORMAT =
            new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ROOT));

    /** 同一キーの再計算は最短この間隔(ms)。表示プラグインに秒未満の即時性は不要。 */
    private static final long REFRESH_INTERVAL_MS = 1000L;
    /** キャッシュ件数の上限(外部 params 暴走時のバックストップ。通常到達しない)。 */
    private static final int MAX_ENTRIES = 10_000;

    /** value は null 可(「既知だが該当属性/条件なし」= PAPI に原文表示させる)。 */
    private record Cached(String value, long updatedAtMs) {
    }

    private final Plugin plugin;
    private final AttributeEngine engine;

    /** キャッシュ。キー: "uuid\0params"(非同期読み・メイン書き)。 */
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();
    /** 再計算予約中のキー(タスク重複防止)。 */
    private final Set<String> pending = ConcurrentHashMap.newKeySet();

    public AttributelibExpansion(Plugin plugin, AttributeEngine engine) {
        this.plugin = plugin;
        this.engine = engine;
    }

    @Override
    public String getIdentifier() {
        return "attributelib";
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // PAPI の /papi reload で登録解除されない
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, String params) {
        if (!(offlinePlayer instanceof Player player) || !player.isOnline()) {
            return "";
        }
        String key = player.getUniqueId() + "\0" + params;
        if (Bukkit.isPrimaryThread()) {
            String value = compute(player, params);
            store(key, value);
            return value; // 未知なら null(PAPI が原文表示)
        }
        // 非同期(TAB 等): キャッシュを即返し、古ければ最大1件だけ再計算を予約する
        Cached cached = cache.get(key);
        if (cached == null || nowMs() - cached.updatedAtMs() >= REFRESH_INTERVAL_MS) {
            scheduleRefresh(player, params, key);
        }
        return cached != null ? cached.value() : "";
    }

    private void scheduleRefresh(Player player, String params, String key) {
        if (!pending.add(key)) {
            return; // 同キーの再計算が既に在庫。タスク氾濫を防ぐ
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                if (player.isOnline()) {
                    store(key, compute(player, params));
                }
            } finally {
                pending.remove(key);
            }
        });
    }

    private void store(String key, String value) {
        if (cache.size() >= MAX_ENTRIES && !cache.containsKey(key)) {
            cache.clear(); // 外部 params 暴走時のバックストップ(通常到達しない)
        }
        cache.put(key, new Cached(value, nowMs()));
    }

    private static long nowMs() {
        return System.currentTimeMillis();
    }

    /** メインスレッドでの実計算。未知のプレースホルダーは null(PAPI が原文表示する)。 */
    private String compute(Player player, String params) {
        if (params.startsWith("cond_")) {
            NamespacedKey key = resolveKey(params.substring("cond_".length()));
            if (key == null) {
                return null;
            }
            return String.valueOf(engine.conditions().byKey(key) != null
                    && engine.conditions().isActive(key, player));
        }
        if (params.startsWith("raw_")) {
            AttributeType type = resolveType(params.substring("raw_".length()));
            return type == null ? null : String.valueOf(engine.get(player, type));
        }
        if (params.startsWith("base_")) {
            AttributeType type = resolveType(params.substring("base_".length()));
            return type == null ? null : format(type, engine.getBase(player, type));
        }
        AttributeType type = resolveType(params);
        return type == null ? null : format(type, engine.get(player, type));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        String prefix = event.getPlayer().getUniqueId() + "\0";
        cache.keySet().removeIf(key -> key.startsWith(prefix));
        pending.removeIf(key -> key.startsWith(prefix));
    }

    private String format(AttributeType type, double value) {
        return type.percentDisplay() ? FORMAT.format(value * 100) + "%" : FORMAT.format(value);
    }

    private AttributeType resolveType(String name) {
        NamespacedKey key = resolveKey(name);
        if (key == null) {
            return null;
        }
        AttributeType type = engine.byKey(key);
        if (type == null && !name.contains(":")) {
            type = engine.byKey(NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT)));
        }
        return type;
    }

    private NamespacedKey resolveKey(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.contains(":")
                ? NamespacedKey.fromString(lower)
                : NamespacedKey.fromString("attributelib:" + lower);
    }
}
