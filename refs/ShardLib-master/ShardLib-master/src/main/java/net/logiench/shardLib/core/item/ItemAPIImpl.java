package net.logiench.shardLib.core.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import net.logiench.shardLib.api.data.CustomDataContainerAPI;
import net.logiench.shardLib.api.item.ItemAPI;
import net.logiench.shardLib.api.item.ItemData;
import net.logiench.shardLib.core.data.CustomDataContainerAPIImpl;
import net.logiench.shardLib.core.data.CustomDataKey;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@Singleton
public class ItemAPIImpl implements ItemAPI {
	private final ItemFactory itemFactory;

	@Inject
	public ItemAPIImpl(ItemFactory itemFactory) {
		this.itemFactory = itemFactory;
	}

	public @NotNull SuperItemStack generate(@NotNull Material material) {
		return itemFactory.createItem(material);
	}

	@Override
	public boolean isShardItem(@Nullable SuperItemStack item) {
		if (item == null) {
			return false;
		}
		return item.hasItemData(ItemDataKey.IS_SHARD_ITEM);
	}

	@Override
	public @NotNull Optional<ItemData> getItemData(@Nullable SuperItemStack item) {
		return ItemDataImpl.fromGson(item);
	}

	@Override
	public boolean setItemData(@Nullable SuperItemStack item, @Nullable ItemData data) {
		if (item == null || !(data instanceof ItemDataImpl itemDataImpl)) {
			return false;
		}
		itemFactory.setItemData(item, itemDataImpl);
		return true;
	}

	@Override
	public @NotNull CustomDataContainerAPI getCustomData(@NotNull SuperItemStack item) {
		return CustomDataContainerAPIImpl.fromGson(
			item.getItemData(CustomDataKey.CUSTOM_DATA2)
		).orElseGet(CustomDataContainerAPIImpl::new);
	}

	@Override
	public boolean setCustomData(@NotNull SuperItemStack item, @NotNull CustomDataContainerAPI data) {
		if (!(data instanceof CustomDataContainerAPIImpl customDataImpl)) {
			return false;
		}
		item.addItemData(CustomDataKey.CUSTOM_DATA2, customDataImpl.toGson());
		return true;
	}

}
