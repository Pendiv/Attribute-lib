package net.logiench.shardCore.core.item.system.data;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import net.logiench.shardCore.core.item.base.def.ShardItem;
import net.logiench.shardCore.core.item.system.generator.ErrorItemFactory;
import net.logiench.shardCore.core.item.system.generator.ItemGenerationResult;
import net.logiench.shardCore.core.item.system.generator.ItemGenerator;
import net.logiench.shardCore.core.item.system.loader.ItemInspector;
import net.logiench.shardCore.core.item.system.loader.ItemLoader;
import net.logiench.shardCore.core.item.system.module.params.GenerationParameters;
import net.logiench.shardCore.core.player.system.PlayerCharacter;
import net.logiench.shardCore.register.ItemRegistry;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Singleton
public class ItemSerializer {

	private final ItemRegistry itemRegistry;
	private final ItemDataHandler dataHandler;
	private final ItemGenerator itemGenerator;
	private final ItemInspector itemInspector;

	@Inject
	private ItemSerializer(ItemRegistry itemRegistry, ItemDataHandler dataHandler, ItemGenerator itemGenerator, ItemInspector itemInspector) {
		this.itemRegistry = itemRegistry;
		this.dataHandler = dataHandler;
		this.itemGenerator = itemGenerator;
		this.itemInspector = itemInspector;
	}

	@Nullable
	@Contract("null -> null")
	public SerializedItemData serialize(@Nullable ItemLoader loader) {
		if (loader == null || ErrorItemFactory.isErrorItem(loader.getLoadedItem())) {
			return null;
		}
		String strGenParams = itemInspector.getStringGenParams(loader);
		if (strGenParams == null) {
			return null;
		}
		return new SerializedItemData(loader.getId(), strGenParams);
	}

	@Nullable
	@Contract("null -> null")
	public ItemGenerationResult deserialize(@Nullable SerializedItemData serializedData) {
		if (serializedData == null) {
			return null;
		}
		return deserialize(serializedData.itemId(), serializedData.genParamsJson());
	}

	@Nullable
	@Contract("null, _ -> null")
	public ItemGenerationResult deserialize(@Nullable String itemId, @Nullable String strGenParams) {
		if (itemId == null) {
			return null;
		}
		return deserialize(itemRegistry.get(itemId), strGenParams);
	}

	@Nullable
	@Contract("null, _ -> null")
	public ItemGenerationResult deserialize(@Nullable ShardItem itemData, @Nullable String strGenParams) {
		if (itemData == null) {
			return null;
		}
		GenerationParameters genParams = dataHandler.deserializeParams(strGenParams);
		return itemGenerator.generateNew(itemData, genParams);
	}

	@Nullable
	@Contract("null -> null")
	public SuperItemStack deserializeItem(@Nullable SerializedItemData serializedData) {
		if (serializedData == null) {
			return null;
		}
		return deserializeItem(serializedData.itemId(), serializedData.genParamsJson());
	}

	@Nullable
	@Contract("null, _ -> null")
	public SuperItemStack deserializeItem(@Nullable String itemId, @Nullable String strGenParams) {
		ItemGenerationResult result = deserialize(itemId, strGenParams);
		if (result == null) {
			return null;
		}
		return result.item();
	}

	@Nullable
	@Contract("_, null, _ -> null")
	public SuperItemStack deserializeAndUpdate(@NotNull PlayerCharacter character, @Nullable String itemId, @Nullable String strGenParams) {
		ItemGenerationResult result = deserialize(itemId, strGenParams);
		if (result == null) {
			return null;
		}
		SuperItemStack item = result.safeItem();
		itemGenerator.updateDynamicLore(character, item);
		return item;
	}
}
