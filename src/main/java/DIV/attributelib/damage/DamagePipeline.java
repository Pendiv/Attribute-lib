package DIV.attributelib.damage;

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
     * @param elementDamage 元素与ダメ倍率(無属性ダメージなら 1.0 を渡す)
     */
    public record AttackerStats(
            double elementDamage,
            double critChance,
            double critDamage,
            double armorPenetration,
            double armorPenetrationFlat,
            double damageDealt
    ) {
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

    private DamagePipeline() {
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
        double multiplier = 1.0;

        // 1. 元素倍率
        if (attacker != null) {
            multiplier *= attacker.elementDamage();
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

        // 4. 部分貫通(企画書 §8-1)。最後に適用するのは、貫通の逆算が
        //    「他の全倍率を掛けた後のダメージ量」に依存するため。
        //    バニラ防具式はダメージ量に対して非線形なので、単純な軽減率の比を
        //    掛けるとバニラ側が増えた base で防具を再評価して軽減が二重に弱まり、
        //    装備持ちの方がダメージを受ける逆転が起きる(実機で確認済みのバグ)。
        //    正しくは「新しい base にバニラ防具式を通した結果が、貫通後の armor で
        //    計算した目標値に一致する」base を逆算する。
        if (attacker != null && armorApplied && armor > 0 && multiplier > 0
                && (attacker.armorPenetration() > 0 || attacker.armorPenetrationFlat() > 0)) {
            double scaled = baseDamage * multiplier;
            double effectiveArmor = Math.max(0,
                    armor * (1 - attacker.armorPenetration()) - attacker.armorPenetrationFlat());
            double targetFinal = scaled * armorFactor(scaled, effectiveArmor, toughness);
            multiplier = solveBaseForFinal(targetFinal, armor, toughness) / baseDamage;
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
