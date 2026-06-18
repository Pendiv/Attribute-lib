package DIV.attributelib.damage;

import org.jspecify.annotations.Nullable;

import java.util.function.DoubleSupplier;

/**
 * ダメージ倍率の計算(純粋関数)。Bukkit の状態に触れないため単体テスト可能。
 *
 * <p>適用順: 元素倍率 → 会心 → 全体倍率 → 部分貫通の防具逆算。
 * 倍率段は全て乗算なので順序は結果に影響しないが、貫通だけは
 * 「他の倍率を全て掛けた後のダメージ量」に依存するため最後に置く(実装コメント参照)。
 * 最終的な適用は {@code setDamage(base × multiplier)} の比例再スケール1回で済む。
 * 確定ダメージはここに来る前に弾かれる(リスナーが早期 return)。</p>
 */
public final class DamagePipeline {

    /**
     * 攻撃側の関係属性スナップショット。
     *
     * @param elementDamage      元素与ダメ倍率(無属性ダメージなら 1.0 を渡す)
     * @param flatBonus          base に上乗せする実数値ダメージの合計(全体 + 元素 + カテゴリ分を
     *                           リスナーが事前合算して渡す)。base に加算されるため会心・%・防具計算が乗る
     * @param categoryMultiplier 射撃・爆発などカテゴリ与ダメ倍率の積(該当しなければ 1.0)
     */
    public record AttackerStats(
            double elementDamage,
            double critChance,
            double critDamage,
            double armorPenetration,
            double armorPenetrationFlat,
            double damageDealt,
            double flatBonus,
            double categoryMultiplier
    ) {
        /** flat / カテゴリ倍率なしの簡易生成(flatBonus=0, categoryMultiplier=1)。 */
        public AttackerStats(double elementDamage, double critChance, double critDamage,
                             double armorPenetration, double armorPenetrationFlat, double damageDealt) {
            this(elementDamage, critChance, critDamage, armorPenetration, armorPenetrationFlat,
                    damageDealt, 0, 1);
        }
    }

    /**
     * 防御側の関係属性スナップショット。
     *
     * @param elementResist 元素軽減割合(無属性ダメージなら 0.0 を渡す)
     */
    public record VictimStats(
            double elementResist,
            double critResist,
            double damageTaken
    ) {
    }

    /**
     * @param multiplier base ダメージに掛ける最終倍率(0 以上)
     * @param critical   このライブラリの会心が発動したか
     */
    public record Result(double multiplier, boolean critical) {
        public static final Result NEUTRAL = new Result(1.0, false);
    }

    /**
     * バニラ防具の上限超過分をダメージ軽減に変換する設定。
     *
     * <p>バニラ防具式は防御 100(armor/5 枝が 20 に到達)で軽減 80% に頭打ちし、それ以上の
     * 防御値は無意味になる。その「死に値」を乗算軽減として活かすための変換。
     * 変換は {@code (実効防御 − threshold) × reductionPerPoint} を {@code maxReduction} で頭打ちし、
     * 防具計算後のダメージにさらに {@code ×(1 − その軽減)} を掛ける形で適用される。</p>
     *
     * @param threshold        この実効防御値を超えた分が軽減に変換される(バニラが 80% に達する点 = 100 が既定)
     * @param reductionPerPoint 超過防御 1 ポイントあたりの追加軽減(0.008 = バニラ armor/5 枝と同率)
     * @param maxReduction     追加軽減の上限([0,1)。完全無敵化を防ぐ)
     */
    public record ArmorOverflow(double threshold, double reductionPerPoint, double maxReduction) {
        public ArmorOverflow {
            threshold = Math.max(0, threshold);
            reductionPerPoint = Math.max(0, reductionPerPoint);
            maxReduction = Math.clamp(maxReduction, 0, 1);
        }
    }

    private DamagePipeline() {
    }

    /**
     * 上限超過防御による追加軽減割合(0..maxReduction)。{@code overflow} が null か
     * 実効防御が threshold 以下なら 0。
     */
    public static double overflowResist(double effectiveArmor, @Nullable ArmorOverflow overflow) {
        if (overflow == null || effectiveArmor <= overflow.threshold()) {
            return 0;
        }
        return Math.min(overflow.maxReduction(),
                (effectiveArmor - overflow.threshold()) * overflow.reductionPerPoint());
    }

    /**
     * @param attacker        攻撃者の属性。攻撃者なし(環境ダメージ)なら null
     * @param vanillaCritical バニラのジャンプ会心が出た攻撃か(true なら会心を重複させない。企画書 §8-7)
     * @param baseDamage      イベントの base ダメージ(防具式の入力に使用)
     * @param armorApplied    バニラ防具計算が適用されるダメージか(bypasses_armor でない)
     * @param armor           被害者の armor 最終値
     * @param toughness       被害者の armor_toughness 最終値
     * @param random          会心抽選の乱数源 [0,1)(テストでは固定値を注入)
     */
    public static Result compute(AttackerStats attacker,
                                 VictimStats victim,
                                 boolean vanillaCritical,
                                 double baseDamage,
                                 boolean armorApplied,
                                 double armor,
                                 double toughness,
                                 DoubleSupplier random) {
        return compute(attacker, victim, vanillaCritical, baseDamage,
                armorApplied, armor, toughness, null, random);
    }

    /**
     * 上限超過防御の軽減化({@link ArmorOverflow})を加味する版。{@code overflow} が null なら
     * 上の8引数版と同じ挙動。貫通の逆算と同じ二分法に統合されるため、両者は無矛盾に合成される。
     */
    public static Result compute(AttackerStats attacker,
                                 VictimStats victim,
                                 boolean vanillaCritical,
                                 double baseDamage,
                                 boolean armorApplied,
                                 double armor,
                                 double toughness,
                                 @Nullable ArmorOverflow overflow,
                                 DoubleSupplier random) {
        double multiplier = 1.0;

        // 1. 実数値加算(base 上乗せ)→ 元素倍率 → カテゴリ倍率 → 元素軽減。
        //    flat は「base に加算してから会心・%・防具が乗る」意味論なので、最終値が
        //    base × multiplier であることを使い、(base + flat) / base 倍として畳み込む。
        //    以降の倍率が全てこの上乗せ後の量に掛かり、貫通の二分法も scaled = base × multiplier
        //    経由で自動的に flat を含む(防具は通常どおり減算される)。
        if (attacker != null) {
            if (baseDamage > 0 && attacker.flatBonus() != 0) {
                multiplier *= (baseDamage + attacker.flatBonus()) / baseDamage;
            }
            multiplier *= attacker.elementDamage();
            multiplier *= attacker.categoryMultiplier();
        }
        multiplier *= Math.max(0, 1 - victim.elementResist());

        // 2. 会心(攻撃者がいる場合のみ。バニラ会心が出ていれば追い打ちしない)
        boolean critical = false;
        if (attacker != null && !vanillaCritical) {
            double chance = Math.clamp(attacker.critChance() - victim.critResist(), 0, 1);
            if (chance > 0 && random.getAsDouble() < chance) {
                critical = true;
                multiplier *= attacker.critDamage();
            }
        }

        // 3. 全体倍率
        if (attacker != null) {
            multiplier *= attacker.damageDealt();
        }
        multiplier *= victim.damageTaken();
        multiplier = Math.max(0, multiplier);

        // 4. 防具(部分貫通 + 上限超過分の軽減化)。最後に適用するのは、逆算が
        //    「他の全倍率を掛けた後のダメージ量」に依存するため。
        //    バニラ防具式はダメージ量に対して非線形なので、単純な軽減率の比を
        //    掛けるとバニラ側が増えた base で防具を再評価して軽減が二重に弱まり、
        //    装備持ちの方がダメージを受ける逆転が起きる(実機で確認済みのバグ)。
        //    正しくは「新しい base にバニラ防具式を通した結果が、貫通後の実効防御で
        //    計算した目標値(+ 超過分の追加軽減)に一致する」base を逆算する。
        //    貫通と上限超過軽減はどちらも実効防御に基づくので二分法に統合できる
        //    (貫通で実効防御が threshold を割れば超過軽減も自動的に消える)。
        if (armorApplied && armor > 0 && baseDamage > 0 && multiplier > 0) {
            double pen = attacker != null ? attacker.armorPenetration() : 0;
            double penFlat = attacker != null ? attacker.armorPenetrationFlat() : 0;
            double effectiveArmor = Math.max(0, armor * (1 - pen) - penFlat);
            double oResist = overflowResist(effectiveArmor, overflow);
            if (pen > 0 || penFlat > 0 || oResist > 0) {
                double scaled = baseDamage * multiplier;
                double targetFinal = scaled * armorFactor(scaled, effectiveArmor, toughness) * (1 - oResist);
                multiplier = solveBaseForFinal(targetFinal, armor, toughness) / baseDamage;
            }
        }

        return new Result(multiplier, critical);
    }

    /**
     * バニラ防具式を通した後の最終値が targetFinal になる base ダメージを逆算する。
     * {@code f(b) = b × armorFactor(b, armor, toughness)} は b に対して単調増加なので
     * 二分法で解く(貫通が有効な被弾時のみ呼ばれる)。
     */
    public static double solveBaseForFinal(double targetFinal, double armor, double toughness) {
        if (targetFinal <= 0) {
            return 0;
        }
        if (armor <= 0) {
            return targetFinal;
        }
        // 軽減は最大 80%(reduction cap 20/25)なので解は [target, target×5] に必ずある
        double lo = targetFinal;
        double hi = targetFinal * 5;
        for (int i = 0; i < 60; i++) {
            double mid = (lo + hi) / 2;
            if (mid * armorFactor(mid, armor, toughness) < targetFinal) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return (lo + hi) / 2;
    }

    /**
     * バニラ防具式: 軽減後/軽減前 のダメージ比率。
     * <pre>reduction = min(20, max(armor/5, armor - 4d/(toughness+8))) / 25</pre>
     */
    public static double armorFactor(double damage, double armor, double toughness) {
        double reduction = Math.min(20, Math.max(armor / 5, armor - 4 * damage / (toughness + 8))) / 25;
        return 1 - Math.max(0, reduction);
    }
}
