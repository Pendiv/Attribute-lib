package DIV.attributelib.damage;

import DIV.attributelib.damage.DamagePipeline.ArmorOverflow;
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

    // ─── 上限超過防御の軽減化(ArmorOverflow) ───

    private static final ArmorOverflow OVERFLOW = new ArmorOverflow(100, 0.008, 0.95);

    @Test
    @DisplayName("overflowResist: threshold 以下は 0、超過分は率で増え、maxReduction で頭打ち")
    void overflowResistFormula() {
        assertEquals(0, DamagePipeline.overflowResist(100, OVERFLOW), 1e-9);
        assertEquals(0, DamagePipeline.overflowResist(50, OVERFLOW), 1e-9);
        // (200-100)×0.008 = 0.8
        assertEquals(0.8, DamagePipeline.overflowResist(200, OVERFLOW), 1e-9);
        // (1000-100)×0.008 = 7.2 → 0.95 で頭打ち
        assertEquals(0.95, DamagePipeline.overflowResist(1000, OVERFLOW), 1e-9);
        // null は常に 0
        assertEquals(0, DamagePipeline.overflowResist(9999, null), 1e-9);
    }

    @Test
    @DisplayName("ArmorOverflow: 異常値は健全な範囲に正規化される")
    void overflowRecordNormalizes() {
        ArmorOverflow o = new ArmorOverflow(-5, -1, 2.0);
        assertEquals(0, o.threshold());
        assertEquals(0, o.reductionPerPoint());
        assertEquals(1.0, o.maxReduction());
    }

    @Test
    @DisplayName("上限超過軽減: バニラ防具後の最終値にさらに ×(1-超過軽減) が乗る")
    void overflowReducesAfterArmor() {
        // armor 200, toughness 0。実効防御 200 → 超過軽減 0.8
        double armor = 200, toughness = 0;
        Result result = DamagePipeline.compute(NEUTRAL_ATTACKER, NEUTRAL_VICTIM,
                false, 10, true, armor, toughness, OVERFLOW, () -> 0.99);
        double newBase = 10 * result.multiplier();
        double actualFinal = newBase * DamagePipeline.armorFactor(newBase, armor, toughness);
        // 目標 = 裸の防具式(80%軽減)を通した値 × (1-0.8)
        double vanillaFinal = 10 * DamagePipeline.armorFactor(10, armor, toughness);
        assertEquals(vanillaFinal * 0.2, actualFinal, 1e-6);
    }

    @Test
    @DisplayName("上限超過軽減: overflow=null なら従来通り(防具のみ)")
    void overflowNullIsVanillaOnly() {
        Result with = DamagePipeline.compute(NEUTRAL_ATTACKER, NEUTRAL_VICTIM,
                false, 10, true, 200, 0, null, () -> 0.99);
        // 攻撃側中立・貫通なし・overflow なし → 倍率は素通し(1.0)
        assertEquals(1.0, with.multiplier(), 1e-9);
    }

    @Test
    @DisplayName("上限超過軽減: 貫通で実効防御が threshold を割れば超過軽減も消える")
    void penetrationCancelsOverflow() {
        // armor 200 だが貫通 60% → 実効防御 80 < threshold(100) → 超過軽減 0
        AttackerStats attacker = new AttackerStats(1, 0, 1.5, 0.60, 0, 1);
        Result result = DamagePipeline.compute(attacker, NEUTRAL_VICTIM,
                false, 10, true, 200, 0, OVERFLOW, () -> 0.99);
        double newBase = 10 * result.multiplier();
        double actualFinal = newBase * DamagePipeline.armorFactor(newBase, 200, 0);
        // 実効防御80のバニラ防具式だけを通した値に着地(追加軽減なし)
        double targetFinal = 10 * DamagePipeline.armorFactor(10, 80, 0);
        assertEquals(targetFinal, actualFinal, 1e-6);
    }

    @Test
    @DisplayName("上限超過軽減: bypasses_armor のダメージには乗らない")
    void overflowOnlyWhenArmorApplied() {
        Result bypassed = DamagePipeline.compute(NEUTRAL_ATTACKER, NEUTRAL_VICTIM,
                false, 10, false, 200, 0, OVERFLOW, () -> 0.99);
        assertEquals(1.0, bypassed.multiplier(), 1e-9);
    }

    @Test
    @DisplayName("上限超過軽減: 実数値貫通で実効防御が threshold を割れば超過軽減も消える")
    void flatPenetrationCancelsOverflow() {
        // armor 200 だが貫通値120 → 実効防御 80 < threshold(100) → 超過軽減 0
        AttackerStats attacker = new AttackerStats(1, 0, 1.5, 0, 120, 1);
        Result result = DamagePipeline.compute(attacker, NEUTRAL_VICTIM,
                false, 10, true, 200, 0, OVERFLOW, () -> 0.99);
        double newBase = 10 * result.multiplier();
        double actualFinal = newBase * DamagePipeline.armorFactor(newBase, 200, 0);
        // 実効防御80のバニラ防具式だけを通した値に着地(超過軽減なし)
        double targetFinal = 10 * DamagePipeline.armorFactor(10, 80, 0);
        assertEquals(targetFinal, actualFinal, 1e-6);
    }

    @Test
    @DisplayName("上限超過軽減: 実数値貫通は超過軽減を部分的に削る(実効防御150 → 軽減0.4)")
    void flatPenetrationPartiallyCutsOverflow() {
        // armor 200, 貫通値50 → 実効防御 150 → 超過軽減 (150-100)×0.008 = 0.4
        AttackerStats attacker = new AttackerStats(1, 0, 1.5, 0, 50, 1);
        Result result = DamagePipeline.compute(attacker, NEUTRAL_VICTIM,
                false, 10, true, 200, 0, OVERFLOW, () -> 0.99);
        double newBase = 10 * result.multiplier();
        double actualFinal = newBase * DamagePipeline.armorFactor(newBase, 200, 0);
        // 実効防御150のバニラ防具式 × (1 - 0.4)
        double targetFinal = 10 * DamagePipeline.armorFactor(10, 150, 0) * (1 - 0.4);
        assertEquals(targetFinal, actualFinal, 1e-6);
    }

    @Test
    @DisplayName("貫通(率+実数)は防具軽減と超過軽減を同時に削り、裸を超えない(タフネス込み)")
    void penetrationCutsBothFactorAndOverflowWithToughness() {
        // armor 400, toughness 8, 率0.25 + 実数20 → 実効防御 max(0, 400×0.75 - 20) = 280
        AttackerStats attacker = new AttackerStats(1, 0, 1.5, 0.25, 20, 1);
        Result result = DamagePipeline.compute(attacker, NEUTRAL_VICTIM,
                false, 10, true, 400, 8, OVERFLOW, () -> 0.99);
        double newBase = 10 * result.multiplier();
        double actualFinal = newBase * DamagePipeline.armorFactor(newBase, 400, 8);
        // 単一の実効防御280が防具式と超過軽減の両方に流れる
        double oResist = DamagePipeline.overflowResist(280, OVERFLOW); // (280-100)×0.008=1.44 → 0.95 で頭打ち
        double targetFinal = 10 * DamagePipeline.armorFactor(10, 280, 8) * (1 - oResist);
        assertEquals(targetFinal, actualFinal, 1e-6);
        assertTrue(actualFinal <= 10 + 1e-9, "裸の10を超過: " + actualFinal);
    }

    @Test
    @DisplayName("逆算ソルバー: 大きな目標値でも往復が一致する(flat/会心積み上げ対策)")
    void solverRoundTripLargeTarget() {
        for (double target : new double[]{100, 1000, 5000}) {
            for (double armor : new double[]{20, 100, 500}) {
                double solved = DamagePipeline.solveBaseForFinal(target, armor, 8);
                assertEquals(target, solved * DamagePipeline.armorFactor(solved, armor, 8), 1e-6,
                        "target=" + target + " armor=" + armor);
            }
        }
    }

    // ─── 実数値加算(flatBonus)とカテゴリ倍率(射撃・爆発) ───

    @Test
    @DisplayName("実数値加算: flat は base に上乗せされる((base+flat)/base 倍)")
    void flatBonusAddsToBase() {
        AttackerStats attacker = new AttackerStats(1, 0, 1.5, 0, 0, 1, 50, 1);
        // base=10, flat=50 → 最終 base は 60 相当 = 倍率 6.0(防具0なので逆算は走らない)
        Result result = compute(attacker, NEUTRAL_VICTIM);
        assertEquals(6.0, result.multiplier(), 1e-9);
    }

    @Test
    @DisplayName("実数値加算: flat は会心・元素%で増幅される(base 加算なので後段が乗る)")
    void flatBonusIsAmplifiedByCritAndElement() {
        // flat=50 → base 60 相当、元素1.5、会心2.0 → 6 × 1.5 × 2.0 = 18
        AttackerStats attacker = new AttackerStats(1.5, 1.0, 2.0, 0, 0, 1, 50, 1);
        Result result = DamagePipeline.compute(attacker, NEUTRAL_VICTIM,
                false, 10, true, 0, 0, () -> 0.0);
        assertTrue(result.critical());
        assertEquals(6.0 * 1.5 * 2.0, result.multiplier(), 1e-9);
    }

    @Test
    @DisplayName("カテゴリ倍率: 元素倍率と直交して掛かる(射撃×物理など)")
    void categoryMultiplierIsOrthogonal() {
        // 元素1.5 × カテゴリ2.0 = 3.0
        AttackerStats attacker = new AttackerStats(1.5, 0, 1.5, 0, 0, 1, 0, 2.0);
        Result result = compute(attacker, NEUTRAL_VICTIM);
        assertEquals(1.5 * 2.0, result.multiplier(), 1e-9);
    }

    @Test
    @DisplayName("6引数コンストラクタは flat=0・カテゴリ倍率=1 として既存挙動を保つ")
    void legacyConstructorIsNeutralFlat() {
        AttackerStats attacker = new AttackerStats(1, 0, 1.5, 0, 0, 1);
        assertEquals(0, attacker.flatBonus());
        assertEquals(1, attacker.categoryMultiplier());
        assertEquals(1.0, compute(attacker, NEUTRAL_VICTIM).multiplier(), 1e-9);
    }

    @Test
    @DisplayName("実数値加算 + 貫通100%: flat 込みの量(60)が裸での最終値に着地する")
    void flatBonusWithFullPenetration() {
        AttackerStats attacker = new AttackerStats(1, 0, 1.5, 1.0, 0, 1, 50, 1);
        Result result = DamagePipeline.compute(attacker, NEUTRAL_VICTIM,
                false, 10, true, 20, 0, () -> 0.99);
        double newBase = 10 * result.multiplier();
        // base10 + flat50 = 60 を裸で受けた最終値にピッタリ着地(防具で増幅されない)
        assertEquals(60.0, newBase * DamagePipeline.armorFactor(newBase, 20, 0), 1e-6);
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
