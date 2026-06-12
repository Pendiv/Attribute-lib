package DIV.attributelib.api;

import DIV.attributelib.equip.ItemModifierCodec;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * アイテムへの属性付与 API。
 *
 * <p><b>カスタム属性</b>(add / get / clear): アイテムの PDC に保存され、装備中だけ
 * エンティティへ transient モディファイアとして自動反映される(装備同期は attributelib が行う)。
 * エンティティ側の PDC には書かれないため、外したのに残る「ゴースト強化」は構造的に起きない。</p>
 *
 * <pre>{@code
 * ItemStack sword = ItemStack.of(Material.DIAMOND_SWORD);
 * ItemAttributes.add(sword, MY_ATTR, Operation.ADD, 10, EquipmentSlotGroup.MAINHAND);
 * ItemAttributes.add(sword, StandardAttributes.CRIT_CHANCE, Operation.ADD, 0.2, EquipmentSlotGroup.MAINHAND);
 * }</pre>
 *
 * <p><b>バニラ属性</b>(setVanilla / removeVanilla): attribute_modifiers コンポーネントへ
 * 書き込む(同期はバニラが行う)。コンポーネントを直接 set すると素材本来の基礎ステータス
 * (剣の攻撃力など)が消える「base-wipe」が起きるため、ここでは既存(デフォルト含む)を
 * 引き継いだ上で追加する。</p>
 */
public final class ItemAttributes {

    private static final NamespacedKey MODIFIERS_KEY = new NamespacedKey("attributelib", "item_modifiers");
    private static final Logger LOGGER = Logger.getLogger("attributelib");

    private ItemAttributes() {
    }

    // ---- カスタム属性 ----

    /** カスタム属性モディファイアをアイテムに追加する。 */
    public static void add(ItemStack item, AttributeType type, Operation operation,
                           double value, EquipmentSlotGroup slot) {
        List<ItemModifier> modifiers = new ArrayList<>(get(item));
        modifiers.add(new ItemModifier(type.key(), operation, value, slot));
        write(item, modifiers);
    }

    /** アイテムに書かれたカスタム属性モディファイアの一覧(なければ空)。 */
    public static List<ItemModifier> get(ItemStack item) {
        if (item == null || item.isEmpty() || !item.hasItemMeta()) {
            return List.of();
        }
        List<String> lines = item.getItemMeta().getPersistentDataContainer()
                .get(MODIFIERS_KEY, PersistentDataType.LIST.strings());
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }
        List<ItemModifier> result = new ArrayList<>(lines.size());
        for (String line : lines) {
            ItemModifier modifier = ItemModifierCodec.parse(line);
            if (modifier == null) {
                LOGGER.warning("壊れたアイテムモディファイア行を無視します (" + item.getType() + "): " + line);
                continue;
            }
            result.add(modifier);
        }
        return result;
    }

    /** 指定属性のカスタムモディファイアをアイテムから取り除く。 */
    public static void remove(ItemStack item, AttributeType type) {
        List<ItemModifier> modifiers = new ArrayList<>(get(item));
        if (modifiers.removeIf(m -> m.attribute().equals(type.key()))) {
            write(item, modifiers);
        }
    }

    /** カスタム属性モディファイアを全て取り除く。 */
    public static void clear(ItemStack item) {
        write(item, List.of());
    }

    private static void write(ItemStack item, List<ItemModifier> modifiers) {
        item.editMeta(meta -> {
            if (modifiers.isEmpty()) {
                meta.getPersistentDataContainer().remove(MODIFIERS_KEY);
                return;
            }
            List<String> lines = new ArrayList<>(modifiers.size());
            for (ItemModifier modifier : modifiers) {
                lines.add(ItemModifierCodec.format(modifier));
            }
            meta.getPersistentDataContainer().set(MODIFIERS_KEY, PersistentDataType.LIST.strings(), lines);
        });
    }

    // ---- バニラ属性(attribute_modifiers コンポーネント) ----

    /**
     * バニラ属性モディファイアをアイテムへ冪等に設定する(同キーは置き換え)。
     * 素材本来の基礎ステータス(デフォルトモディファイア)は保持される。
     */
    public static void setVanilla(ItemStack item, Attribute attribute, NamespacedKey key,
                                  AttributeModifier.Operation operation, double amount, EquipmentSlotGroup slot) {
        ItemAttributeModifiers.Builder builder = copyWithout(item, key);
        builder.addModifier(attribute, new AttributeModifier(key, amount, operation, slot), slot);
        item.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, builder.build());
    }

    /** 指定キーのバニラ属性モディファイアをアイテムから取り除く。 */
    public static void removeVanilla(ItemStack item, NamespacedKey key) {
        item.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, copyWithout(item, key).build());
    }

    /** 現在の(デフォルト含む)モディファイアを、指定キーを除いてビルダーへ写す。 */
    private static ItemAttributeModifiers.Builder copyWithout(ItemStack item, NamespacedKey excludeKey) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.itemAttributes();
        ItemAttributeModifiers current = item.getData(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (current != null) {
            for (ItemAttributeModifiers.Entry entry : current.modifiers()) {
                if (!entry.modifier().getKey().equals(excludeKey)) {
                    builder.addModifier(entry.attribute(), entry.modifier(), entry.getGroup(), entry.display());
                }
            }
        }
        return builder;
    }
}
