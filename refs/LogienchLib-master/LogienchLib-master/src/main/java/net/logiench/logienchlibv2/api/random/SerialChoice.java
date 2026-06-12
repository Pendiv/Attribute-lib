package net.logiench.logienchlibv2.api.random;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Random;

/// 直列
public class SerialChoice<T> extends RandomChoice<T> {
	private final Random random = new Random();

	public SerialChoice(int initialCapacity) {
		super(initialCapacity);
	}

	public SerialChoice() {
		super();
	}

	public SerialChoice(@NotNull Collection<? extends RandomRecord<T>> c) {
		super(c);
	}

	public double getTotalWeight() {
		double totalWeight = 0;
		for (RandomRecord<T> r : this) {
			totalWeight += r.weight();
		}
		return totalWeight;
	}

	/**
	 * 各要素の重みから、ランダムな要素を取得します
	 *
	 * @return 選択されたランダムな要素
	 */
	public RandomRecord<T> getRandom() {
		if (isEmpty()) {
			return null;
		}
		if (size() == 1) {
			return getFirst();
		}
		double random = this.random.nextDouble(getTotalWeight());
		for (RandomRecord<T> r : this) {
			if (random < r.weight()) {
				return r;
			}
			random -= r.weight();
		}
		return null;
	}

	@Override
	public SerialChoice<T> clone() {
		return new SerialChoice<>(super.clone());
	}
}
