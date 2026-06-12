package DIV.attributelib.core;

import DIV.attributelib.api.Operation;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CodecTest {

    private static final NamespacedKey KEY = NamespacedKey.fromString("myplugin:dodge_chance");

    @Test
    @DisplayName("base 行はラウンドトリップで完全一致する")
    void baseRoundTrip() {
        String line = Codec.formatBase(KEY, -3.25);
        Codec.BaseEntry entry = Codec.parseBase(line);
        assertEquals(KEY, entry.attribute());
        assertEquals(-3.25, entry.value());
    }

    @Test
    @DisplayName("壊れた base 行は null になる(例外を投げない)")
    void baseMalformed() {
        assertNull(Codec.parseBase("noseparator"));
        assertNull(Codec.parseBase("a|b|c"));
        assertNull(Codec.parseBase("INVALID KEY!|1.0"));
        assertNull(Codec.parseBase("myplugin:x|notanumber"));
        assertNull(Codec.parseBase("myplugin:x|Infinity"));
    }

    @Test
    @DisplayName("modifier 行はラウンドトリップで全フィールド一致する(persistent のみ保存)")
    void modifierRoundTrip() {
        Modifier original = new Modifier(KEY, "enhancedmobs:trait/swift", Operation.MULTIPLY, 1.5, 123456789L, true);
        Modifier restored = Codec.parseModifier(Codec.formatModifier(original));
        assertEquals(original, restored);

        Modifier permanent = new Modifier(KEY, "a:b", Operation.SET, 0.0, Modifier.PERMANENT, true);
        assertEquals(permanent, Codec.parseModifier(Codec.formatModifier(permanent)));
    }

    @Test
    @DisplayName("壊れた modifier 行は null になる(例外を投げない)")
    void modifierMalformed() {
        assertNull(Codec.parseModifier(""));
        assertNull(Codec.parseModifier("a|b|c|d"));                            // フィールド不足
        assertNull(Codec.parseModifier("BAD KEY!|src|ADD|1.0|-1"));            // 不正キー
        assertNull(Codec.parseModifier("myplugin:x|src|NOTANOP|1.0|-1"));      // 不正演算
        assertNull(Codec.parseModifier("myplugin:x|src|ADD|xx|-1"));           // 不正数値
        assertNull(Codec.parseModifier("myplugin:x|src|ADD|1.0|zz"));          // 不正期限
        assertNull(Codec.parseModifier("myplugin:x||ADD|1.0|-1"));             // 空 sourceId
        assertNull(Codec.parseModifier("myplugin:x|src|ADD|1.0|-99"));         // 不正な負の期限
    }
}
