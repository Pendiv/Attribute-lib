package net.logiench.shardLib.core.item;

import com.google.inject.Singleton;
import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import org.bukkit.Material;

import java.util.Map;

@Singleton
public class ItemFactory {

	public SuperItemStack createItem(Material material) {
		// アイテムにデータを書き込む
		SuperItemStack item = SuperItemStack.init(material);

		item.addItemData(ItemDataKey.IS_SHARD_ITEM, true);

		ItemDataImpl itemData = new ItemDataBuilderImpl()
			.setBaseStats(Map.of())
			.build();

		// アイテムの箱だけを作成し、それ以外は完全に外部で行う
		setItemData(item, itemData);

		return item;
	}

	public void setItemData(SuperItemStack item, ItemDataImpl itemData) {
		item.addItemData(ItemDataKey.STATS, itemData.toGson());
	}
}
