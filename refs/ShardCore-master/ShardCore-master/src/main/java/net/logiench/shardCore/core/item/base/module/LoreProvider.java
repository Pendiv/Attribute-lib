package net.logiench.shardCore.core.item.base.module;

import net.logiench.shardCore.core.item.base.def.ShardItem;
import net.logiench.shardCore.core.item.base.module.tools.StructuredLore;
import net.logiench.shardCore.core.item.system.module.context.GenerationContext;

public interface LoreProvider<I extends ShardItem> {

	/**
	 * 鑑定後の確定したLoreを作成します
	 *
	 * @param structuredLore 構築するLoreの構造
	 * @param context        生成コンテキスト
	 */
	void updateLore(StructuredLore structuredLore, GenerationContext<? extends I> context);
}
