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
 * attributelib はメインスレッド専用。そこで非同期からの要求には前回のスナップショットを
 * 即返し、メインスレッドへ再計算タスクを1個予約する(次回ポーリングで最新値になる)。</p>
 */
public final class AttributelibExpansion extends PlaceholderExpansion implements Listener {

    private static final DecimalFormat FORMAT =
            new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ROOT));

    private final Plugin plugin;
    private final AttributeEngine engine;

    /** 非同期要求用スナップショット。キー: "uuid\0params"(非同期読み・メイン書き)。 */
    private final Map<String, String> snapshot = new ConcurrentHashMap<>();

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
        if (Bukkit.isPrimaryThread()) {
            return compute(player, params);
        }
        // 非同期(TAB 等): メインスレッドで再計算を予約し、前回値を即返す
        String key = player.getUniqueId() + "\0" + params;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                String value = compute(player, params);
                snapshot.put(key, value != null ? value : "");
            }
        });
        return snapshot.getOrDefault(key, "");
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
        snapshot.keySet().removeIf(key -> key.startsWith(prefix));
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
