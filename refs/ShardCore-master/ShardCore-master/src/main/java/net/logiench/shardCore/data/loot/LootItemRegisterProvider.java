package net.logiench.shardCore.data.loot;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import net.logiench.shardCore.core.item.system.module.params.GenerationParameters;
import net.logiench.shardCore.core.loot.base.LootItem;
import net.logiench.shardCore.core.loot.base.LootItemData;
import net.logiench.shardCore.core.loot.base.LootItemStack;
import net.logiench.shardCore.core.loot.base.LootTable;
import net.logiench.shardCore.data.item.def.equipment.armor.chestplate.ObsidianChestplate;
import net.logiench.shardCore.register.ItemRegistry;
import net.logiench.shardCore.register.MobLootTableRegistry;
import org.bukkit.Material;

import java.util.function.Consumer;

@Singleton
public class LootItemRegisterProvider {
	private final MobLootTableRegistry mobLootTableRegistry;
	private final ItemRegistry itemRegistry;

	@Inject
	private LootItemRegisterProvider(MobLootTableRegistry mobLootTableRegistry, ItemRegistry itemRegistry) {
		this.mobLootTableRegistry = mobLootTableRegistry;
		this.itemRegistry = itemRegistry;
	}

	/**
	 * すべてのルートテーブルはここで登録します
	 */
	public void registerDefaults() {
		register("test_loot", builder -> {
			// レベル1〜5の範囲で排出
			builder.addPool(pool -> pool
				.rolls(1, 5)
				.add(new LootItemStack(SuperItemStack.init(Material.STONE)), 10d, 1, 3)
				.add(new LootItemStack(SuperItemStack.init(Material.APPLE)), 10d, 0, 1)
				.add(new LootItemStack(SuperItemStack.init(Material.GOLD_INGOT)), 20d, 0, 1)
				.add(new LootItemStack(SuperItemStack.init(Material.DIAMOND)), 1d, 0, 1)
			).addPool(pool -> pool
				.rolls(1)
				.add(new LootItemData(itemRegistry.get(ObsidianChestplate.class), 1, 100, GenerationParameters.of(), true), 1d)
			);
		});
	}

	public void register(String tableId, Consumer<LootTable.Builder<LootItem>> consumer) {
		LootTable.Builder<LootItem> builder = new LootTable.Builder<>() {};
		consumer.accept(builder);
		mobLootTableRegistry.register(tableId, builder.build());
	}
}
