package DIV.attributelib.api;

import DIV.attributelib.equip.ItemModifierCodec;
import DIV.attributelib.equip.LoreComposer;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers;
import io.papermc.paper.datacomponent.item.attribute.AttributeModifierDisplay;
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
    private static final NamespacedKey LORE_SECTION_KEY = new NamespacedKey("attributelib", "lore_section");
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

    /** 条件付きのカスタム属性モディファイア。装備中かつ条件成立中のみ効く(lore に条件が表示される)。 */
    public static void add(ItemStack item, AttributeType type, Operation operation,
                           double value, EquipmentSlotGroup slot, Condition condition) {
        List<ItemModifier> modifiers = new ArrayList<>(get(item));
        modifiers.add(new ItemModifier(type.key(), operation, value, slot, condition.key()));
        write(item, modifiers);
    }

    /**
     * アイテムにカスタム属性モディファイアが1つでもあるか。
     * meta のクローンを作らない読み取り専用ビューで判定するため、
     * 装備変更・チャンクロードごとの事前判定(ホットパス)に使える。
     */
    public static boolean hasModifiers(ItemStack item) {
        return item != null && !item.isEmpty()
                && item.getPersistentDataContainer().has(MODIFIERS_KEY);
    }

    /** アイテムに書かれたカスタム属性モディファイアの一覧(なければ空)。 */
    public static List<ItemModifier> get(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return List.of();
        }
        // getItemMeta()(meta の複製)を避け、読み取り専用ビューから直接読む
        List<String> lines = item.getPersistentDataContainer()
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
            // 1. PDC へモディファイア本体を保存
            if (modifiers.isEmpty()) {
                meta.getPersistentDataContainer().remove(MODIFIERS_KEY);
            } else {
                List<String> lines = new ArrayList<>(modifiers.size());
                for (ItemModifier modifier : modifiers) {
                    lines.add(ItemModifierCodec.format(modifier));
                }
                meta.getPersistentDataContainer().set(MODIFIERS_KEY, PersistentDataType.LIST.strings(), lines);
            }

            // 2. lore セクションを更新(層4: 書き込みはこの1箇所に集約)
            int[] section = meta.getPersistentDataContainer()
                    .get(LORE_SECTION_KEY, PersistentDataType.INTEGER_ARRAY);
            int oldStart = section != null && section.length == 2 ? section[0] : -1;
            int oldLength = section != null && section.length == 2 ? section[1] : 0;
            List<net.kyori.adventure.text.Component> currentLore =
                    meta.hasLore() ? meta.lore() : List.of();
            LoreComposer.Splice result = LoreComposer.splice(currentLore, oldStart, oldLength,
                    LoreComposer.buildSection(modifiers, ItemAttributes::resolveType,
                            ItemAttributes::resolveConditionName));
            meta.lore(result.lore().isEmpty() ? null : result.lore());
            if (result.start() < 0) {
                meta.getPersistentDataContainer().remove(LORE_SECTION_KEY);
            } else {
                meta.getPersistentDataContainer().set(LORE_SECTION_KEY,
                        PersistentDataType.INTEGER_ARRAY, new int[]{result.start(), result.length()});
            }
        });
    }

    /** 表示用の属性解決。attributelib 未ロード(テスト等)や未登録属性は null。 */
    private static AttributeType resolveType(NamespacedKey key) {
        DIV.attributelib.Attributelib plugin = DIV.attributelib.Attributelib.instance();
        return plugin != null ? plugin.engine().byKey(key) : null;
    }

    /** 表示用の条件名解決。未登録条件はキー名をそのまま表示する。 */
    private static net.kyori.adventure.text.Component resolveConditionName(NamespacedKey key) {
        DIV.attributelib.Attributelib plugin = DIV.attributelib.Attributelib.instance();
        Condition condition = plugin != null ? plugin.engine().conditions().byKey(key) : null;
        return condition != null ? condition.displayName()
                : net.kyori.adventure.text.Component.text(key.toString());
    }

    // ---- バニラ属性(attribute_modifiers コンポーネント) ----

    /**
     * バニラ属性モディファイアをアイテムへ冪等に設定する(同キーは置き換え)。
     * 素材本来の基礎ステータス(デフォルトモディファイア)は保持され、
     * 表示はバニラ標準のまま(色・書式ともバニラルール)。
     */
    public static void setVanilla(ItemStack item, Attribute attribute, NamespacedKey key,
                                  AttributeModifier.Operation operation, double amount, EquipmentSlotGroup slot) {
        setVanilla(item, attribute, key, operation, amount, slot, AttributeModifierDisplay.reset());
    }

    /**
     * 表示指定付きのバニラ属性設定。{@link AttributeModifierDisplay#hidden()} で非表示、
     * {@link AttributeModifierDisplay#override(net.kyori.adventure.text.ComponentLike)} で
     * 行そのものを自前デザインに置換できる(クライアント描画なので lore を汚さない)。
     */
    public static void setVanilla(ItemStack item, Attribute attribute, NamespacedKey key,
                                  AttributeModifier.Operation operation, double amount,
                                  EquipmentSlotGroup slot, AttributeModifierDisplay display) {
        ItemAttributeModifiers.Builder builder = copyWithout(item, key);
        builder.addModifier(attribute, new AttributeModifier(key, amount, operation, slot), slot, display);
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
