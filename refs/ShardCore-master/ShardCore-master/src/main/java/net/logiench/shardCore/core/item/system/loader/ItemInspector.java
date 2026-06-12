package net.logiench.shardCore.core.item.system.loader;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.logiench.logienchlibv2.api.minecraft.data.DataContainer;
import net.logiench.shardCore.ShardCore;
import net.logiench.shardCore.core.item.base.def.ShardItem;
import net.logiench.shardCore.core.item.base.module.ItemModule;
import net.logiench.shardCore.core.item.base.module.tools.LoreSection;
import net.logiench.shardCore.core.item.system.data.ItemDataHandler;
import net.logiench.shardCore.core.item.system.module.context.InspectContext;
import net.logiench.shardCore.core.item.system.module.context.ReadContext;
import net.logiench.shardCore.core.item.system.module.params.GenerationParameters;
import net.logiench.shardCore.register.ItemRegistry;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * アイテムの情報を取得するクラスです
 */
@Singleton
public class ItemInspector {

	private final ItemRegistry itemRegistry;
	private final ItemDataHandler handler;

	@Inject
	private ItemInspector(ItemRegistry itemRegistry, ItemDataHandler handler) {
		this.itemRegistry = itemRegistry;
		this.handler = handler;
	}

	@Contract("null -> null")
	public ShardItem getItemData(@Nullable ItemLoader loader) {
		if (loader == null) {
			return null;
		}
		return itemRegistry.get(loader.getId());
	}

	@Contract("null -> null")
	public GenerationParameters getGenParams(@Nullable ItemLoader loader) {
		String strParams = getStringGenParams(loader);
		if (strParams == null) {
			return null;
		}
		return handler.deserializeParams(strParams);
	}

	@Contract("null -> null")
	public String getStringGenParams(@Nullable ItemLoader loader) {
		if (loader == null) {
			return null;
		}
		return loader.getLoadedItem().getItemData(ItemDataHandler.ITEM_PARAMS);
	}

	@Contract("null -> null; !null -> !null")
	@Nullable
	@Unmodifiable
	public Map<LoreSection, Integer> getSectionIndexes(@Nullable ItemLoader loader) {
		if (loader == null) {
			return null;
		}
		DataContainer indexContainer = loader.getLoadedItem().getDataContainer().getContainer(ItemDataHandler.ITEM_LORE_INDEXES);
		if (indexContainer == null) {
			return Map.of();
		}
		Map<LoreSection, Integer> loreIndexes = new EnumMap<>(LoreSection.class);
		for (NamespacedKey key : indexContainer.getKeys()) {
			String sectionName = key.getKey().toUpperCase();
			try {
				loreIndexes.put(LoreSection.valueOf(sectionName), indexContainer.get(key, PersistentDataType.INTEGER));
			} catch (IllegalArgumentException e) {
				ShardCore.getPLogger().warning("LoreSectionIndexesに存在しないLoreSectionの名前が含まれています。 name: " + sectionName);
				return Map.of();
			}
		}
		return Collections.unmodifiableMap(loreIndexes);
	}

	@NotNull
	public ReadContext inspect(@Nullable ItemLoader loader) {
		ShardItem data = getItemData(loader);
		if (data == null) {
			return InspectContext.EMPTY;
		}
		return inspect(data, loader);
	}

	// dataとloaderのアイテムタイプが一致しているか確認していないので外部に公開しない
	private <I extends ShardItem> ReadContext inspect(@NotNull I data, @NotNull ItemLoader loader) {
		ReadContext context = new InspectContext();
		handler.runModules(data, ItemModule::getReader,
			r -> r.read(loader, data, context));
		return context;
	}
}
