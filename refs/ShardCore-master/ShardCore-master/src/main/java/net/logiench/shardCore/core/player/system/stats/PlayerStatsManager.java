package net.logiench.shardCore.core.player.system.stats;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.logiench.shardCore.core.item.system.loader.ItemLoader;
import net.logiench.shardCore.core.player.system.PlayerCharacter;
import net.logiench.shardCore.event.PlayerStatsUpdateEvent;
import net.logiench.shardLib.api.attribute.data.AttributeModifier;
import net.logiench.shardLib.api.attribute.data.ModifierOperation;
import net.logiench.shardLib.api.attribute.data.StackingRule;
import net.logiench.shardLib.api.player.PlayerAttributeAPI;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class PlayerStatsManager {
	private static final String SOURCE_MODIFIER_KEY = "sc:item";

	@Inject
	private PlayerStatsManager() {
	}

	public void applyEquipmentStats(@NotNull PlayerCharacter character, @NotNull Collection<ItemLoader> equipmentItems) {
		PlayerAttributeAPI attributeAPI = character.attributeAPI();
		// すべてのアイテムステータスの集計を取る
		Map<String, Double> statsMap = new HashMap<>();
		/*for (Map.Entry<PlayerEquipmentTable.Slot, PlayerEquipmentTable> entry : equipmentManager.getEquipments(character.getUniqueId()).entrySet()) {
			PlayerEquipmentTable table = entry.getValue();
			Optional<ItemData> itemDataOptional = itemAPI.getItemData(
				serializer.deserializeItem(table.itemId(), table.itemData()));
			if (itemDataOptional.isEmpty()) {
				continue;
			}
			statsMap.putAll(itemDataOptional.get().getBaseStats());
		}*/
		for (ItemLoader item : equipmentItems) {
			//			statsMap.putAll(item.getStats()); // ItemLoaderではなくItemInspectorが行うべき役割だと思われる
		}
		// 同じソースから与えられたModifierをすべて削除
		attributeAPI.removeModifiers(SOURCE_MODIFIER_KEY);
		// すべてのステータスを適応
		for (Map.Entry<String, Double> entry : statsMap.entrySet()) {
			attributeAPI.addModifier(new AttributeModifier(
				SOURCE_MODIFIER_KEY, entry.getKey(),
				ModifierOperation.ADD, StackingRule.STACKABLE, entry.getValue()
			));
		}
		attributeAPI.recalculateStats();
		Bukkit.getPluginManager().callEvent(new PlayerStatsUpdateEvent(character));
	}
}
