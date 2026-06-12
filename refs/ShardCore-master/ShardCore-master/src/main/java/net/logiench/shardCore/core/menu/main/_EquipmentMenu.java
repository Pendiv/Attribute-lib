package net.logiench.shardCore.core.menu.main;

import com.google.inject.Inject;
import net.kyori.adventure.text.Component;
import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import net.logiench.shardCore.core.item.system.data.ItemSerializer;
import net.logiench.shardCore.core.item.system.loader.ItemInspector;
import net.logiench.shardCore.core.menu.util.SimpleMenu;
import net.logiench.shardCore.core.player.system._PlayerCharacterManager;
import net.logiench.shardCore.core.player.system.item.PlayerEquipmentGemManager;
import net.logiench.shardCore.core.player.system.stats.PlayerStatsManager;
import org.bukkit.entity.Player;

import java.util.UUID;

public class _EquipmentMenu extends SimpleMenu {
	private static final Component TITLE = Component.text("Equipment Menu");
	/*private static final Map<PlayerEquipmentEntity.Slot, SlotAndItem> SLOT_INDEX = Map.of(
		PlayerEquipmentEntity.Slot.HEAD, new SlotAndItem(10, SuperItemStack.init(Material.IRON_HELMET)),
		PlayerEquipmentEntity.Slot.CHEST, new SlotAndItem(11, SuperItemStack.init(Material.IRON_CHESTPLATE)),
		PlayerEquipmentEntity.Slot.LEGS, new SlotAndItem(12, SuperItemStack.init(Material.IRON_LEGGINGS)),
		PlayerEquipmentEntity.Slot.FEET, new SlotAndItem(13, SuperItemStack.init(Material.IRON_BOOTS)),
		PlayerEquipmentEntity.Slot.OFF_HAND, new SlotAndItem(15, SuperItemStack.init(Material.SHIELD)),
		PlayerEquipmentEntity.Slot.MAIN_HAND, new SlotAndItem(16, SuperItemStack.init(Material.IRON_SWORD))
	);

	@Inject
	private PlayerEquipmentService equipmentManager;*/
	@Inject
	private ItemSerializer serializer;
	@Inject
	private ItemInspector inspector;
	@Inject
	private PlayerStatsManager statsManager;
	@Inject
	private _PlayerCharacterManager characterManager;
	@Inject
	private PlayerEquipmentGemManager equipmentGemManager;

	public _EquipmentMenu(Player player) {
		super(player);
	}

	@Override
	protected void initMenu() {
		UUID playerId = player.getUniqueId();

		/*for (Map.Entry<PlayerEquipmentEntity.Slot, SlotAndItem> entry : SLOT_INDEX.entrySet()) {
			int slotIndex = entry.getValue().slot();
			menu.setItem(slotIndex - 9, entry.getValue().item());
			PlayerEquipmentEntity value = equipmentManager.getEquipment(playerId, entry.getKey());
			if (value == null) {
				continue;
			}
			ItemGenerationResult r = serializer.deserialize(value.itemId(), value.itemData());

			if (r == null) {
				continue;
			}
			menu.setItem(slotIndex, r.safeItem());
		}
		ClickInventoryMenuCreator creator = new ClickInventoryMenuCreator();
		for (Map.Entry<PlayerEquipmentEntity.Slot, SlotAndItem> entry : SLOT_INDEX.entrySet()) {
			creator.addAllowedSlot(entry.getValue().slot(), e -> {
				ItemLoader loader = ItemLoader.of(e.getClickedItem());
				if (loader == null) {
					return false;
				}
				ShardItem data = inspector.getItemData(loader);
				return entry.getKey().isAllowedItem(data.getItemType());
			});
		}
		menu.addAllListener(creator)
			.addCloseListener(ev -> {
				Inventory inv = ev.getInventory();
				List<ItemLoader> equipmentItems = new ArrayList<>();
				for (Map.Entry<PlayerEquipmentEntity.Slot, SlotAndItem> entry : SLOT_INDEX.entrySet()) {
					PlayerEquipmentEntity.Slot slot = entry.getKey();
					ItemLoader loader = ItemLoader.of(inv.getItem(entry.getValue().slot()));

					SerializedItemData serialized = serializer.serialize(loader);
					if (serialized == null) {
						equipmentManager.removeEquipment(playerId, slot);
						System.out.println("[DEBUG] Slot " + slot + " は変換できません");
						continue;
					}

					equipmentManager.setEquipment(playerId, slot, serialized.itemId(), serialized.genParamsJson());
					// 保存できるものだけステータスに適応するためのリストに追加
					equipmentItems.add(loader);
					System.out.println("[DEBUG] Slot " + slot + " を保存しました");
				}
				equipmentGemManager.applyItemLoaders(playerId, equipmentItems);

				characterManager.onCharacter(player, c -> {
					statsManager.applyEquipmentStats(c, equipmentItems);
					Bukkit.getPluginManager().callEvent(new PlayerStatsUpdateEvent(c));
				});
			});*/
	}

	@Override
	public Component getTitle() {
		return TITLE;
	}

	@Override
	public int getSize() {
		return 54;
	}

	private record SlotAndItem(int slot, SuperItemStack item) {}
}
