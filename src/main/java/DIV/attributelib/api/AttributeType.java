package DIV.attributelib.api;

import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;

import java.util.Locale;

/**
 * カスタム属性の定義。{@link Attributes#register} が返す正規インスタンスを
 * 定数として保持して使う。
 *
 * @param key            属性ID(登録元プラグインの名前空間。衝突しない)
 * @param defaultValue   base 未設定時の基礎値
 * @param min            最終値の下限(不要なら -Double.MAX_VALUE)
 * @param max            最終値の上限(不要なら Double.MAX_VALUE)
 * @param displayName    lore 表示などで使う表示名
 * @param percentDisplay true なら ADD/SET の値を百分率で表示する(0.25 → +25%)。
 *                       確率・割合・倍率差分のセマンティクスを持つ属性向け。
 *                       MULTIPLY は常に百分率表示(バニラの乗算表示と同じ)
 */
public record AttributeType(
        NamespacedKey key,
        double defaultValue,
        double min,
        double max,
        Component displayName,
        boolean percentDisplay
) {

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
        // displayName コンポーネントは非 null(下で正規化)。内部の register 経由では
        // null が渡され得るため(@Nullable な引数を許す)、ここで prettify に正規化する。
        if (displayName == null) {
            displayName = prettify(key);
        }
    }

    /** 表示名は ID から自動生成("dodge_chance" → "Dodge Chance")、％表示なし。 */
    public AttributeType(NamespacedKey key, double defaultValue, double min, double max) {
        this(key, defaultValue, min, max, prettify(key), false);
    }

    /** 値をこの属性の [min, max] に収める。 */
    public double clamp(double value) {
        return Math.clamp(value, min, max);
    }

    private static Component prettify(NamespacedKey key) {
        String[] words = key.getKey().split("_");
        StringBuilder name = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!name.isEmpty()) {
                name.append(' ');
            }
            name.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return Component.text(name.toString());
    }
}
