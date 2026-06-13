package DIV.attributelib.api;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.jspecify.annotations.Nullable;

/**
 * アイテムに書かれたカスタム属性モディファイア1件。
 * 装備されている間だけ、エンティティへ transient モディファイアとして反映される。
 *
 * @param attribute 対象属性のキー
 * @param operation 演算種別
 * @param value     値
 * @param slot      有効なスロット(MAINHAND / ARMOR / ANY など)
 * @param condition 適用条件のキー(null = 無条件)。装備中かつ条件成立中のみ効く
 */
public record ItemModifier(
        NamespacedKey attribute,
        Operation operation,
        double value,
        EquipmentSlotGroup slot,
        @Nullable NamespacedKey condition
) {
    /** 無条件モディファイア。 */
    public ItemModifier(NamespacedKey attribute, Operation operation, double value, EquipmentSlotGroup slot) {
        this(attribute, operation, value, slot, null);
    }

    public ItemModifier {
        if (attribute == null || operation == null || slot == null) {
            throw new IllegalArgumentException("attribute / operation / slot は必須です");
        }
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("value が有限値ではありません: " + value);
        }
    }
}
