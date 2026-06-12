package DIV.attributelib.core;

import DIV.attributelib.api.AttributeType;

import java.util.List;

/**
 * 属性値の計算式(純粋関数)。Bukkit の状態に一切触れないため単体テスト可能。
 *
 * <p>式: {@code final = clamp( (base + ΣADD) × ΠMULTIPLY )}。
 * SET が1つでもあれば「最後の SET の値を clamp したもの」が全てに勝つ。</p>
 */
final class Calculator {

    private Calculator() {
    }

    /**
     * @param type      属性定義(clamp 範囲に使用)
     * @param base      基礎値(base 未設定なら呼び出し側が defaultValue を渡す)
     * @param modifiers この属性を対象とするモディファイアのみを、追加順で渡す
     */
    static double compute(AttributeType type, double base, List<Modifier> modifiers) {
        double add = 0;
        double multiply = 1;
        Double set = null;
        for (Modifier m : modifiers) {
            switch (m.operation()) {
                case ADD -> add += m.value();
                case MULTIPLY -> multiply *= m.value();
                case SET -> set = m.value();
            }
        }
        double result = set != null ? set : (base + add) * multiply;
        return type.clamp(result);
    }
}
