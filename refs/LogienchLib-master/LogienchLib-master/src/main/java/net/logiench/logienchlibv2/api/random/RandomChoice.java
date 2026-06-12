package net.logiench.logienchlibv2.api.random;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

public abstract class RandomChoice<T> extends ArrayList<RandomRecord<T>> implements Cloneable {
	public RandomChoice(@NotNull Collection<? extends RandomRecord<T>> c) {
		super(c);
	}

	public RandomChoice(int initialCapacity) {
		super(initialCapacity);
	}

	public RandomChoice() {
		super();
	}

	public void add(int index, T value, double weight) {
		add(index, new RandomRecord<>(value, weight));
	}

	public boolean add(T value, double weight) {
		return add(new RandomRecord<>(value, weight));
	}

	public void addFirst(T value, double weight) {
		addFirst(new RandomRecord<>(value, weight));
	}

	public void addLast(T value, double weight) {
		addLast(new RandomRecord<>(value, weight));
	}

	@Override
	@SuppressWarnings("unchecked")
	public ArrayList<RandomRecord<T>> clone() {
		return (ArrayList<RandomRecord<T>>) super.clone();
	}
}
