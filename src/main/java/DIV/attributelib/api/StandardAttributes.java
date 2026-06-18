package DIV.attributelib.api;

import DIV.attributelib.core.AttributeEngine;
import DIV.attributelib.damage.DamageTypes;
import DIV.attributelib.damage.ElementRegistry;
import net.kyori.adventure.text.Component;
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
    /** 全与ダメ実数値加算(攻撃側)。base に上乗せされ、会心・%・防具計算が乗る。既定 0。 */
    public static AttributeType DAMAGE_DEALT_FLAT;
    /** 全被ダメ倍率(防御側、確定ダメージ以外の全ダメージに乗る)。 */
    public static AttributeType DAMAGE_TAKEN;
    /** 物理与ダメ倍率(攻撃側)。= {@code DamageElements.PHYSICAL.damageAttribute()} */
    public static AttributeType PHYSICAL_DAMAGE;
    /** 物理与ダメ実数値加算(攻撃側)。= {@code DamageElements.PHYSICAL.flatAttribute()} */
    public static AttributeType PHYSICAL_DAMAGE_FLAT;
    /** 物理軽減割合(防御側)。 */
    public static AttributeType PHYSICAL_RESIST;
    /** 魔法与ダメ倍率(攻撃側)。 */
    public static AttributeType MAGIC_DAMAGE;
    /** 魔法与ダメ実数値加算(攻撃側)。 */
    public static AttributeType MAGIC_DAMAGE_FLAT;
    /** 魔法軽減割合(防御側)。 */
    public static AttributeType MAGIC_RESIST;
    /** 炎与ダメ倍率(攻撃側)。 */
    public static AttributeType FIRE_DAMAGE;
    /** 炎与ダメ実数値加算(攻撃側)。 */
    public static AttributeType FIRE_DAMAGE_FLAT;
    /** 炎軽減割合(防御側)。バニラの炎系ダメージにも効く。 */
    public static AttributeType FIRE_RESIST;
    /** 雷与ダメ倍率(攻撃側)。 */
    public static AttributeType LIGHTNING_DAMAGE;
    /** 雷与ダメ実数値加算(攻撃側)。 */
    public static AttributeType LIGHTNING_DAMAGE_FLAT;
    /** 雷軽減割合(防御側)。バニラの雷にも効く。 */
    public static AttributeType LIGHTNING_RESIST;
    /**
     * 射撃与ダメ倍率(攻撃側)。矢・三叉槍・投射物など {@code minecraft:is_projectile} タグの
     * ダメージに乗る。元素とは直交し、物理の矢なら physical と projectile の両方が乗る。既定 1.0。
     */
    public static AttributeType PROJECTILE_DAMAGE;
    /** 射撃与ダメ実数値加算(攻撃側)。射撃ダメージの base に上乗せ。既定 0。 */
    public static AttributeType PROJECTILE_DAMAGE_FLAT;
    /**
     * 爆発与ダメ倍率(攻撃側)。{@code minecraft:is_explosion} タグのダメージに乗る。
     * 元素とは直交する。既定 1.0。
     */
    public static AttributeType EXPLOSION_DAMAGE;
    /** 爆発与ダメ実数値加算(攻撃側)。爆発ダメージの base に上乗せ。既定 0。 */
    public static AttributeType EXPLOSION_DAMAGE_FLAT;
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
        DAMAGE_DEALT = engine.register(attributelib, "damage_dealt", 1.0, 0.0, Double.MAX_VALUE,
                Component.text("与ダメージ"), true);
        DAMAGE_DEALT_FLAT = engine.register(attributelib, "damage_dealt_flat", 0.0, 0.0, Double.MAX_VALUE,
                Component.text("与ダメージ(実数)"), false);
        DAMAGE_TAKEN = engine.register(attributelib, "damage_taken", 1.0, 0.0, Double.MAX_VALUE,
                Component.text("被ダメージ"), true);
        CRIT_CHANCE = engine.register(attributelib, "crit_chance", 0.0, 0.0, 1.0,
                Component.text("会心率"), true);
        CRIT_DAMAGE = engine.register(attributelib, "crit_damage", 1.5, 1.0, Double.MAX_VALUE,
                Component.text("会心ダメージ"), true);
        CRIT_RESIST = engine.register(attributelib, "crit_resist", 0.0, 0.0, 1.0,
                Component.text("会心耐性"), true);
        ARMOR_PENETRATION = engine.register(attributelib, "armor_penetration", 0.0, 0.0, 1.0,
                Component.text("防具貫通"), true);
        ARMOR_PENETRATION_FLAT = engine.register(attributelib, "armor_penetration_flat", 0.0, 0.0, Double.MAX_VALUE,
                Component.text("防具貫通値"), false);
        HEAL_MULTIPLIER = engine.register(attributelib, "heal_multiplier", 1.0, 0.0, Double.MAX_VALUE,
                Component.text("回復量"), true);

        // 射撃・爆発カテゴリ(元素と直交。is_projectile / is_explosion タグで判定される)
        PROJECTILE_DAMAGE = engine.register(attributelib, "projectile_damage", 1.0, 0.0, Double.MAX_VALUE,
                Component.text("射撃与ダメージ"), true);
        PROJECTILE_DAMAGE_FLAT = engine.register(attributelib, "projectile_damage_flat", 0.0, 0.0, Double.MAX_VALUE,
                Component.text("射撃与ダメージ(実数)"), false);
        EXPLOSION_DAMAGE = engine.register(attributelib, "explosion_damage", 1.0, 0.0, Double.MAX_VALUE,
                Component.text("爆発与ダメージ"), true);
        EXPLOSION_DAMAGE_FLAT = engine.register(attributelib, "explosion_damage_flat", 0.0, 0.0, Double.MAX_VALUE,
                Component.text("爆発与ダメージ(実数)"), false);

        DamageElement physical = elements.register(attributelib, "physical",
                Component.text("物理与ダメージ"), Component.text("物理耐性"));
        DamageElement magic = elements.register(attributelib, "magic",
                Component.text("魔法与ダメージ"), Component.text("魔法耐性"));
        DamageElement fire = elements.register(attributelib, "fire",
                Component.text("炎与ダメージ"), Component.text("炎耐性"));
        DamageElement lightning = elements.register(attributelib, "lightning",
                Component.text("雷与ダメージ"), Component.text("雷耐性"));
        DamageElements.initBuiltins(physical, magic, fire, lightning);

        PHYSICAL_DAMAGE = physical.damageAttribute();
        PHYSICAL_DAMAGE_FLAT = physical.flatAttribute();
        PHYSICAL_RESIST = physical.resistAttribute();
        MAGIC_DAMAGE = magic.damageAttribute();
        MAGIC_DAMAGE_FLAT = magic.flatAttribute();
        MAGIC_RESIST = magic.resistAttribute();
        FIRE_DAMAGE = fire.damageAttribute();
        FIRE_DAMAGE_FLAT = fire.flatAttribute();
        FIRE_RESIST = fire.resistAttribute();
        LIGHTNING_DAMAGE = lightning.damageAttribute();
        LIGHTNING_DAMAGE_FLAT = lightning.flatAttribute();
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
