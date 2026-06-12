package DIV.attributelib.damage;

import DIV.attributelib.api.DamageElement;
import DIV.attributelib.api.StandardAttributes;
import DIV.attributelib.core.AttributeEngine;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * ダメージパイプラインの適用リスナー。サーバーに1つだけ登録され、
 * 標準属性の適用はすべてここを通る(二重処理を構造的に防ぐ)。
 *
 * <p>優先度 HIGH で固定。他プラグインの素朴な setDamage 修飾は NORMAL 以下で行えば
 * パイプラインの入力(base)として扱われ、MONITOR では適用後の値が見える。</p>
 */
public final class DamageListener implements Listener {

    /** この tick 中に会心が発動したイベント(表示層・利用側プラグインへの照会用)。 */
    private static final Map<EntityDamageEvent, Boolean> CRITICALS = new WeakHashMap<>();

    /** データを持たない側に使うデフォルト統計(属性状態を生成しないため)。 */
    private static final DamagePipeline.AttackerStats DEFAULT_ATTACKER =
            new DamagePipeline.AttackerStats(1, 0, 1.5, 0, 0, 1);
    private static final DamagePipeline.VictimStats DEFAULT_VICTIM =
            new DamagePipeline.VictimStats(0, 0, 1);

    /** 宣誓ダメージのマーカー(DamageLib.deal が damage() 呼び出しの間だけ設定する)。 */
    private static LivingEntity declaredVictim;
    private static DamageElement declaredElement;

    private final AttributeEngine engine;
    private final ElementRegistry elements;

    public DamageListener(AttributeEngine engine, ElementRegistry elements) {
        this.engine = engine;
        this.elements = elements;
    }

    /** {@link DIV.attributelib.api.DamageLib#wasCritical} の実体。 */
    public static boolean wasCritical(EntityDamageEvent event) {
        return CRITICALS.containsKey(event);
    }

    /** 宣誓ダメージの開始(DamageLib 内部用)。damage() 呼び出しの直前に設定する。 */
    public static void declare(LivingEntity victim, DamageElement element) {
        declaredVictim = victim;
        declaredElement = element;
    }

    /** 宣誓ダメージの終了(DamageLib 内部用)。finally で必ず呼ぶ。 */
    public static void clearDeclaration() {
        declaredVictim = null;
        declaredElement = null;
    }

    /**
     * 宣誓された元素。棘の鎧などで処理中に別のダメージイベントが割り込んでも
     * 誤適用しないよう、宣誓時の被害者と一致する場合のみ返す。
     */
    private static DamageElement declaredFor(Entity victim) {
        return victim == declaredVictim ? declaredElement : null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        double base = event.getDamage();
        if (base <= 0) {
            return;
        }
        DamageSource source = event.getDamageSource();
        if (DamageTypes.isTrueDamage(source)) {
            return; // 確定ダメージ: 入力値がそのまま通る
        }

        LivingEntity attacker = source.getCausingEntity() instanceof LivingEntity living ? living : null;

        // ホットパス早期離脱: 双方に attributelib のデータが無ければ全段が恒等変換
        // (属性は全てデフォルト = 倍率1)なので、状態生成も計算もせず素通しする
        boolean attackerHasData = attacker != null && engine.hasData(attacker);
        boolean victimHasData = engine.hasData(victim);
        if (!attackerHasData && !victimHasData) {
            return;
        }

        DamageElement declared = declaredFor(victim);
        DamageElement element = declared != null
                ? declared
                : elements.elementOf(source.getDamageType().getKey());

        boolean vanillaCritical = event instanceof EntityDamageByEntityEvent byEntity && byEntity.isCritical();

        // データの無い側はデフォルト定数を使い、属性状態を生成しない
        DamagePipeline.AttackerStats attackerStats = attacker == null ? null
                : !attackerHasData ? DEFAULT_ATTACKER
                : new DamagePipeline.AttackerStats(
                        element != null ? engine.get(attacker, element.damageAttribute()) : 1.0,
                        engine.get(attacker, StandardAttributes.CRIT_CHANCE),
                        engine.get(attacker, StandardAttributes.CRIT_DAMAGE),
                        engine.get(attacker, StandardAttributes.ARMOR_PENETRATION),
                        engine.get(attacker, StandardAttributes.ARMOR_PENETRATION_FLAT),
                        engine.get(attacker, StandardAttributes.DAMAGE_DEALT));

        DamagePipeline.VictimStats victimStats = !victimHasData ? DEFAULT_VICTIM
                : new DamagePipeline.VictimStats(
                        element != null ? engine.get(victim, element.resistAttribute()) : 0.0,
                        engine.get(victim, StandardAttributes.CRIT_RESIST),
                        engine.get(victim, StandardAttributes.DAMAGE_TAKEN));

        // 防具値とタグ照会は貫通が有効な時だけ必要(compute は armorApplied=false なら参照しない)
        boolean penActive = attackerStats != null
                && (attackerStats.armorPenetration() > 0 || attackerStats.armorPenetrationFlat() > 0);
        boolean armorApplied = penActive && !DamageTypes.bypassesArmor(source);
        double armor = 0;
        double toughness = 0;
        if (armorApplied) {
            AttributeInstance armorInstance = victim.getAttribute(Attribute.ARMOR);
            AttributeInstance toughnessInstance = victim.getAttribute(Attribute.ARMOR_TOUGHNESS);
            armor = armorInstance != null ? armorInstance.getValue() : 0;
            toughness = toughnessInstance != null ? toughnessInstance.getValue() : 0;
        }

        DamagePipeline.Result result = DamagePipeline.compute(
                attackerStats,
                victimStats,
                vanillaCritical,
                base,
                armorApplied,
                armor,
                toughness,
                ThreadLocalRandom.current()::nextDouble);

        if (result.critical()) {
            CRITICALS.put(event, Boolean.TRUE);
        }
        if (result.multiplier() != 1.0) {
            event.setDamage(Math.max(0, base * result.multiplier()));
        }
    }
}
