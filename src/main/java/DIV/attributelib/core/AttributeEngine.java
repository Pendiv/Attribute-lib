package DIV.attributelib.core;

import DIV.attributelib.api.AttributeType;
import DIV.attributelib.api.ModifierHandle;
import DIV.attributelib.api.Operation;
import DIV.attributelib.api.VanillaAttributes;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 属性レジストリ + エンティティ状態キャッシュ(attributelib の内部中枢)。
 * 利用側プラグインは {@link DIV.attributelib.api.Attributes} ファサードを使うこと。
 */
public final class AttributeEngine {

    /** ブリッジがバニラ側に書き込むモディファイアのキー(エンティティ×属性ごとに1個)。 */
    public static final NamespacedKey BRIDGE_KEY = new NamespacedKey("attributelib", "bridge");

    private final Plugin plugin;
    private final NamespacedKey baseKey;
    private final NamespacedKey modifiersKey;
    private final Map<NamespacedKey, AttributeType> registry = new LinkedHashMap<>();
    private final Map<UUID, EntityAttributes> entities = new HashMap<>();

    /** ブリッジ属性: 属性キー → バニラ Attribute。 */
    private final Map<NamespacedKey, Attribute> bridged = new HashMap<>();
    /** 時限ブリッジの失効タスク(エンティティごと最大1個、期限ちょうどに1回だけ実行)。 */
    private final Map<UUID, Arm> arms = new HashMap<>();

    private final ConditionRegistry conditions;

    /**
     * 条件付きブリッジモディファイアを持つエンティティ。条件の成立/不成立は時間経過で
     * 変わり、バニラ値は能動的に書き戻す必要があるため、この集合が空でない間だけ
     * 周期タスク({@link #CONDITION_REFRESH_PERIOD} tick)で再評価する。
     * 空になればタスクは自動停止する。
     */
    private final Map<UUID, EntityAttributes> conditionalBridgeEntities = new HashMap<>();
    private BukkitTask conditionRefreshTask;
    private static final long CONDITION_REFRESH_PERIOD = 20L;

    private record Arm(long deadline, BukkitTask task) {
    }

    public AttributeEngine(Plugin plugin) {
        this.plugin = plugin;
        this.baseKey = new NamespacedKey(plugin, "base");
        this.modifiersKey = new NamespacedKey(plugin, "modifiers");
        this.conditions = new ConditionRegistry(plugin.getLogger());
    }

    /** 条件レジストリ(ファサード Conditions の実体)。 */
    public ConditionRegistry conditions() {
        return conditions;
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

    // ---- バニラブリッジ ----

    /**
     * 全バニラ属性をブリッジ属性として登録する(onEnable で1回)。
     * ブリッジ属性はカスタム属性と同じ API(モディファイア・時限・PDC 永続化)で操作でき、
     * 合成結果が transient なバニラモディファイア({@link #BRIDGE_KEY})として反映される。
     * transient なのでバニラ NBT に残らず、ロード時の再適用で復元される(ゴースト強化なし)。
     */
    public void registerBridges() {
        checkMainThread();
        int count = 0;
        for (Attribute attribute : io.papermc.paper.registry.RegistryAccess.registryAccess()
                .getRegistry(io.papermc.paper.registry.RegistryKey.ATTRIBUTE)) {
            NamespacedKey key = attribute.getKey();
            if (registry.containsKey(key)) {
                continue;
            }
            // 表示名はバニラの翻訳キー(クライアント言語で表示)。範囲はバニラ側が
            // AttributeInstance 内部でクランプするため、こちらでは制限しない
            AttributeType type = new AttributeType(key, attribute.getDefaultValue(),
                    -Double.MAX_VALUE, Double.MAX_VALUE, Component.translatable(attribute), false);
            registry.put(key, type);
            bridged.put(key, attribute);
            count++;
        }
        plugin.getLogger().info("バニラ属性をブリッジ登録: " + count + " 件");
    }

    /** この属性キーがバニラブリッジか。 */
    public boolean isBridged(NamespacedKey key) {
        return bridged.containsKey(key);
    }

    /** バニラ Attribute に対応するブリッジ属性。{@link #registerBridges} 後に有効。 */
    public AttributeType bridgedType(Attribute attribute) {
        AttributeType type = registry.get(attribute.getKey());
        if (type == null) {
            throw new IllegalStateException("ブリッジ未登録です(attributelib の onEnable 前): " + attribute.getKey());
        }
        return type;
    }

    /**
     * 状態変更通知(EntityAttributes の全変更経路から呼ばれる)。
     * ブリッジ属性ならバニラ側へ合成結果を書き込む。
     */
    void onStateChanged(EntityAttributes state, NamespacedKey attributeKey) {
        Attribute attribute = bridged.get(attributeKey);
        if (attribute != null) {
            pushBridge(state, attributeKey, attribute);
        }
    }

    private void pushBridge(EntityAttributes state, NamespacedKey key, Attribute attribute) {
        LivingEntity entity = state.entity();
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return; // このエンティティが持たない属性
        }
        Double target = state.computeBridged(registry.get(key), instance.getBaseValue());
        double delta = target == null ? 0 : target - instance.getBaseValue();

        // 現在書き込まれている値と同じなら何もしない(周期再評価の無駄な書き換えを防ぐ)
        AttributeModifier current = null;
        for (AttributeModifier modifier : instance.getModifiers()) {
            if (modifier.getKey().equals(BRIDGE_KEY)) {
                current = modifier;
                break;
            }
        }
        if (delta == 0) {
            if (current != null) {
                instance.removeModifier(current);
            }
        } else if (current == null || current.getAmount() != delta) {
            if (current != null) {
                instance.removeModifier(current);
            }
            instance.addTransientModifier(new AttributeModifier(BRIDGE_KEY, delta,
                    AttributeModifier.Operation.ADD_NUMBER));
        }
    }

    private void trackConditionalBridge(EntityAttributes state) {
        conditionalBridgeEntities.put(state.entity().getUniqueId(), state);
        if (conditionRefreshTask == null) {
            conditionRefreshTask = Bukkit.getScheduler().runTaskTimer(plugin,
                    this::refreshConditionalBridges, CONDITION_REFRESH_PERIOD, CONDITION_REFRESH_PERIOD);
        }
    }

    /**
     * 条件付きブリッジの再評価(周期タスク本体。テストから直接呼んでもよい)。
     * 対象が空になったらタスクごと止まる。
     */
    public void refreshConditionalBridges() {
        if (conditionalBridgeEntities.isEmpty()) {
            if (conditionRefreshTask != null) {
                conditionRefreshTask.cancel();
                conditionRefreshTask = null;
            }
            return;
        }
        for (EntityAttributes state : List.copyOf(conditionalBridgeEntities.values())) {
            for (NamespacedKey key : state.attributeKeysWithData()) {
                Attribute attribute = bridged.get(key);
                if (attribute != null && state.hasConditional(key)) {
                    pushBridge(state, key, attribute);
                }
            }
        }
    }

    /**
     * 時限ブリッジの失効タスクを(必要なら)仕掛ける。
     * 遅延読み取りだけでは「誰も読まなくてもゲームに効いている」バニラ値を
     * 戻せないため、失効期限ちょうどに1回だけタスクを走らせる(毎tick監視はしない)。
     */
    private void armIfNeeded(EntityAttributes state) {
        long deadline = state.nearestExpiryFor(this::isBridged);
        if (deadline == Long.MAX_VALUE) {
            return;
        }
        LivingEntity entity = state.entity();
        UUID id = entity.getUniqueId();
        Arm existing = arms.get(id);
        if (existing != null && existing.deadline() <= deadline) {
            return; // 既により早い(または同時刻の)タスクがある
        }
        if (existing != null) {
            existing.task().cancel();
        }
        // +1 補正: スケジューラは tick 内でワールド時間の進行より先に走るため、
        // delay=N のタスク実行時点では gameTime が N-1 しか進んでいない(実測)。
        // 補正しないと期限と同じ gameTime で発火してスイープが空振りする
        long delay = Math.max(1, deadline - entity.getWorld().getGameTime() + 1);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> fireArm(id), delay);
        arms.put(id, new Arm(deadline, task));
    }

    private void fireArm(UUID id) {
        arms.remove(id);
        EntityAttributes state = entities.get(id);
        if (state == null) {
            return;
        }
        // 注意: entity.isValid() では弾かない。プレイヤー不在の遅延スポーンチャンクでは
        // 生きたエンティティでも false になる(実測)。除去済みエンティティは
        // EntityRemoveEvent → evict → cancelArm でタスクごと消えるため、ここに来ない
        // (除去→再追加のサイクルでは onAddToWorld → touch → 再武装で自己修復する)
        state.sweep(); // 失効除去 → onStateChanged 経由でバニラ側へ反映される
        armIfNeeded(state); // まだ時限ブリッジが残っていれば次の期限で再武装
    }

    private void cancelArm(UUID id) {
        Arm arm = arms.remove(id);
        if (arm != null) {
            arm.task().cancel();
        }
    }

    /** onDisable 用: 書き込んだバニラモディファイアを掃除して全状態を破棄する。 */
    public void shutdown() {
        for (EntityAttributes state : entities.values()) {
            for (NamespacedKey key : state.attributeKeysWithData()) {
                Attribute attribute = bridged.get(key);
                if (attribute != null) {
                    VanillaAttributes.remove(state.entity(), attribute, BRIDGE_KEY);
                }
            }
        }
        arms.values().forEach(arm -> arm.task().cancel());
        arms.clear();
        conditionalBridgeEntities.clear();
        if (conditionRefreshTask != null) {
            conditionRefreshTask.cancel();
            conditionRefreshTask = null;
        }
        entities.clear();
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

    /** 公開エントリの引数を境界で検証する(深い場所での生 NPE を防ぐ)。 */
    private static void require(LivingEntity entity, AttributeType type) {
        Objects.requireNonNull(entity, "entity が null です");
        Objects.requireNonNull(type, "type が null です");
    }

    public double get(LivingEntity entity, AttributeType type) {
        require(entity, type);
        Attribute attribute = bridged.get(type.key());
        if (attribute != null) {
            // ブリッジの最終値の真実はバニラ側(他プラグインのモディファイアも合成済み)
            AttributeInstance instance = entity.getAttribute(attribute);
            return instance != null ? instance.getValue() : type.defaultValue();
        }
        return of(entity).get(type);
    }

    public double getBase(LivingEntity entity, AttributeType type) {
        require(entity, type);
        Attribute attribute = bridged.get(type.key());
        if (attribute != null && !of(entity).hasExplicitBase(type)) {
            AttributeInstance instance = entity.getAttribute(attribute);
            return instance != null ? instance.getBaseValue() : type.defaultValue();
        }
        return of(entity).getBase(type);
    }

    public void setBase(LivingEntity entity, AttributeType type, double value) {
        require(entity, type);
        of(entity).setBase(type, value);
    }

    public void resetBase(LivingEntity entity, AttributeType type) {
        require(entity, type);
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
        return add(entity, type, sourceId, operation, value, durationTicks, persistent, null);
    }

    public ModifierHandle add(LivingEntity entity, AttributeType type, String sourceId,
                              Operation operation, double value, long durationTicks, boolean persistent,
                              NamespacedKey condition) {
        require(entity, type);
        // operation / sourceId / value は Modifier コンストラクタが境界で検証する
        EntityAttributes state = of(entity);
        long expiresAt = durationTicks <= 0
                ? Modifier.PERMANENT
                : entity.getWorld().getGameTime() + durationTicks;
        ModifierHandle handle = state.add(
                new Modifier(type.key(), sourceId, operation, value, expiresAt, persistent, condition));
        if (isBridged(type.key())) {
            if (expiresAt != Modifier.PERMANENT) {
                armIfNeeded(state); // 時限ブリッジは失効時刻に能動的に書き戻す必要がある
            }
            if (condition != null) {
                trackConditionalBridge(state); // 条件付きブリッジは周期再評価の対象
            }
        }
        return handle;
    }

    public void removeBySource(LivingEntity entity, String sourceId) {
        Objects.requireNonNull(entity, "entity が null です");
        if (sourceId == null) {
            return; // 何も一致しない = no-op(無害)
        }
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
            state = new EntityAttributes(this, entity, baseKey, modifiersKey, plugin.getLogger());
            entities.put(entity.getUniqueId(), state);
            // ロード直後の一括反映: 永続データにブリッジ属性があればバニラ側へ書き戻す。
            // さらに「データは無いのに bridge モディファイアが残っている」属性も掃除する
            // (エンティティ本体が生きたまま状態だけ再構築されるケースで、再構築中に
            //  期限切れになった時限ブリッジを load() が捨てると孤児が残るため。実バグの修正)
            java.util.Set<NamespacedKey> dataKeys = state.attributeKeysWithData();
            for (Map.Entry<NamespacedKey, Attribute> entry : bridged.entrySet()) {
                AttributeInstance instance = entity.getAttribute(entry.getValue());
                if (instance == null) {
                    continue;
                }
                boolean needsPush = dataKeys.contains(entry.getKey());
                if (!needsPush) {
                    for (AttributeModifier modifier : instance.getModifiers()) {
                        if (modifier.getKey().equals(BRIDGE_KEY)) {
                            needsPush = true; // 孤児: push が delta 0 として除去する
                            break;
                        }
                    }
                }
                if (needsPush) {
                    pushBridge(state, entry.getKey(), entry.getValue());
                }
                // 永続データに条件付きブリッジがあれば周期再評価の対象に戻す
                if (dataKeys.contains(entry.getKey()) && state.hasConditional(entry.getKey())) {
                    trackConditionalBridge(state);
                }
            }
            armIfNeeded(state);
        }
        return state;
    }

    /** 状態を(必要なら)構築する。ロード時のブリッジ復元はこの構築に伴って自動実行される。 */
    public void touch(LivingEntity entity) {
        of(entity);
    }

    public void evict(UUID entityId) {
        entities.remove(entityId);
        conditionalBridgeEntities.remove(entityId);
        cancelArm(entityId);
    }

    public void clearCache() {
        shutdown();
    }

    /** デバッグ用: 現在キャッシュしているエンティティ数。 */
    public int cachedEntityCount() {
        return entities.size();
    }
}
