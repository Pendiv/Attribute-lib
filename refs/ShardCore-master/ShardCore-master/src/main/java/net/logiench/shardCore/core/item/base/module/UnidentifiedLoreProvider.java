package net.logiench.shardCore.core.item.base.module;

import net.logiench.logienchlibv2.api.minecraft.text.LoreList;
import net.logiench.shardCore.core.item.base.def.ShardItem;
import net.logiench.shardCore.core.item.system.module.context.UnidentifiedContext;

/**
 * 未鑑定時のLore作成処理を実装します
 */
public interface UnidentifiedLoreProvider<I extends ShardItem> {

	/**
	 * 未鑑定時のLoreを作成します
	 *
	 * @param lore    構築するLoreリスト
	 * @param context 生成コンテキスト
	 */
	void updateUnidentifiedLore(LoreList lore, UnidentifiedContext<? extends I> context);
}
