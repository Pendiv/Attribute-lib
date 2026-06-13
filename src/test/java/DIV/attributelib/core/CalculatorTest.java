package DIV.attributelib.core;

import DIV.attributelib.api.AttributeType;
import DIV.attributelib.api.Operation;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculatorTest {

    private static final NamespacedKey KEY = NamespacedKey.fromString("test:attr");

    private static AttributeType unbounded(double def) {
        return new AttributeType(KEY, def, -Double.MAX_VALUE, Double.MAX_VALUE);
    }

    private static Modifier mod(Operation op, double value) {
        return new Modifier(KEY, "test:source", op, value, Modifier.PERMANENT, true);
    }

    @Test
    @DisplayName("モディファイアなしなら base がそのまま返る")
    void noModifiers() {
        assertEquals(7.5, Calculator.compute(unbounded(0), 7.5, List.of()));
    }

    @Test
    @DisplayName("ADD は合計が base に加算される")
    void addSums() {
        double result = Calculator.compute(unbounded(0), 10,
                List.of(mod(Operation.ADD, 5), mod(Operation.ADD, -2)));
        assertEquals(13, result);
    }

    @Test
    @DisplayName("MULTIPLY は積で合成される")
    void multiplyCompounds() {
        double result = Calculator.compute(unbounded(0), 10,
                List.of(mod(Operation.MULTIPLY, 1.5), mod(Operation.MULTIPLY, 2)));
        assertEquals(30, result);
    }

    @Test
    @DisplayName("式は (base + ΣADD) × ΠMULTIPLY の順で適用される")
    void addBeforeMultiply() {
        // 追加順を逆(MULTIPLY が先)にしても結果は変わらないこと
        double result = Calculator.compute(unbounded(0), 10,
                List.of(mod(Operation.MULTIPLY, 2), mod(Operation.ADD, 5)));
        assertEquals(30, result);
    }

    @Test
    @DisplayName("SET があれば他の全てに勝つ")
    void setOverridesEverything() {
        double result = Calculator.compute(unbounded(0), 10,
                List.of(mod(Operation.ADD, 100), mod(Operation.SET, 1), mod(Operation.MULTIPLY, 50)));
        assertEquals(1, result);
    }

    @Test
    @DisplayName("SET が複数なら最後に追加されたものが勝つ")
    void lastSetWins() {
        double result = Calculator.compute(unbounded(0), 10,
                List.of(mod(Operation.SET, 1), mod(Operation.SET, 99)));
        assertEquals(99, result);
    }

    @Test
    @DisplayName("最終値は属性の範囲にクランプされる(SET 含む)")
    void clamps() {
        AttributeType chance = new AttributeType(KEY, 0, 0, 100);
        assertEquals(100, Calculator.compute(chance, 50, List.of(mod(Operation.ADD, 500))));
        assertEquals(0, Calculator.compute(chance, 50, List.of(mod(Operation.ADD, -500))));
        assertEquals(100, Calculator.compute(chance, 0, List.of(mod(Operation.SET, 12345))));
    }

    @Test
    @DisplayName("AttributeType は不正な定義を拒否する")
    void attributeTypeValidation() {
        assertThrows(IllegalArgumentException.class, () -> new AttributeType(KEY, Double.NaN, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new AttributeType(KEY, 0, 10, 1));
        assertThrows(IllegalArgumentException.class, () -> new AttributeType(KEY, 50, 0, 10));
    }

    @Test
    @DisplayName("Modifier は不正な引数を拒否する")
    void modifierValidation() {
        assertThrows(IllegalArgumentException.class,
                () -> new Modifier(KEY, "", Operation.ADD, 1, Modifier.PERMANENT, true));
        assertThrows(IllegalArgumentException.class,
                () -> new Modifier(KEY, "a|b", Operation.ADD, 1, Modifier.PERMANENT, true));
        assertThrows(IllegalArgumentException.class,
                () -> new Modifier(KEY, "test:s", Operation.ADD, Double.POSITIVE_INFINITY, Modifier.PERMANENT, true));
        assertThrows(IllegalArgumentException.class,
                () -> new Modifier(KEY, "test:s", Operation.ADD, 1, -5, true));
    }

    @Test
    @DisplayName("null の operation / attribute は add 時点で弾く(読み取り時の遅延 NPE を防ぐ)")
    void rejectsNullOperationAndAttributeEarly() {
        assertThrows(IllegalArgumentException.class,
                () -> new Modifier(KEY, "test:s", null, 1, Modifier.PERMANENT, true));
        assertThrows(IllegalArgumentException.class,
                () -> new Modifier(null, "test:s", Operation.ADD, 1, Modifier.PERMANENT, true));
    }

    @Test
    @DisplayName("期限判定は gameTime >= expiresAt で失効、PERMANENT は失効しない")
    void expiry() {
        Modifier timed = new Modifier(KEY, "test:s", Operation.ADD, 1, 100, true);
        org.junit.jupiter.api.Assertions.assertFalse(timed.isExpired(99));
        org.junit.jupiter.api.Assertions.assertTrue(timed.isExpired(100));
        Modifier permanent = mod(Operation.ADD, 1);
        org.junit.jupiter.api.Assertions.assertFalse(permanent.isExpired(Long.MAX_VALUE));
    }
}
