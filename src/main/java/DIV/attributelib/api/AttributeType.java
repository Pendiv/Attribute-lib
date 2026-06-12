package DIV.attributelib.api;

import org.bukkit.NamespacedKey;

/**
 * カスタム属性の定義。{@link Attributes#register} が返す正規インスタンスを
 * 定数として保持して使う。
 *
 * @param key          属性ID(登録元プラグインの名前空間。衝突しない)
 * @param defaultValue base 未設定時の基礎値
 * @param min          最終値の下限(不要なら -Double.MAX_VALUE)
 * @param max          最終値の上限(不要なら Double.MAX_VALUE)
 */
public record AttributeType(NamespacedKey key, double defaultValue, double min, double max) {

    public AttributeType {
        if (!Double.isFinite(defaultValue)) {
            throw new IllegalArgumentException("defaultValue が有限値ではありません: " + defaultValue);
        }
        if (min > max) {
            throw new IllegalArgumentException("min(" + min + ") > max(" + max + ")");
        }
        if (defaultValue < min || defaultValue > max) {
            throw new IllegalArgumentException(
                    "defaultValue(" + defaultValue + ") が範囲 [" + min + ", " + max + "] の外です");
        }
    }

    /** 値をこの属性の [min, max] に収める。 */
    public double clamp(double value) {
        return Math.clamp(value, min, max);
    }
}
