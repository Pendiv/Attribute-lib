package DIV.attributelib.api;

import DIV.attributelib.Attributelib;
import DIV.attributelib.damage.DamageListener;
import DIV.attributelib.damage.DamageTypes;
import org.bukkit.NamespacedKey;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jspecify.annotations.Nullable;

import java.util.logging.Logger;

/**
 * 能動ダメージ API。カスタムダメージタイプ・元素でダメージを与える。
 *
 * <pre>{@code
 * DamageLib.magic(attacker, victim, 8);                      // 魔法
 * DamageLib.pierce(attacker, victim, 5);                     // 物理貫通
 * DamageLib.trueDamage(attacker, victim, 3);                 // 確定(入力値=与ダメ)
 * DamageLib.deal(DamageElements.FIRE, attacker, victim, 6);  // 元素を宣誓して撃つ
 * }</pre>
 *
 * <p>ここで与えたダメージも被害者側の {@link EntityDamageEvent} を通常通り通過するため、
 * 受動(被弾修飾)・能動・他プラグイン由来が同一のパイプラインで処理される。</p>
 */
public final class DamageLib {

    private static boolean fallbackWarned;

    private DamageLib() {
    }

    /** 物理貫通ダメージ(attributelib:physical_pierce)。バニラ防具は素通り、物理軽減%は効く。 */
    public static void pierce(@Nullable LivingEntity attacker, LivingEntity victim, double amount) {
        deal(DamageTypes.PHYSICAL_PIERCE, DamageType.MOB_ATTACK, null, attacker, victim, amount);
    }

    /** 魔法ダメージ(attributelib:magic)。 */
    public static void magic(@Nullable LivingEntity attacker, LivingEntity victim, double amount) {
        deal(DamageTypes.MAGIC, DamageType.MAGIC, null, attacker, victim, amount);
    }

    /** 確定ダメージ(attributelib:true_damage)。全防御と無敵フレームを素通りし、入力値がそのまま通る。 */
    public static void trueDamage(@Nullable LivingEntity attacker, LivingEntity victim, double amount) {
        deal(DamageTypes.TRUE_DAMAGE, DamageType.MAGIC, null, attacker, victim, amount);
    }

    /**
     * 元素を「宣誓」してダメージを与える。専用のデータパックダメージタイプが無くても、
     * このダメージは指定元素として扱われ、{@code <id>_damage} / {@code <id>_resist} が効く。
     * キャリアは attributelib:elemental(防具素通り、message_id は magic)。
     *
     * <p>元素に固有のバニラ相互作用(炎無効 mob など)が必要なら、利用側が自前の
     * データパックタイプを作って {@link DamageElements#mapDamageType} で紐付け、
     * 自分で DamageSource を組んで撃てばよい — その場合もパイプラインは同じに動く。</p>
     */
    public static void deal(DamageElement element, LivingEntity attacker, LivingEntity victim, double amount) {
        if (element == null) {
            throw new IllegalArgumentException("element が null です");
        }
        deal(DamageTypes.ELEMENTAL, DamageType.MAGIC, element, attacker, victim, amount);
    }

    /** このイベントで attributelib の会心が発動したか(表示用)。 */
    public static boolean wasCritical(EntityDamageEvent event) {
        return DamageListener.wasCritical(event);
    }

    /** このダメージが確定ダメージ(パイプライン不干渉)か。 */
    public static boolean isTrueDamage(DamageSource source) {
        return DamageTypes.isTrueDamage(source);
    }

    /** ダメージソースの元素判定。無属性なら null。 */
    public static @Nullable DamageElement elementOf(DamageSource source) {
        Attributelib plugin = Attributelib.instance();
        if (plugin == null) {
            throw new IllegalStateException(
                    "attributelib がまだロードされていません。plugin.yml の depend に attributelib を追加してください");
        }
        return plugin.elements().elementOf(source.getDamageType().getKey());
    }

    private static void deal(NamespacedKey typeKey, DamageType fallback, DamageElement declaredElement,
                             LivingEntity attacker, LivingEntity victim, double amount) {
        java.util.Objects.requireNonNull(victim, "victim が null です");
        if (amount <= 0) {
            return;
        }
        DamageType type = DamageTypes.resolve(typeKey);
        if (type == null) {
            // データパック未読込(初回起動の再起動前)。挙動は近いバニラタイプで代替する
            if (!fallbackWarned) {
                fallbackWarned = true;
                Logger.getLogger("attributelib").warning(
                        "カスタムダメージタイプ " + typeKey + " が未登録のためバニラタイプで代替します。"
                                + "サーバーを再起動するとデータパックが有効になります");
            }
            type = fallback;
        }
        DamageSource.Builder builder = DamageSource.builder(type);
        if (attacker != null) {
            builder.withCausingEntity(attacker).withDirectEntity(attacker);
        }
        if (declaredElement != null) {
            DamageListener.declare(victim, declaredElement);
            try {
                victim.damage(amount, builder.build());
            } finally {
                DamageListener.clearDeclaration();
            }
        } else {
            victim.damage(amount, builder.build());
        }
    }
}
