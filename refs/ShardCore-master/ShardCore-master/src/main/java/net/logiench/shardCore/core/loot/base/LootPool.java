package net.logiench.shardCore.core.loot.base;

import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class LootPool<T> {
	private final NumberProvider rolls; // Pool自体の抽選回数
	private final NavigableMap<Double, LootEntry<T>> entries;
	private final List<LootCondition> conditions; // ★追加: 実行条件
	private final double totalWeight;

	private LootPool(NumberProvider rolls, NavigableMap<Double, LootEntry<T>> entries, List<LootCondition> conditions, double totalWeight) {
		this.rolls = rolls;
		this.entries = entries;
		this.conditions = conditions;
		this.totalWeight = totalWeight;
	}

	/**
	 * 指定されたリストに結果を追加します
	 *
	 * @param output  結果格納先
	 * @param context 条件判定用のコンテキスト
	 */
	public void generateTo(List<T> output, @Nullable Object context) {
		// 1. 条件チェック (1つでもfalseならこのPoolはスキップ)
		for (LootCondition condition : conditions) {
			if (!condition.test(context)) {
				return;
			}
		}

		int count = rolls.nextInt();
		if (count <= 0 || entries.isEmpty()) {
			return;
		}

		ThreadLocalRandom random = ThreadLocalRandom.current();

		for (int i = 0; i < count; i++) {
			double value = random.nextDouble() * totalWeight;
			var entryMap = entries.higherEntry(value);

			if (entryMap != null) {
				LootEntry<T> entry = entryMap.getValue();

				// ★追加: エントリごとの個数を決定して追加
				int amount = entry.amount().nextInt();
				for (int j = 0; j < amount; j++) {
					output.add(entry.item());
				}
			}
		}
	}

	// --- Builder ---
	public static class Builder<T> {
		private NumberProvider rolls = NumberProvider.constant(1);
		private final List<LootEntry<T>> entries = new ArrayList<>();
		private final List<LootCondition> conditions = new ArrayList<>();

		public Builder<T> rolls(NumberProvider provider) {
			this.rolls = provider;
			return this;
		}

		public Builder<T> rolls(int count) {
			return rolls(NumberProvider.constant(count));
		}

		public Builder<T> rolls(int min, int max) {
			return rolls(NumberProvider.uniform(min, max));
		}

		// 条件追加
		public Builder<T> condition(LootCondition condition) {
			this.conditions.add(condition);
			return this;
		}

		// 通常追加 (個数1固定)
		public Builder<T> add(T item, double weight) {
			return add(item, weight, NumberProvider.constant(1));
		}

		// ★追加: 個数指定付き追加 (count)
		public Builder<T> add(T item, double weight, int count) {
			return add(item, weight, NumberProvider.constant(count));
		}

		// ★追加: 個数範囲指定付き追加 (min-max)
		public Builder<T> add(T item, double weight, int min, int max) {
			return add(item, weight, NumberProvider.uniform(min, max));
		}

		// ★追加: 個数指定付き追加 (Provider)
		public Builder<T> add(T item, double weight, NumberProvider amount) {
			entries.add(new LootEntry<>(item, weight, amount));
			return this;
		}

		public LootPool<T> build() {
			NavigableMap<Double, LootEntry<T>> map = new TreeMap<>();
			double totalWeight = 0;
			for (var e : entries) {
				if (e.weight() <= 0) {
					continue;
				}
				totalWeight += e.weight();
				map.put(totalWeight, e);
			}
			return new LootPool<>(rolls, Collections.unmodifiableNavigableMap(map), List.copyOf(conditions), totalWeight);
		}
	}

	// エントリ定義に amount を追加
	record LootEntry<T>(T item, double weight, NumberProvider amount) {}
}