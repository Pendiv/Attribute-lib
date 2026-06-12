package DIV.attributelib.api;

import DIV.attributelib.Attributelib;
import DIV.attributelib.core.ConditionRegistry;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * 適用条件の登録・参照 API。モディファイアに条件を付けると、成立している間だけ効く:
 *
 * <pre>{@code
 * // 夜間のみ攻撃力+3(バニラ属性ブリッジでも同じ)
 * Attributes.add(mob, Vanilla.ATTACK_DAMAGE, "myplugin:trait/nocturnal",
 *         Operation.ADD, 3, Conditions.NIGHT);
 *
 * // 水中のみ採掘速度2倍のツルハシ(lore に「(水中)」が自動表示される)
 * ItemAttributes.add(pickaxe, Vanilla.SUBMERGED_MINING_SPEED,
 *         Operation.MULTIPLY, 2.0, EquipmentSlotGroup.MAINHAND, Conditions.IN_WATER);
 *
 * // 独自条件も1行(キーだけが保存され、評価時に引き直される)
 * Condition BOSS_NEARBY = Conditions.register(this, "boss_nearby",
 *         Component.text("ボス接近中"), e -> ...);
 * }</pre>
 *
 * <p>カスタム属性は読み取りの瞬間に評価される。バニラブリッジは条件の反転を
 * 周期タスク(条件付きブリッジを持つエンティティが存在する間だけ稼働)が反映する。</p>
 */
public final class Conditions {

    /** 昼間(ワールドの昼)。 */
    public static Condition DAY;
    /** 夜間。 */
    public static Condition NIGHT;
    /** 水に触れている。 */
    public static Condition IN_WATER;
    /** 水没している(頭まで水中)。 */
    public static Condition UNDERWATER;
    /** 溶岩に触れている。 */
    public static Condition IN_LAVA;
    /** 雨に濡れている。 */
    public static Condition IN_RAIN;
    /** 燃えている。 */
    public static Condition BURNING;
    /** オーバーワールドにいる。 */
    public static Condition IN_OVERWORLD;
    /** ネザーにいる。 */
    public static Condition IN_NETHER;
    /** ジ・エンドにいる。 */
    public static Condition IN_THE_END;
    /** スニーク中。 */
    public static Condition SNEAKING;
    /** 接地している。 */
    public static Condition ON_GROUND;
    /** 体力が満タン。 */
    public static Condition FULL_HEALTH;

    private Conditions() {
    }

    private static ConditionRegistry registry() {
        Attributelib plugin = Attributelib.instance();
        if (plugin == null) {
            throw new IllegalStateException(
                    "attributelib がまだロードされていません。plugin.yml の depend に attributelib を追加してください");
        }
        return plugin.conditions();
    }

    /**
     * 条件を登録する。述語はメインスレッドで(ダメージ計算などの最中に)呼ばれるため、
     * 軽い判定だけを書くこと。
     */
    public static Condition register(Plugin plugin, String id, Component displayName,
                                     Predicate<LivingEntity> test) {
        return registry().register(plugin, id, displayName, test);
    }

    /** 登録済み条件をキーで引く。未登録なら null。 */
    public static Condition byKey(NamespacedKey key) {
        return registry().byKey(key);
    }

    /** 登録済み条件の一覧(登録順)。 */
    public static Collection<Condition> registered() {
        return registry().registered();
    }

    // ---- エフェクト条件 ----

    /**
     * 「このポーション効果を持っている」条件。全エフェクト分が起動時に
     * {@code attributelib:effect/<名前>} のキーで自動登録されている。
     *
     * <pre>{@code
     * // 毒状態の間だけ被ダメ+50%
     * Attributes.add(mob, StandardAttributes.DAMAGE_TAKEN, "myplugin:trait/frail",
     *         Operation.ADD, 0.5, Conditions.hasEffect(PotionEffectType.POISON));
     * }</pre>
     */
    public static Condition hasEffect(PotionEffectType effect) {
        Condition condition = byKey(effectConditionKey(effect));
        if (condition == null) {
            throw new IllegalStateException("エフェクト条件が未登録です(attributelib の onEnable 前?): " + effect.getKey());
        }
        return condition;
    }

    private static NamespacedKey effectConditionKey(PotionEffectType effect) {
        NamespacedKey key = effect.getKey();
        // バニラは effect/speed、他名前空間は effect/<ns>.<key> で衝突を避ける
        String id = key.getNamespace().equals(NamespacedKey.MINECRAFT)
                ? "effect/" + key.getKey()
                : "effect/" + key.getNamespace() + "." + key.getKey();
        return new NamespacedKey("attributelib", id.toLowerCase(Locale.ROOT));
    }

    // ---- 合成(AND / OR / NOT) ----

    /**
     * 全条件が成立しているときだけ成立する合成条件を登録する。
     * 合成も独立した条件としてキーを持つため、PDC 永続化・コマンド・lore 表示が全て効く。
     *
     * <pre>{@code
     * Condition NETHER_BURNING = Conditions.allOf(this, "nether_burning",
     *         Conditions.IN_NETHER, Conditions.BURNING);
     * }</pre>
     */
    public static Condition allOf(Plugin plugin, String id, Condition... conditions) {
        requireComponents(conditions);
        return register(plugin, id, joinNames("+", conditions),
                entity -> Arrays.stream(conditions).allMatch(c -> c.test().test(entity)));
    }

    /** いずれかの条件が成立しているときに成立する合成条件を登録する。 */
    public static Condition anyOf(Plugin plugin, String id, Condition... conditions) {
        requireComponents(conditions);
        return register(plugin, id, joinNames("/", conditions),
                entity -> Arrays.stream(conditions).anyMatch(c -> c.test().test(entity)));
    }

    /** 条件の否定を登録する(例: 夜間でない = 実質昼夜以外の判定にも)。 */
    public static Condition not(Plugin plugin, String id, Condition condition) {
        if (condition == null) {
            throw new IllegalArgumentException("condition が null です");
        }
        return register(plugin, id,
                Component.text("!").append(condition.displayName()),
                entity -> !condition.test().test(entity));
    }

    private static void requireComponents(Condition[] conditions) {
        if (conditions == null || conditions.length < 2) {
            throw new IllegalArgumentException("合成には条件が2つ以上必要です");
        }
        for (Condition condition : conditions) {
            if (condition == null) {
                throw new IllegalArgumentException("null の条件は合成できません");
            }
        }
    }

    private static Component joinNames(String separator, Condition... conditions) {
        return Component.join(JoinConfiguration.separator(Component.text(separator)),
                Arrays.stream(conditions).map(Condition::displayName).toList());
    }

    /** attributelib 内部用。onEnable で1回だけ呼ばれる。 */
    public static void initStandard(Plugin attributelib) {
        if (DAY != null) {
            throw new IllegalStateException("標準条件は初期化済みです");
        }
        DAY = register(attributelib, "day", Component.text("昼間"),
                entity -> entity.getWorld().isDayTime());
        NIGHT = register(attributelib, "night", Component.text("夜間"),
                entity -> !entity.getWorld().isDayTime());
        IN_WATER = register(attributelib, "in_water", Component.text("水中"),
                LivingEntity::isInWater);
        UNDERWATER = register(attributelib, "underwater", Component.text("水没"),
                LivingEntity::isUnderWater);
        IN_LAVA = register(attributelib, "in_lava", Component.text("溶岩中"),
                LivingEntity::isInLava);
        IN_RAIN = register(attributelib, "in_rain", Component.text("降雨中"),
                LivingEntity::isInRain);
        BURNING = register(attributelib, "burning", Component.text("燃焼中"),
                entity -> entity.getFireTicks() > 0);
        IN_OVERWORLD = register(attributelib, "in_overworld", Component.text("オーバーワールド"),
                entity -> entity.getWorld().getEnvironment() == World.Environment.NORMAL);
        IN_NETHER = register(attributelib, "in_nether", Component.text("ネザー"),
                entity -> entity.getWorld().getEnvironment() == World.Environment.NETHER);
        IN_THE_END = register(attributelib, "in_the_end", Component.text("ジ・エンド"),
                entity -> entity.getWorld().getEnvironment() == World.Environment.THE_END);
        SNEAKING = register(attributelib, "sneaking", Component.text("スニーク中"),
                LivingEntity::isSneaking);
        ON_GROUND = register(attributelib, "on_ground", Component.text("接地中"),
                LivingEntity::isOnGround);
        FULL_HEALTH = register(attributelib, "full_health", Component.text("体力満タン"),
                entity -> {
                    var maxHealth = entity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                    return maxHealth == null || entity.getHealth() >= maxHealth.getValue() - 1e-9;
                });

        // 全ポーション効果の「持っているか」条件(attributelib:effect/<名前>)
        int effectCount = 0;
        for (PotionEffectType effect : RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.MOB_EFFECT)) {
            NamespacedKey key = effectConditionKey(effect);
            registry().register(attributelib, key.getKey(),
                    Component.translatable(effect),
                    entity -> entity.hasPotionEffect(effect));
            effectCount++;
        }
        attributelib.getLogger().info("エフェクト条件を登録: " + effectCount + " 件");
    }
}
