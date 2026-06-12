package net.logiench.shardCore.core.loot.base;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class LootTable<T> {
	private final List<LootPool<T>> pools;

	private LootTable(List<LootPool<T>> pools) {
		this.pools = pools;
	}

	/**
	 * コンテキストなしで生成（条件判定にはnullが渡されます）
	 */
	public List<T> generate() {
		return generate(null);
	}

	/**
	 * コンテキストを指定して生成
	 * @param context 条件判定に使われるオブジェクト (EntityDeathEventなど)
	 */
	public List<T> generate(@Nullable Object context) {
		if (pools.isEmpty()) {
			return Collections.emptyList();
		}

		List<T> result = new ArrayList<>();
		for (LootPool<T> pool : pools) {
			pool.generateTo(result, context);
		}
		return result;
	}

	public static class Builder<T> {
		private final List<LootPool<T>> pools = new ArrayList<>();

		public Builder<T> addPool(Consumer<LootPool.Builder<T>> consumer) {
			LootPool.Builder<T> builder = new LootPool.Builder<>();
			consumer.accept(builder);
			pools.add(builder.build());
			return this;
		}

		public LootTable<T> build() {
			return new LootTable<>(List.copyOf(pools));
		}
	}
}