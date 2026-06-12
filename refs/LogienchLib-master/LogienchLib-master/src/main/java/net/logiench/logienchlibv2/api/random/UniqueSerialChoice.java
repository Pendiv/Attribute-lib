package net.logiench.logienchlibv2.api.random;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/// 直列かつ重複のないランダムを取得します
public class UniqueSerialChoice<T> extends RandomChoice<T> {
	public UniqueSerialChoice(int initialCapacity) {
		super(initialCapacity);
	}

	public UniqueSerialChoice() {
		super();
	}

	public UniqueSerialChoice(@NotNull Collection<? extends RandomRecord<T>> c) {
		super(c);
	}

	public double getTotalWeight() {
		double totalWeight = 0;
		for (RandomRecord<T> r : this) {
			totalWeight += r.weight();
		}
		return totalWeight;
	}

	public Table getUniqueTable() {
		return new Table(this, getTotalWeight());
	}

	@Override
	public UniqueSerialChoice<T> clone() {
		return new UniqueSerialChoice<>(super.clone());
	}

	public class Table {
		private final Random random = new Random();
		private final List<RandomRecord<T>> records;
		private double totalWeight;

		private Table(Collection<? extends RandomRecord<T>> c, double totalWeight) {
			this.records = new ArrayList<>(c);
			this.totalWeight = totalWeight;
		}

		/**
		 * 各要素の重みから、ランダムな要素を取得します。
		 * 取得された項目は削除されます。
		 *
		 * @return 選択されたランダムな要素
		 */
		public RandomRecord<T> getRandom() {
			if (records.isEmpty()) {
				return null;
			}
			if (records.size() == 1) {
				return records.removeFirst();
			}
			double random = this.random.nextDouble(totalWeight);
			for (RandomRecord<T> r : records) {
				if (random < r.weight()) {
					this.totalWeight -= r.weight();
					records.remove(r);
					return r;
				}
				random -= r.weight();
			}
			return null;
		}

		public int size() {
			return records.size();
		}
	}
}
