package DIV.attributelib.equip;

import DIV.attributelib.api.AttributeType;
import DIV.attributelib.api.ItemModifier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.EquipmentSlotGroup;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * カスタム属性の lore セクションを組み立てる(lore 書き込みの集約点、層4)。
 *
 * <p>表示はバニラのツールチップ文法に忠実に従う:</p>
 * <ul>
 *   <li>スロットヘッダ: 空行 + 灰色の translatable(クライアント言語で
 *       「メインハンド:」等に翻訳される)。並び順もバニラの EquipmentSlotGroup 順</li>
 *   <li>加算プラス = 青 {@code attribute.modifier.plus.0}、マイナス = 赤 {@code take.0}</li>
 *   <li>百分率(乗算、または percentDisplay 属性)= {@code plus.1 / take.1}</li>
 *   <li>SET = 深緑 {@code equals.0 / equals.1}(バニラの基準値表示と同じ色)</li>
 *   <li>数値フォーマットはバニラと同じ {@code #.##}</li>
 * </ul>
 */
public final class LoreComposer {

    /** バニラの ATTRIBUTE_MODIFIER_FORMAT と同一。 */
    private static final DecimalFormat FORMAT =
            new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ROOT));

    /** バニラのツールチップ描画順(EquipmentSlotGroup の宣言順)。 */
    private static final List<EquipmentSlotGroup> GROUP_ORDER = List.of(
            EquipmentSlotGroup.ANY, EquipmentSlotGroup.MAINHAND, EquipmentSlotGroup.OFFHAND,
            EquipmentSlotGroup.HAND, EquipmentSlotGroup.FEET, EquipmentSlotGroup.LEGS,
            EquipmentSlotGroup.CHEST, EquipmentSlotGroup.HEAD, EquipmentSlotGroup.ARMOR,
            EquipmentSlotGroup.BODY, EquipmentSlotGroup.SADDLE);

    private LoreComposer() {
    }

    /**
     * モディファイア一覧から lore セクション(行リスト)を組み立てる。
     *
     * @param typeResolver 属性キー → 定義の解決(未登録属性は ID をそのまま表示名にする)
     */
    public static List<Component> buildSection(List<ItemModifier> modifiers,
                                               Function<NamespacedKey, AttributeType> typeResolver) {
        List<Component> lines = new ArrayList<>();
        for (EquipmentSlotGroup group : GROUP_ORDER) {
            List<ItemModifier> inGroup = modifiers.stream()
                    .filter(m -> m.slot().equals(group))
                    .toList();
            if (inGroup.isEmpty()) {
                continue;
            }
            lines.add(Component.empty());
            lines.add(Component.translatable("item.modifiers." + group)
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            for (ItemModifier modifier : inGroup) {
                AttributeType type = typeResolver.apply(modifier.attribute());
                Component name = type != null
                        ? type.displayName()
                        : Component.text(modifier.attribute().toString());
                boolean percent = type != null && type.percentDisplay();
                lines.add(line(modifier, name, percent));
            }
        }
        return lines;
    }

    /**
     * 既存 lore 内の旧セクションを新セクションで置き換える(純粋関数)。
     * 旧範囲が不正(他プラグインによる lore 改変等)なら末尾へ追加にフォールバックする。
     *
     * @param oldStart  旧セクションの開始行。無ければ -1
     * @param oldLength 旧セクションの行数
     * @return 置換後の lore と新セクションの位置
     */
    public static Splice splice(List<Component> currentLore, int oldStart, int oldLength,
                                List<Component> section) {
        List<Component> lore = new ArrayList<>(currentLore);
        int insertAt = lore.size();
        if (oldStart >= 0 && oldLength >= 0 && oldStart + oldLength <= lore.size()) {
            lore.subList(oldStart, oldStart + oldLength).clear();
            insertAt = oldStart;
        }
        lore.addAll(insertAt, section);
        return new Splice(lore, section.isEmpty() ? -1 : insertAt, section.size());
    }

    /** {@link #splice} の結果。lore が空ならアイテムから lore 自体を消してよい。 */
    public record Splice(List<Component> lore, int start, int length) {
    }

    private static Component line(ItemModifier modifier, Component name, boolean percentAttribute) {
        double value = modifier.value();
        return switch (modifier.operation()) {
            case ADD -> percentAttribute
                    ? signed(value * 100, 1, name)
                    : signed(value, 0, name);
            // MULTIPLY は係数(1.2 = +20%)なので、1 からの差分を百分率で出す
            case MULTIPLY -> signed((value - 1) * 100, 1, name);
            case SET -> Component.translatable(
                            "attribute.modifier.equals." + (percentAttribute ? 1 : 0),
                            Component.text(FORMAT.format(percentAttribute ? value * 100 : value)),
                            name)
                    .color(NamedTextColor.DARK_GREEN)
                    .decoration(TextDecoration.ITALIC, false);
        };
    }

    /** 正なら青の plus、負なら赤の take(バニラの配色)。variant: 0=実数, 1=百分率。 */
    private static Component signed(double value, int variant, Component name) {
        boolean positive = value >= 0;
        return Component.translatable(
                        "attribute.modifier." + (positive ? "plus" : "take") + "." + variant,
                        Component.text(FORMAT.format(Math.abs(value))),
                        name)
                .color(positive ? NamedTextColor.BLUE : NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false);
    }
}
