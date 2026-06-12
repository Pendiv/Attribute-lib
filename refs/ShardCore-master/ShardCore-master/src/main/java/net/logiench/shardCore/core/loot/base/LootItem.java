package net.logiench.shardCore.core.loot.base;

import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import net.logiench.shardCore.core.item.system.generator.ItemGenerator;
import org.jetbrains.annotations.Nullable;

public interface LootItem {
	/**
	 * アイテムを新しく作成します。
	 *
	 * @param generator アイテムを生成するためのジェネレータ
	 */
	@Nullable
	SuperItemStack generate(ItemGenerator generator);
}
