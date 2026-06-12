package DIV.attributelib.api;

import DIV.attributelib.core.AttributeEngine;
import DIV.attributelib.damage.DamageTypes;
import DIV.attributelib.damage.ElementRegistry;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/**
 * attributelib が提供する標準属性(層2のダメージパイプラインが参照する)。
 * すべて attributelib の onEnable で登録されるため、depend している
 * プラグインからは常に利用可能。
 *
 * <p>対構造(攻撃側 | 防御側):</p>
 * <pre>
 * DAMAGE_DEALT   (全与ダメ倍率)      | DAMAGE_TAKEN   (全被ダメ倍率)
 * PHYSICAL_DAMAGE(物理与ダメ倍率)    | PHYSICAL_RESIST(物理軽減割合)
 * MAGIC_DAMAGE   (魔法与ダメ倍率)    | MAGIC_RESIST   (魔法軽減割合)
 * FIRE_DAMAGE    (炎与ダメ倍率)      | FIRE_RESIST    (炎軽減割合)
 * LIGHTNING_DAMAGE(雷与ダメ倍率)     | LIGHTNING_RESIST(雷軽減割合)
 * CRIT_CHANCE / CRIT_DAMAGE         | CRIT_RESIST
 * ARMOR_PENETRATION(率) / _FLAT(値) | バニラ armor
 * HEAL_MULTIPLIER(回復倍率)         | —
 * </pre>
 *
 * <p>元素ペア(physical / magic / fire / lightning)は {@link DamageElements} の
 * 標準元素として登録され、ここの定数は同じ {@link AttributeType} インスタンスを指す。
 * 新しい元素ペアは {@link DamageElements#register} で誰でも追加できる。</p>
 *
 * <p>セマンティクス: 倍率系は 1.0 が中立。軽減(resist)系は「カットする割合」で
 * 0.0 = 効果なし、1.0 = 完全無効、負値 = 脆弱化。CRIT_CHANCE は 0..1 の確率。</p>
 */
public final class StandardAttributes {

    /** 全与ダメ倍率(攻撃側、確定ダメージ以外の全ダメージに乗る)。 */
    public static AttributeType DAMAGE_DEALT;
    /** 全被ダメ倍率(防御側、確定ダメージ以外の全ダメージに乗る)。 */
    public static AttributeType DAMAGE_TAKEN;
    /** 物理与ダメ倍率(攻撃側)。= {@code DamageElements.PHYSICAL.damageAttribute()} */
    public static AttributeType PHYSICAL_DAMAGE;
    /** 物理軽減割合(防御側)。 */
    public static AttributeType PHYSICAL_RESIST;
    /** 魔法与ダメ倍率(攻撃側)。 */
    public static AttributeType MAGIC_DAMAGE;
    /** 魔法軽減割合(防御側)。 */
    public static AttributeType MAGIC_RESIST;
    /** 炎与ダメ倍率(攻撃側)。 */
    public static AttributeType FIRE_DAMAGE;
    /** 炎軽減割合(防御側)。バニラの炎系ダメージにも効く。 */
    public static AttributeType FIRE_RESIST;
    /** 雷与ダメ倍率(攻撃側)。 */
    public static AttributeType LIGHTNING_DAMAGE;
    /** 雷軽減割合(防御側)。バニラの雷にも効く。 */
    public static AttributeType LIGHTNING_RESIST;
    /** 会心率 0..1(攻撃側)。バニラのジャンプ会心が出た攻撃では重複して発動しない。 */
    public static AttributeType CRIT_CHANCE;
    /** 会心倍率(攻撃側)。既定 1.5。 */
    public static AttributeType CRIT_DAMAGE;
    /** 会心耐性 0..1(防御側)。攻撃側の会心率から引かれる。 */
    public static AttributeType CRIT_RESIST;
    /** 防具貫通率 0..1(攻撃側)。被害者の armor を割合で無視する(防具が効くダメージのみ)。 */
    public static AttributeType ARMOR_PENETRATION;
    /** 防具貫通値(攻撃側)。被害者の armor から定数で引く。 */
    public static AttributeType ARMOR_PENETRATION_FLAT;
    /** 回復倍率。0 で回復不可(EnhancedMobs の HealMultiplier を吸収)。 */
    public static AttributeType HEAL_MULTIPLIER;

    private StandardAttributes() {
    }

    /** attributelib 内部用。onEnable で1回だけ呼ばれる。 */
    public static void init(Plugin attributelib, AttributeEngine engine, ElementRegistry elements) {
        if (DAMAGE_DEALT != null) {
            throw new IllegalStateException("StandardAttributes は初期化済みです");
        }
        DAMAGE_DEALT = engine.register(attributelib, "damage_dealt", 1.0, 0.0, Double.MAX_VALUE);
        DAMAGE_TAKEN = engine.register(attributelib, "damage_taken", 1.0, 0.0, Double.MAX_VALUE);
        CRIT_CHANCE = engine.register(attributelib, "crit_chance", 0.0, 0.0, 1.0);
        CRIT_DAMAGE = engine.register(attributelib, "crit_damage", 1.5, 1.0, Double.MAX_VALUE);
        CRIT_RESIST = engine.register(attributelib, "crit_resist", 0.0, 0.0, 1.0);
        ARMOR_PENETRATION = engine.register(attributelib, "armor_penetration", 0.0, 0.0, 1.0);
        ARMOR_PENETRATION_FLAT = engine.register(attributelib, "armor_penetration_flat", 0.0, 0.0, Double.MAX_VALUE);
        HEAL_MULTIPLIER = engine.register(attributelib, "heal_multiplier", 1.0, 0.0, Double.MAX_VALUE);

        DamageElement physical = elements.register(attributelib, "physical");
        DamageElement magic = elements.register(attributelib, "magic");
        DamageElement fire = elements.register(attributelib, "fire");
        DamageElement lightning = elements.register(attributelib, "lightning");
        DamageElements.initBuiltins(physical, magic, fire, lightning);

        PHYSICAL_DAMAGE = physical.damageAttribute();
        PHYSICAL_RESIST = physical.resistAttribute();
        MAGIC_DAMAGE = magic.damageAttribute();
        MAGIC_RESIST = magic.resistAttribute();
        FIRE_DAMAGE = fire.damageAttribute();
        FIRE_RESIST = fire.resistAttribute();
        LIGHTNING_DAMAGE = lightning.damageAttribute();
        LIGHTNING_RESIST = lightning.resistAttribute();

        seedVanillaMappings(elements, physical, magic, fire, lightning);
    }

    /** バニラ+同梱ダメージタイプを標準元素に紐付ける。 */
    private static void seedVanillaMappings(ElementRegistry elements, DamageElement physical,
                                            DamageElement magic, DamageElement fire, DamageElement lightning) {
        elements.mapDamageType(DamageTypes.PHYSICAL_PIERCE, physical);
        for (String key : new String[]{"mob_attack", "mob_attack_no_aggro", "player_attack", "arrow",
                "trident", "mob_projectile", "thrown", "spit", "sting",
                "falling_anvil", "falling_block", "falling_stalactite"}) {
            elements.mapDamageType(NamespacedKey.minecraft(key), physical);
        }

        elements.mapDamageType(DamageTypes.MAGIC, magic);
        elements.mapDamageType(NamespacedKey.minecraft("magic"), magic);
        elements.mapDamageType(NamespacedKey.minecraft("indirect_magic"), magic);

        for (String key : new String[]{"in_fire", "on_fire", "lava", "hot_floor",
                "fireball", "unattributed_fireball", "campfire"}) {
            elements.mapDamageType(NamespacedKey.minecraft(key), fire);
        }

        elements.mapDamageType(NamespacedKey.minecraft("lightning_bolt"), lightning);
    }
}
