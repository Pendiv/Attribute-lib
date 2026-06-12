package net.logiench.shardCore.core.item.base.module;

import net.logiench.shardCore.core.item.base.def.ShardItem;
import net.logiench.shardCore.core.item.system.module.context.UpdateContext;

public interface ItemStatsUpdater<I extends ShardItem> {

	/**
	 * アイテムの現在の状態を元に、更新処理を行います。
	 * <p><b>更新した内容は必ず<code>GenerationParameters</code>にも適応するようにしてください</b></p>
	 * このデータを元にアイテムの復元を行うので、更新した情報がなければ前の状態で復元されます。
	 */
	void update(UpdateContext<? extends I> context);
}
