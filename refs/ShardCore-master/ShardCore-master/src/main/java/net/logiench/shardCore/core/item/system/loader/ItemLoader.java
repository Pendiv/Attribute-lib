package net.logiench.shardCore.core.item.system.loader;

import lombok.Getter;
import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import net.logiench.shardCore.core.item.system.data.ItemDataHandler;
import net.logiench.shardCore.core.item.system.generator.ErrorItemFactory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * アイテムがShardItemの形式であることを保証しますが、存在しないIDの可能性があります。
 */
public class ItemLoader {
	@Getter
	@NotNull
	private final String id;
	@Getter
	@NotNull
	private final SuperItemStack loadedItem;
	//	private ItemData itemData;

	@Nullable
	@Contract("null -> null")
	public static ItemLoader of(ItemStack item) {
		if (item == null) {
			return null;
		}
		return of(SuperItemStack.safeInit(item));
	}

	@Nullable
	@Contract("null -> null")
	public static ItemLoader of(SuperItemStack item) {
		if (item == null || ErrorItemFactory.isErrorItem(item)) {
			return null;
		}
		// IDが存在するということはShardItemで基本問題なし
		String id = item.getItemData(ItemDataHandler.ITEM_ID);
		if (id != null) {
			return new ItemLoader(id, item);
		}
		return null;
	}

	private ItemLoader(@NotNull String id, @NotNull SuperItemStack loadedItem) {
		this.id = id;
		this.loadedItem = loadedItem;
	}
/*
	public Map<String, Double> getStats() {
		if (itemData == null) {
			Optional<ItemData> optional = ShardLibProvider.get().getItemAPI().getItemData(loadedItem);
			if (optional.isEmpty()) {
				return Map.of();
			}
			this.itemData = optional.get();
		}
		return itemData.getBaseStats();
	}

	public static Map<String, Double> getAllStats(List<SuperItemStack> items) {
		Map<String, Double> map = new HashMap<>();
		for (SuperItemStack item : items) {
			ItemLoader loader = of(item);
			if (loader == null) {
				continue;
			}
			loader.getStats().forEach((k, v) ->
				map.merge(k, v, Double::sum));
		}
		return map;
	}*/
}
