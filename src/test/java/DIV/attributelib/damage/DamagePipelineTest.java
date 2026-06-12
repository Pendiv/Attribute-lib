package DIV.attributelib.damage;

import DIV.attributelib.damage.DamagePipeline.AttackerStats;
import DIV.attributelib.damage.DamagePipeline.Result;
import DIV.attributelib.damage.DamagePipeline.VictimStats;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamagePipelineTest {

    private static final AttackerStats NEUTRAL_ATTACKER = new AttackerStats(1, 0, 1.5, 0, 0, 1);
    private static final VictimStats NEUTRAL_VICTIM = new VictimStats(0, 0, 1);

    private static Result compute(AttackerStats attacker, VictimStats victim) {
        return DamagePipeline.compute(attacker, victim, false, 10, true, 0, 0, () -> 0.99);
    }

    @Test
    @DisplayName("全属性が中立なら倍率 1.0(会心なし)")
    void neutralIsIdentity() {
        Result result = compute(NEUTRAL_ATTACKER, NEUTRAL_VICTIM);
        assertEquals(1.0, result.multiplier());
        assertFalse(result.critical());
    }

    @Test
    @DisplayName("元素倍率: 与ダメ% × (1 - 軽減%)")
    void elementMultiplier() {
        AttackerStats attacker = new AttackerStats(1.5, 0, 1.5, 0, 0, 1);
        VictimStats victim = new VictimStats(0.25, 0, 1);
        Result result = compute(attacker, victim);
        assertEquals(1.5 * 0.75, result.multiplier(), 1e-9);
    }

    @Test
    @DisplayName("攻撃者なしでも元素軽減は効く(バニラの炎・雷など)")
    void resistWorksWithoutAttacker() {
        VictimStats victim = new VictimStats(0.5, 0, 1);
        Result result = DamagePipeline.compute(null, victim, false, 10, true, 0, 0, () -> 0.99);
        assertEquals(0.5, result.multiplier(), 1e-9);
    }

    @Test
    @DisplayName("軽減 100% 超は 0 で止まる(負ダメージにならない)")
    void resistOverCapClampsToZero() {
        VictimStats victim = new VictimStats(1.5, 0, 1);
        assertEquals(0, compute(NEUTRAL_ATTACKER, victim).multiplier());
    }

    @Test
    @DisplayName("負の軽減(脆弱)はダメージ増になる")
    void negativeResistAmplifies() {
        VictimStats victim = new VictimStats(-0.5, 0, 1);
        assertEquals(1.5, compute(NEUTRAL_ATTACKER, victim).multiplier(), 1e-9);
    }

    @Test
    @DisplayName("無属性ダメージ(elementDamage=1, elementResist=0)には全体倍率のみ効く")
    void neutralElementStats() {
        AttackerStats attacker = new AttackerStats(1, 0, 1.5, 0, 0, 2);
        VictimStats victim = new VictimStats(0, 0, 0.5);
        Result result = compute(attacker, victim);
        assertEquals(2 * 0.5, result.multiplier(), 1e-9);
    }

    @Test
    @DisplayName("会心: 乱数が確率を下回れば crit_damage が乗る")
    void critApplies() {
        AttackerStats attacker = new AttackerStats(1, 0.3, 2.0, 0, 0, 1);
        Result result = DamagePipeline.compute(attacker, NEUTRAL_VICTIM,
                false, 10, true, 0, 0, () -> 0.29);
        assertTrue(result.critical());
        assertEquals(2.0, result.multiplier());
    }

    @Test
    @DisplayName("会心: 乱数が確率以上なら発動しない")
    void critMisses() {
        AttackerStats attacker = new AttackerStats(1, 0.3, 2.0, 0, 0, 1);
        Result result = DamagePipeline.compute(attacker, NEUTRAL_VICTIM,
                false, 10, true, 0, 0, () -> 0.30);
        assertFalse(result.critical());
        assertEquals(1.0, result.multiplier());
    }

    @Test
    @DisplayName("会心耐性は会心率から引かれる")
    void critResistReducesChance() {
        AttackerStats attacker = new AttackerStats(1, 0.3, 2.0, 0, 0, 1);
        VictimStats victim = new VictimStats(0, 0.3, 1);
        Result result = DamagePipeline.compute(attacker, victim,
                false, 10, true, 0, 0, () -> 0.0);
        assertFalse(result.critical());
    }

    @Test
    @DisplayName("バニラのジャンプ会心が出た攻撃では会心を重複させない")
    void vanillaCritSuppressesOurCrit() {
        AttackerStats attacker = new AttackerStats(1, 1.0, 2.0, 0, 0, 1);
        Result result = DamagePipeline.compute(attacker, NEUTRAL_VICTIM,
                true, 10, true, 0, 0, () -> 0.0);
        assertFalse(result.critical());
        assertEquals(1.0, result.multiplier());
    }

    @Test
    @DisplayName("攻撃者なし(環境ダメージ)では会心も攻撃側倍率も発生しない")
    void noAttackerNoOffense() {
        VictimStats victim = new VictimStats(0, 0, 0.8);
        Result result = DamagePipeline.compute(null, victim,
                false, 10, false, 0, 0, () -> 0.0);
        assertEquals(0.8, result.multiplier(), 1e-9);
        assertFalse(result.critical());
    }

    @Test
    @DisplayName("バニラ防具式: 既知値(armor20 toughness0 damage10 → 60%軽減)")
    void armorFactorKnownValues() {
        assertEquals(1.0, DamagePipeline.armorFactor(10, 0, 0));
        // reduction = min(20, max(20/5=4, 20-40/8=15))/25 = 0.6
        assertEquals(0.4, DamagePipeline.armorFactor(10, 20, 0), 1e-9);
        // タフネス8: max(4, 20-40/16=17.5)/25 = 0.7
        assertEquals(0.3, DamagePipeline.armorFactor(10, 20, 8), 1e-9);
        // 負 armor でも factor は 1.0 を超えない
        assertEquals(1.0, DamagePipeline.armorFactor(10, -5, 0));
    }

    @Test
    @DisplayName("逆算ソルバー: 解にバニラ防具式を通すと目標値に戻る")
    void solverRoundTrip() {
        for (double target : new double[]{0.5, 4, 10, 50}) {
            for (double armor : new double[]{2, 10, 20, 30}) {
                for (double toughness : new double[]{0, 8, 12}) {
                    double solved = DamagePipeline.solveBaseForFinal(target, armor, toughness);
                    assertEquals(target, solved * DamagePipeline.armorFactor(solved, armor, toughness), 1e-6,
                            "target=" + target + " armor=" + armor + " toughness=" + toughness);
                }
            }
        }
        // armor なしなら入力そのまま
        assertEquals(7.0, DamagePipeline.solveBaseForFinal(7, 0, 0));
        // 既知の解析解: b×(1-(20-b/2)/25)=10 → b²+10b-500=0 → b=(−10+√2100)/2
        assertEquals((-10 + Math.sqrt(2100)) / 2,
                DamagePipeline.solveBaseForFinal(10, 20, 0), 1e-6);
    }

    @Test
    @DisplayName("貫通100%: 新baseにバニラ防具式を通すと裸での最終値に一致(増幅されない)")
    void fullArmorPenetration() {
        AttackerStats attacker = new AttackerStats(1, 0, 1.5, 1.0, 0, 1);
        Result result = DamagePipeline.compute(attacker, NEUTRAL_VICTIM,
                false, 10, true, 20, 0, () -> 0.99);
        double newBase = 10 * result.multiplier();
        // 実際にバニラが適用する最終値 = 裸で受ける10にピッタリ着地
        assertEquals(10.0, newBase * DamagePipeline.armorFactor(newBase, 20, 0), 1e-6);
    }

    @Test
    @DisplayName("貫通は bypasses_armor のダメージには効かない")
    void penetrationOnlyWhenArmorApplied() {
        AttackerStats attacker = new AttackerStats(1, 0, 1.5, 1.0, 50, 1);
        Result bypassed = DamagePipeline.compute(attacker, NEUTRAL_VICTIM,
                false, 10, false, 20, 0, () -> 0.99);
        assertEquals(1.0, bypassed.multiplier());
    }

    @Test
    @DisplayName("貫通値(flat): armor20−10=10 相当の最終値に着地する")
    void flatPenetration() {
        AttackerStats attacker = new AttackerStats(1, 0, 1.5, 0, 10, 1);
        Result result = DamagePipeline.compute(attacker, NEUTRAL_VICTIM,
                false, 10, true, 20, 0, () -> 0.99);
        double newBase = 10 * result.multiplier();
        double targetFinal = 10 * DamagePipeline.armorFactor(10, 10, 0); // armor10 で受けた場合
        assertEquals(targetFinal, newBase * DamagePipeline.armorFactor(newBase, 20, 0), 1e-6);
    }

    @Test
    @DisplayName("貫通の不変条件: 装備持ちの最終値が裸の最終値を超えない(逆転バグの再発防止)")
    void penetrationNeverExceedsUnarmored() {
        for (double pen : new double[]{0.25, 0.5, 0.75, 1.0}) {
            AttackerStats attacker = new AttackerStats(1, 0, 1.5, pen, 0, 1);
            for (double armor : new double[]{2, 10, 20}) {
                Result result = DamagePipeline.compute(attacker, NEUTRAL_VICTIM,
                        false, 4, true, armor, 0, () -> 0.99);
                double newBase = 4 * result.multiplier();
                double actualFinal = newBase * DamagePipeline.armorFactor(newBase, armor, 0);
                assertTrue(actualFinal <= 4 + 1e-9,
                        "pen=" + pen + " armor=" + armor + " final=" + actualFinal + " が裸の4を超過");
            }
        }
    }

    @Test
    @DisplayName("全段合成: 倍率を全て掛けた後の量に対して貫通が逆算される")
    void fullComposition() {
        AttackerStats attacker = new AttackerStats(1.5, 1.0, 2.0, 1.0, 0, 1.2);
        VictimStats victim = new VictimStats(0.2, 0, 0.9);
        Result result = DamagePipeline.compute(attacker, victim,
                false, 10, true, 20, 0, () -> 0.0);
        assertTrue(result.critical());
        double scaled = 10 * (1.5 * 0.8) * 2.0 * 1.2 * 0.9;   // 元素 × 会心 × 全体
        double newBase = 10 * result.multiplier();
        // 貫通100% → 最終値は「scaled を裸で受けた値」= scaled に着地
        assertEquals(scaled, newBase * DamagePipeline.armorFactor(newBase, 20, 0), 1e-6);
    }
}
