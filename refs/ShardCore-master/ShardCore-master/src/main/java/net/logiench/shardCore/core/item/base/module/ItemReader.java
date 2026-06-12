package net.logiench.shardCore.core.item.base.module;

import net.logiench.shardCore.core.item.base.def.ShardItem;
import net.logiench.shardCore.core.item.system.loader.ItemLoader;
import net.logiench.shardCore.core.item.system.module.context.ReadContext;
import org.jetbrains.annotations.NotNull;

public interface ItemReader<I extends ShardItem> {
	/**
	 * アイテムの情報を取得し <code>context</code>に格納します
	 *
	 * @param loader  アイテムの情報まとめ
	 * @param data    アイテムのデータ
	 * @param context 格納する対象
	 */
	void read(@NotNull ItemLoader loader, @NotNull I data, @NotNull ReadContext context);
}
