package DIV.attributelib.equip;

import DIV.attributelib.api.AttributeType;
import DIV.attributelib.api.ItemModifier;
import DIV.attributelib.api.Operation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoreComposerTest {

    private static final NamespacedKey FLAT_KEY = NamespacedKey.fromString("test:flat_attr");
    private static final NamespacedKey PERCENT_KEY = NamespacedKey.fromString("test:percent_attr");

    private static final AttributeType FLAT = new AttributeType(
            FLAT_KEY, 0, -Double.MAX_VALUE, Double.MAX_VALUE, Component.text("貫通値"), false);
    private static final AttributeType PERCENT = new AttributeType(
            PERCENT_KEY, 0, -Double.MAX_VALUE, Double.MAX_VALUE, Component.text("会心率"), true);

    private static final Function<NamespacedKey, AttributeType> RESOLVER =
            Map.of(FLAT_KEY, FLAT, PERCENT_KEY, PERCENT)::get;

    private static final Function<NamespacedKey, Component> COND =
            key -> Component.text("夜間");

    private static TranslatableComponent asTranslatable(Component component) {
        return assertInstanceOf(TranslatableComponent.class, component);
    }

    @Test
    @DisplayName("ヘッダ: 空行 + 灰色の item.modifiers.<group>(バニラの文法)")
    void header() {
        List<Component> lines = LoreComposer.buildSection(
                List.of(new ItemModifier(FLAT_KEY, Operation.ADD, 5, EquipmentSlotGroup.MAINHAND)), RESOLVER, COND);
        assertEquals(3, lines.size());
        assertEquals(Component.empty(), lines.getFirst());
        TranslatableComponent header = asTranslatable(lines.get(1));
        assertEquals("item.modifiers.mainhand", header.key());
        assertEquals(NamedTextColor.GRAY, header.color());
    }

    @Test
    @DisplayName("加算プラスは青の plus.0、マイナスは赤の take.0(値は絶対値)")
    void addLine() {
        List<Component> lines = LoreComposer.buildSection(List.of(
                new ItemModifier(FLAT_KEY, Operation.ADD, 5, EquipmentSlotGroup.MAINHAND),
                new ItemModifier(FLAT_KEY, Operation.ADD, -2.5, EquipmentSlotGroup.MAINHAND)), RESOLVER, COND);
        TranslatableComponent plus = asTranslatable(lines.get(2));
        assertEquals("attribute.modifier.plus.0", plus.key());
        assertEquals(NamedTextColor.BLUE, plus.color());
        assertEquals("5", ((net.kyori.adventure.text.TextComponent) plus.arguments().getFirst().asComponent()).content());

        TranslatableComponent take = asTranslatable(lines.get(3));
        assertEquals("attribute.modifier.take.0", take.key());
        assertEquals(NamedTextColor.RED, take.color());
        assertEquals("2.5", ((net.kyori.adventure.text.TextComponent) take.arguments().getFirst().asComponent()).content());
    }

    @Test
    @DisplayName("percentDisplay 属性の ADD は百分率(plus.1、0.25 → 25)")
    void percentAttributeAdd() {
        List<Component> lines = LoreComposer.buildSection(List.of(
                new ItemModifier(PERCENT_KEY, Operation.ADD, 0.25, EquipmentSlotGroup.MAINHAND)), RESOLVER, COND);
        TranslatableComponent line = asTranslatable(lines.get(2));
        assertEquals("attribute.modifier.plus.1", line.key());
        assertEquals("25", ((net.kyori.adventure.text.TextComponent) line.arguments().getFirst().asComponent()).content());
    }

    @Test
    @DisplayName("MULTIPLY は係数の1からの差分を百分率で(1.2 → +20%、0.8 → -20%)")
    void multiplyLine() {
        List<Component> lines = LoreComposer.buildSection(List.of(
                new ItemModifier(FLAT_KEY, Operation.MULTIPLY, 1.2, EquipmentSlotGroup.MAINHAND),
                new ItemModifier(FLAT_KEY, Operation.MULTIPLY, 0.8, EquipmentSlotGroup.MAINHAND)), RESOLVER, COND);
        TranslatableComponent up = asTranslatable(lines.get(2));
        assertEquals("attribute.modifier.plus.1", up.key());
        assertEquals(NamedTextColor.BLUE, up.color());
        assertEquals("20", ((net.kyori.adventure.text.TextComponent) up.arguments().getFirst().asComponent()).content());

        TranslatableComponent down = asTranslatable(lines.get(3));
        assertEquals("attribute.modifier.take.1", down.key());
        assertEquals(NamedTextColor.RED, down.color());
        assertEquals("20", ((net.kyori.adventure.text.TextComponent) down.arguments().getFirst().asComponent()).content());
    }

    @Test
    @DisplayName("SET は深緑の equals(バニラの基準値表示と同じ)")
    void setLine() {
        List<Component> lines = LoreComposer.buildSection(List.of(
                new ItemModifier(FLAT_KEY, Operation.SET, 7, EquipmentSlotGroup.MAINHAND),
                new ItemModifier(PERCENT_KEY, Operation.SET, 0.5, EquipmentSlotGroup.MAINHAND)), RESOLVER, COND);
        TranslatableComponent flat = asTranslatable(lines.get(2));
        assertEquals("attribute.modifier.equals.0", flat.key());
        assertEquals(NamedTextColor.DARK_GREEN, flat.color());

        TranslatableComponent percent = asTranslatable(lines.get(3));
        assertEquals("attribute.modifier.equals.1", percent.key());
        assertEquals("50", ((net.kyori.adventure.text.TextComponent) percent.arguments().getFirst().asComponent()).content());
    }

    @Test
    @DisplayName("グループはバニラのツールチップ順(mainhand → feet → head)で並ぶ")
    void groupOrder() {
        List<Component> lines = LoreComposer.buildSection(List.of(
                new ItemModifier(FLAT_KEY, Operation.ADD, 1, EquipmentSlotGroup.HEAD),
                new ItemModifier(FLAT_KEY, Operation.ADD, 1, EquipmentSlotGroup.MAINHAND),
                new ItemModifier(FLAT_KEY, Operation.ADD, 1, EquipmentSlotGroup.FEET)), RESOLVER, COND);
        List<String> headers = lines.stream()
                .filter(c -> c instanceof TranslatableComponent t && t.key().startsWith("item.modifiers."))
                .map(c -> ((TranslatableComponent) c).key())
                .toList();
        assertEquals(List.of("item.modifiers.mainhand", "item.modifiers.feet", "item.modifiers.head"), headers);
    }

    @Test
    @DisplayName("未登録属性は ID をそのまま表示名にする(落ちない)")
    void unknownAttributeFallsBack() {
        List<Component> lines = LoreComposer.buildSection(List.of(
                new ItemModifier(NamespacedKey.fromString("other:unknown"), Operation.ADD, 1,
                        EquipmentSlotGroup.MAINHAND)), key -> null, COND);
        assertEquals(3, lines.size());
    }

    @Test
    @DisplayName("条件付きモディファイアには灰色の条件サフィックスが付く")
    void conditionSuffix() {
        List<Component> lines = LoreComposer.buildSection(List.of(
                new ItemModifier(FLAT_KEY, Operation.ADD, 5, EquipmentSlotGroup.MAINHAND,
                        NamespacedKey.fromString("attributelib:night"))), RESOLVER, COND);
        Component line = lines.get(2);
        // 子に「 (」+条件名+「)」が付いている
        assertEquals(1, line.children().size());
        Component suffix = line.children().getFirst();
        assertEquals(NamedTextColor.GRAY, suffix.color());
    }

    @Test
    @DisplayName("splice: 旧セクションをその位置で置換し、無ければ末尾に追加する")
    void splice() {
        List<Component> userLore = List.of(Component.text("説明文1"), Component.text("説明文2"));
        List<Component> section = List.of(Component.text("A"), Component.text("B"));

        // 初回: 末尾追加
        LoreComposer.Splice first = LoreComposer.splice(userLore, -1, 0, section);
        assertEquals(4, first.lore().size());
        assertEquals(2, first.start());
        assertEquals(2, first.length());

        // 更新: 同位置で置換(行数が変わっても破綻しない)
        List<Component> bigger = List.of(Component.text("A"), Component.text("B"), Component.text("C"));
        LoreComposer.Splice second = LoreComposer.splice(first.lore(), first.start(), first.length(), bigger);
        assertEquals(5, second.lore().size());
        assertEquals(Component.text("説明文1"), second.lore().getFirst());
        assertEquals(2, second.start());
        assertEquals(3, second.length());

        // 除去: 空セクション
        LoreComposer.Splice removed = LoreComposer.splice(second.lore(), second.start(), second.length(), List.of());
        assertEquals(userLore, removed.lore());
        assertEquals(-1, removed.start());
    }

    @Test
    @DisplayName("splice: 範囲が壊れていたら(他プラグインの lore 改変)末尾追加にフォールバック")
    void spliceBrokenRange() {
        List<Component> lore = List.of(Component.text("x"));
        LoreComposer.Splice result = LoreComposer.splice(lore, 5, 3, List.of(Component.text("A")));
        assertEquals(2, result.lore().size());
        assertEquals(1, result.start());
        assertTrue(result.lore().contains(Component.text("x")));
    }
}
