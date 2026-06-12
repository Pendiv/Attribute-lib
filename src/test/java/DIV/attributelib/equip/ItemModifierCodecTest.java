package DIV.attributelib.equip;

import DIV.attributelib.api.ItemModifier;
import DIV.attributelib.api.Operation;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ItemModifierCodecTest {

    private static final NamespacedKey KEY = NamespacedKey.fromString("myplugin:dodge_chance");

    @Test
    @DisplayName("全スロットグループでラウンドトリップが一致する")
    void roundTrip() {
        for (EquipmentSlotGroup slot : new EquipmentSlotGroup[]{
                EquipmentSlotGroup.ANY, EquipmentSlotGroup.MAINHAND, EquipmentSlotGroup.OFFHAND,
                EquipmentSlotGroup.HAND, EquipmentSlotGroup.HEAD, EquipmentSlotGroup.CHEST,
                EquipmentSlotGroup.LEGS, EquipmentSlotGroup.FEET, EquipmentSlotGroup.ARMOR,
                EquipmentSlotGroup.BODY}) {
            ItemModifier original = new ItemModifier(KEY, Operation.MULTIPLY, 1.25, slot);
            assertEquals(original, ItemModifierCodec.parse(ItemModifierCodec.format(original)),
                    "slot=" + slot);
        }
    }

    @Test
    @DisplayName("壊れた行は null になる(例外を投げない)")
    void malformed() {
        assertNull(ItemModifierCodec.parse(""));
        assertNull(ItemModifierCodec.parse("a|b|c"));
        assertNull(ItemModifierCodec.parse("BAD KEY!|ADD|1.0|mainhand"));
        assertNull(ItemModifierCodec.parse("myplugin:x|NOTANOP|1.0|mainhand"));
        assertNull(ItemModifierCodec.parse("myplugin:x|ADD|xx|mainhand"));
        assertNull(ItemModifierCodec.parse("myplugin:x|ADD|1.0|notaslot"));
        assertNull(ItemModifierCodec.parse("myplugin:x|ADD|Infinity|mainhand"));
    }
}
