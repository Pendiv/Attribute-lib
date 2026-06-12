package net.logiench.logienchlibv2.api.random;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

@SuppressWarnings("unused")
/// 並列
public class ParallelChoice<T> extends RandomChoice<T> {
	private final Random random = new Random();
	private final double maxWeight;

	public ParallelChoice(int initialCapacity, double maxWeight) {
		super(initialCapacity);
		if (maxWeight <= 0) {
			throw new IllegalArgumentException("This parameter must be greater than 0");
		}
		this.maxWeight = maxWeight;
	}

	/// 最大の重みは100に設定されます
	public ParallelChoice() {
		this(100);
	}

	public ParallelChoice(double maxWeight) {
		super();
		this.maxWeight = maxWeight;
	}

	public ParallelChoice(@NotNull Collection<? extends RandomRecord<T>> c, double maxWeight) {
		super(c);
		this.maxWeight = maxWeight;
	}

	public double getMaxWeight() {
		return maxWeight;
	}

	/**
	 * 各要素の重みから、適合する全ての要素を取得します
	 *
	 * @return 選択されたランダムな要素
	 */
	public List<RandomRecord<T>> getRandom() {
		List<RandomRecord<T>> result = new ArrayList<>();
		for (RandomRecord<T> r : this) {
			if (r.weight() >= maxWeight || r.weight() > random.nextDouble(maxWeight)) {
				result.add(r);
			}
		}
		return List.copyOf(result);
	}
}
