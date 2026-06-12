package DIV.attributelib.core;

import DIV.attributelib.api.AttributeType;
import DIV.attributelib.api.ModifierHandle;
import DIV.attributelib.api.Operation;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 属性レジストリ + エンティティ状態キャッシュ(attributelib の内部中枢)。
 * 利用側プラグインは {@link DIV.attributelib.api.Attributes} ファサードを使うこと。
 */
public final class AttributeEngine {

    private final Plugin plugin;
    private final NamespacedKey baseKey;
    private final NamespacedKey modifiersKey;
    private final Map<NamespacedKey, AttributeType> registry = new LinkedHashMap<>();
    private final Map<UUID, EntityAttributes> entities = new HashMap<>();

    public AttributeEngine(Plugin plugin) {
        this.plugin = plugin;
        this.baseKey = new NamespacedKey(plugin, "base");
        this.modifiersKey = new NamespacedKey(plugin, "modifiers");
    }

    /** メインスレッド以外からの呼び出しを即座に拒否する。中途半端な同期はしない方針。 */
    public static void checkMainThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("attributelib はメインスレッド専用です");
        }
    }

    public AttributeType register(Plugin owner, String id, double defaultValue, double min, double max) {
        return register(owner, id, defaultValue, min, max, null, false);
    }

    public AttributeType register(Plugin owner, String id, double defaultValue, double min, double max,
                                  net.kyori.adventure.text.Component displayName, boolean percentDisplay) {
        checkMainThread();
        NamespacedKey key = new NamespacedKey(owner, id);
        AttributeType type = new AttributeType(key, defaultValue, min, max, displayName, percentDisplay);
        AttributeType existing = registry.putIfAbsent(key, type);
        if (existing != null) {
            throw new IllegalStateException("属性が二重登録されました: " + key);
        }
        plugin.getLogger().info("属性を登録: " + key + " (default=" + defaultValue
                + (min != -Double.MAX_VALUE || max != Double.MAX_VALUE ? ", range=[" + min + ", " + max + "]" : "")
                + ")");
        return type;
    }

    /** 登録済み属性を ID で引く。未登録なら null。 */
    public AttributeType byKey(NamespacedKey key) {
        return registry.get(key);
    }

    /** 登録済み属性の一覧(登録順)。 */
    public Collection<AttributeType> registered() {
        return Collections.unmodifiableCollection(registry.values());
    }

    // ---- エンティティ操作(ファサード/コマンドの入口) ----

    /**
     * ホットパス用の事前判定: このエンティティが attributelib のデータ
     * (メモリ状態 or PDC)を一切持たなければ false。
     * 属性状態を生成せず、NBT のキー存在確認だけで答えるため、
     * 「無関係なバニラ mob 同士の戦闘」をゼロコストに近く素通しできる。
     * false のとき全属性は定義のデフォルト値に等しい。
     */
    public boolean hasData(LivingEntity entity) {
        if (entities.containsKey(entity.getUniqueId())) {
            return true;
        }
        var pdc = entity.getPersistentDataContainer();
        return pdc.has(baseKey) || pdc.has(modifiersKey);
    }

    public double get(LivingEntity entity, AttributeType type) {
        return of(entity).get(type);
    }

    public double getBase(LivingEntity entity, AttributeType type) {
        return of(entity).getBase(type);
    }

    public void setBase(LivingEntity entity, AttributeType type, double value) {
        of(entity).setBase(type, value);
    }

    public void resetBase(LivingEntity entity, AttributeType type) {
        of(entity).resetBase(type);
    }

    /**
     * モディファイアを付与する。
     *
     * @param durationTicks 0 以下なら無期限。正なら対象ワールドの gameTime 基準で
     *                      durationTicks 後に失効する(遅延判定なのでスケジューラ不要・再起動安全)
     * @param persistent    true なら PDC に保存され再起動を跨ぐ。false はメモリのみ
     */
    public ModifierHandle add(LivingEntity entity, AttributeType type, String sourceId,
                              Operation operation, double value, long durationTicks, boolean persistent) {
        EntityAttributes state = of(entity);
        long expiresAt = durationTicks <= 0
                ? Modifier.PERMANENT
                : entity.getWorld().getGameTime() + durationTicks;
        return state.add(new Modifier(type.key(), sourceId, operation, value, expiresAt, persistent));
    }

    public void removeBySource(LivingEntity entity, String sourceId) {
        of(entity).removeBySource(sourceId);
    }

    /** デバッグ用: エンティティに明示設定されている base のコピー。 */
    public Map<NamespacedKey, Double> baseView(LivingEntity entity) {
        return of(entity).baseView();
    }

    /** デバッグ用: エンティティの有効モディファイア一覧のコピー。 */
    public List<Modifier> modifierView(LivingEntity entity) {
        return of(entity).modifierView();
    }

    private EntityAttributes of(LivingEntity entity) {
        checkMainThread();
        EntityAttributes state = entities.get(entity.getUniqueId());
        if (state == null || state.entity() != entity) {
            // 同一UUIDでもチャンク再ロード後は別インスタンスになる。
            // 古い参照に書いてもエンティティ本体に届かないため、必ず作り直す
            // (永続データは PDC に書き込み済みなので、ここで失われるのは transient のみ = 仕様通り)。
            state = new EntityAttributes(entity, baseKey, modifiersKey, plugin.getLogger());
            entities.put(entity.getUniqueId(), state);
        }
        return state;
    }

    public void evict(UUID entityId) {
        entities.remove(entityId);
    }

    public void clearCache() {
        entities.clear();
    }

    /** デバッグ用: 現在キャッシュしているエンティティ数。 */
    public int cachedEntityCount() {
        return entities.size();
    }
}
