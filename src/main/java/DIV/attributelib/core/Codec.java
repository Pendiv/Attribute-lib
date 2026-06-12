package DIV.attributelib.core;

import DIV.attributelib.api.Operation;
import org.bukkit.NamespacedKey;

/**
 * PDC へ保存する文字列形式との相互変換。
 *
 * <ul>
 *   <li>base 行:       {@code ns:key|value}</li>
 *   <li>modifier 行:   {@code ns:key|sourceId|op|value|expiresAt}</li>
 * </ul>
 * '|' は {@link Modifier} のバリデーションで sourceId に入らないことが保証されており、
 * NamespacedKey の文字集合([a-z0-9._-/:])にも含まれない。
 *
 * <p>parse 系は壊れた行に対して null を返す(例外を投げない)。壊れたデータを
 * 「静かに無いことにする」のは事故源なので、呼び出し側で必ず警告ログを出すこと。</p>
 */
final class Codec {

    private Codec() {
    }

    static String formatBase(NamespacedKey attribute, double value) {
        return attribute.toString() + "|" + value;
    }

    /** @return {attribute, value} のペア。壊れた行なら null。 */
    static BaseEntry parseBase(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length != 2) {
            return null;
        }
        NamespacedKey key = NamespacedKey.fromString(parts[0]);
        if (key == null) {
            return null;
        }
        double value;
        try {
            value = Double.parseDouble(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }
        if (!Double.isFinite(value)) {
            return null;
        }
        return new BaseEntry(key, value);
    }

    record BaseEntry(NamespacedKey attribute, double value) {
    }

    static String formatModifier(Modifier m) {
        return m.attribute().toString() + "|" + m.sourceId() + "|" + m.operation().name()
                + "|" + m.value() + "|" + m.expiresAt();
    }

    /** @return 復元した Modifier(persistent=true)。壊れた行なら null。 */
    static Modifier parseModifier(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length != 5) {
            return null;
        }
        NamespacedKey key = NamespacedKey.fromString(parts[0]);
        if (key == null) {
            return null;
        }
        try {
            Operation op = Operation.valueOf(parts[2]);
            double value = Double.parseDouble(parts[3]);
            long expiresAt = Long.parseLong(parts[4]);
            return new Modifier(key, parts[1], op, value, expiresAt, true);
        } catch (IllegalArgumentException e) {
            // Operation.valueOf / parseDouble / parseLong / Modifier コンストラクタ検証の全失敗がここ
            return null;
        }
    }
}
