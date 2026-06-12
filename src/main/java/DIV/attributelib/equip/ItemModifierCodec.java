package DIV.attributelib.equip;

import DIV.attributelib.api.ItemModifier;
import DIV.attributelib.api.Operation;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.EquipmentSlotGroup;

/**
 * アイテム PDC へ保存する文字列形式との相互変換。
 * 形式: {@code ns:key|op|value|slotGroup}
 *
 * <p>parse は壊れた行に対して null を返す(呼び出し側で警告ログを出すこと)。</p>
 */
public final class ItemModifierCodec {

    private ItemModifierCodec() {
    }

    public static String format(ItemModifier modifier) {
        return modifier.attribute().toString() + "|" + modifier.operation().name()
                + "|" + modifier.value() + "|" + modifier.slot();
    }

    /** @return 復元した ItemModifier。壊れた行なら null。 */
    public static ItemModifier parse(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length != 4) {
            return null;
        }
        NamespacedKey key = NamespacedKey.fromString(parts[0]);
        EquipmentSlotGroup slot = EquipmentSlotGroup.getByName(parts[3]);
        if (key == null || slot == null) {
            return null;
        }
        try {
            return new ItemModifier(key, Operation.valueOf(parts[1]), Double.parseDouble(parts[2]), slot);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
