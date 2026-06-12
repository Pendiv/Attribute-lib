package net.logiench.shardCore.core.item.base.module;

import net.logiench.shardCore.core.item.base.def.ShardItem;
import net.logiench.shardCore.core.item.system.module.context.BaseContext;
import net.logiench.shardCore.core.item.system.module.context.GenerationContext;

public interface ItemProcessor<I extends ShardItem> {
	/**
	 * アイテムのデータ抽選や、データの適応など、内部的な処理を行います。
	 * これはProcessorメソッドの中で必ず一番初めに実行されます。
	 *
	 * @param context 生成コンテキスト
	 */
	void process(GenerationContext<? extends I> context);

	/**
	 * アイテムが同一のものかを判別する数値を生成します。
	 * この値は再起動後も一致しなければなりません。
	 *
	 * @param context 生成するためのデータ
	 * @return checksumという名のhash
	 */
	int checksum(BaseContext context);
}
