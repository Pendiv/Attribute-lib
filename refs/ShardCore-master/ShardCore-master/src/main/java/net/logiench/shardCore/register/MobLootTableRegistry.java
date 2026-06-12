package net.logiench.shardCore.register;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.logiench.shardCore.core.loot.base.LootItem;
import net.logiench.shardCore.core.loot.base.LootTable;
import net.logiench.shardCore.core.loot.system.LootItemGenerateProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class MobLootTableRegistry {
	private final Map<String, LootItemGenerateProvider> lootTableMap = new HashMap<>();
	private final LootItemGenerateProvider.Factory lootItemGenerateProviderFactory;

	@Inject
	private MobLootTableRegistry(LootItemGenerateProvider.Factory lootItemGenerateProviderFactory) {
		this.lootItemGenerateProviderFactory = lootItemGenerateProviderFactory;
	}

	public void register(@NotNull String tableId, @NotNull LootTable<LootItem> lootTable) {
		lootTableMap.put(tableId, lootItemGenerateProviderFactory.create(lootTable));
	}

	@Nullable
	public LootItemGenerateProvider get(@Nullable String tableId) {
		if (tableId == null) {
			return null;
		}
		return lootTableMap.get(tableId);
	}
}
