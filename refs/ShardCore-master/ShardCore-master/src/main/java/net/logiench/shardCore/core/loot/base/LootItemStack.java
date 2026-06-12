package net.logiench.shardCore.core.loot.base;

import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import net.logiench.shardCore.core.item.system.generator.ItemGenerator;
import org.jetbrains.annotations.NotNull;

/**
 * テストに使用するためのクラスです。
 * アイテムをそのまま格納できます。
 */
public record LootItemStack(@NotNull SuperItemStack item) implements LootItem {
	@Override
	public @NotNull SuperItemStack generate(ItemGenerator generator) {
		return item.clone();
	}
}
