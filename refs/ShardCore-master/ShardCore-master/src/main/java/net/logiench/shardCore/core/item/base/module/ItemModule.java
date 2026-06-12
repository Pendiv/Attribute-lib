package net.logiench.shardCore.core.item.base.module;

import net.logiench.shardCore.core.item.base.def.ShardItem;
import org.jetbrains.annotations.Nullable;

public interface ItemModule<I extends ShardItem> {
	Class<I> getTargetType();

	/**
	 * このモジュールでの{@link #getCalculator()}が使用する乱数のシードを変化させる要因となります。
	 * この値は他のモジュールと重複させることができます。
	 * 他のモジュールと同様のキーを指定すると、同じ乱数のシードが得られます。
	 */
	String getModuleKey();

	/**
	 * アイテムのデータを取得し、Contextに格納します。
	 * ここで作成されるデータは {@link #getCalculator()} で作成されるContextと同様にする必要があります。
	 */
	@Nullable
	default ItemReader<I> getReader() {
		return null;
	}

	@Nullable
	default ItemStatsUpdater<I> getUpdater() {
		return null;
	}

	// ステータス計算
	@Nullable
	default ItemStatsCalculator<I> getCalculator() {
		return null;
	}

	// 未鑑定Lore生成
	@Nullable
	default UnidentifiedLoreProvider<I> getUnidentifiedLore() {
		return null;
	}

	// 実処理
	@Nullable
	default ItemProcessor<I> getProcessor() {
		return null;
	}

	// Lore生成
	@Nullable
	default LoreProvider<I> getLoreProvider() {
		return null;
	}

	/**
	 * Loreを動的に変更する処理を提供します。
	 * <b>この処理でLoreのサイズを変化させることは許可されていません。</b>
	 */
	@Nullable
	default DynamicLoreUpdatable getDynamicUpdater() {
		return null;
	}
}
