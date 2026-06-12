package DIV.attributelib.api;

import DIV.attributelib.Attributelib;
import org.bukkit.attribute.Attribute;

/**
 * バニラ属性のブリッジ定数カタログ。カスタム属性と完全に同じ API で操作できる:
 *
 * <pre>{@code
 * // 30秒だけ最大HP+10(再起動・アンロードを跨いでも正確に失効する)
 * Attributes.add(mob, Vanilla.MAX_HEALTH, "myplugin:trait/tank", Operation.ADD, 10, 600);
 * Attributes.removeAll(mob, "myplugin:trait/tank");   // sourceId 一括除去
 *
 * // 採掘速度(プレイヤー)— ツルハシ側に書くなら ItemAttributes.add で同じ属性を指定
 * Attributes.add(player, Vanilla.BLOCK_BREAK_SPEED, "myplugin:buff", Operation.MULTIPLY, 1.5, 200);
 * ItemAttributes.add(pickaxe, Vanilla.MINING_EFFICIENCY, Operation.ADD, 10, EquipmentSlotGroup.MAINHAND);
 * }</pre>
 *
 * <p>仕組み: モディファイア管理・時限・永続化は attributelib のエンジンが持ち、合成結果だけを
 * transient なバニラモディファイア({@code attributelib:bridge})として書き込む。
 * バニラ NBT に残らないため、外したのに残る事故が構造的に起きない。
 * {@link Attributes#get} は バニラの最終値(他プラグインのモディファイア込み)を返す。</p>
 *
 * <p><b>注意</b>: 他プラグインがバニラ base 値を変更した場合、こちらの書き込み分は
 * 次の再計算(変更・失効・再ロード時)まで古い base 基準のまま。
 * また MAX_HEALTH を下げても現在HPの即時クランプはバニラ仕様に従う。
 * 値の有効範囲はバニラが属性ごとに内部クランプする。</p>
 *
 * <p>このクラスは attributelib の onEnable 以降に触ること(depend していれば自動的に満たされる)。</p>
 */
public final class Vanilla {

    public static final AttributeType MAX_HEALTH = of(Attribute.MAX_HEALTH);
    public static final AttributeType FOLLOW_RANGE = of(Attribute.FOLLOW_RANGE);
    public static final AttributeType KNOCKBACK_RESISTANCE = of(Attribute.KNOCKBACK_RESISTANCE);
    public static final AttributeType MOVEMENT_SPEED = of(Attribute.MOVEMENT_SPEED);
    public static final AttributeType FLYING_SPEED = of(Attribute.FLYING_SPEED);
    public static final AttributeType ATTACK_DAMAGE = of(Attribute.ATTACK_DAMAGE);
    public static final AttributeType ATTACK_KNOCKBACK = of(Attribute.ATTACK_KNOCKBACK);
    public static final AttributeType ATTACK_SPEED = of(Attribute.ATTACK_SPEED);
    public static final AttributeType ARMOR = of(Attribute.ARMOR);
    public static final AttributeType ARMOR_TOUGHNESS = of(Attribute.ARMOR_TOUGHNESS);
    public static final AttributeType FALL_DAMAGE_MULTIPLIER = of(Attribute.FALL_DAMAGE_MULTIPLIER);
    public static final AttributeType LUCK = of(Attribute.LUCK);
    public static final AttributeType MAX_ABSORPTION = of(Attribute.MAX_ABSORPTION);
    public static final AttributeType SAFE_FALL_DISTANCE = of(Attribute.SAFE_FALL_DISTANCE);
    public static final AttributeType SCALE = of(Attribute.SCALE);
    public static final AttributeType STEP_HEIGHT = of(Attribute.STEP_HEIGHT);
    public static final AttributeType GRAVITY = of(Attribute.GRAVITY);
    public static final AttributeType JUMP_STRENGTH = of(Attribute.JUMP_STRENGTH);
    public static final AttributeType BURNING_TIME = of(Attribute.BURNING_TIME);
    public static final AttributeType CAMERA_DISTANCE = of(Attribute.CAMERA_DISTANCE);
    public static final AttributeType EXPLOSION_KNOCKBACK_RESISTANCE = of(Attribute.EXPLOSION_KNOCKBACK_RESISTANCE);
    public static final AttributeType MOVEMENT_EFFICIENCY = of(Attribute.MOVEMENT_EFFICIENCY);
    public static final AttributeType OXYGEN_BONUS = of(Attribute.OXYGEN_BONUS);
    public static final AttributeType WATER_MOVEMENT_EFFICIENCY = of(Attribute.WATER_MOVEMENT_EFFICIENCY);
    public static final AttributeType TEMPT_RANGE = of(Attribute.TEMPT_RANGE);
    /** ブロックへの到達距離。 */
    public static final AttributeType BLOCK_INTERACTION_RANGE = of(Attribute.BLOCK_INTERACTION_RANGE);
    /** エンティティへの到達距離。 */
    public static final AttributeType ENTITY_INTERACTION_RANGE = of(Attribute.ENTITY_INTERACTION_RANGE);
    /** 採掘速度倍率(プレイヤー)。 */
    public static final AttributeType BLOCK_BREAK_SPEED = of(Attribute.BLOCK_BREAK_SPEED);
    /** 採掘効率(効率エンチャント相当の加算値)。 */
    public static final AttributeType MINING_EFFICIENCY = of(Attribute.MINING_EFFICIENCY);
    public static final AttributeType SNEAKING_SPEED = of(Attribute.SNEAKING_SPEED);
    /** 水中採掘速度(水中ペナルティの緩和)。 */
    public static final AttributeType SUBMERGED_MINING_SPEED = of(Attribute.SUBMERGED_MINING_SPEED);
    public static final AttributeType SWEEPING_DAMAGE_RATIO = of(Attribute.SWEEPING_DAMAGE_RATIO);
    public static final AttributeType SPAWN_REINFORCEMENTS = of(Attribute.SPAWN_REINFORCEMENTS);
    public static final AttributeType WAYPOINT_TRANSMIT_RANGE = of(Attribute.WAYPOINT_TRANSMIT_RANGE);
    public static final AttributeType WAYPOINT_RECEIVE_RANGE = of(Attribute.WAYPOINT_RECEIVE_RANGE);

    private Vanilla() {
    }

    /** バニラ Attribute からブリッジ属性を引く(定数に無い将来の属性にも使える)。 */
    public static AttributeType of(Attribute attribute) {
        Attributelib plugin = Attributelib.instance();
        if (plugin == null) {
            throw new IllegalStateException(
                    "attributelib がまだロードされていません。plugin.yml の depend に attributelib を追加してください");
        }
        return plugin.engine().bridgedType(attribute);
    }
}
