package net.logiench.shardLib.core.attribute.data;

import net.logiench.shardLib.api.attribute.data.AttributeOperationModifier;
import net.logiench.shardLib.api.attribute.data.ModifierOperation;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Stream;

/**
 * {@link ModifierOperation}のpriority昇順で{@link AttributeOperationModifier}を保存します
 */
public class OperationList implements Iterable<AttributeOperationModifier> {
	private final List<AttributeOperationModifier> attributeProviders = new ArrayList<>();
	private final EnumMap<ModifierOperation, Integer> operationIndexes = new EnumMap<>(ModifierOperation.class);

	public OperationList() {
		for (ModifierOperation operation : ModifierOperation.getSorted()) {
			operationIndexes.put(operation, 0);
		}
	}

	public void add(@NotNull AttributeOperationModifier modifier) {
		Integer index = operationIndexes.get(modifier.getOperation());
		if (index == null) {
			throw new IllegalArgumentException("No index found for modifier: " + modifier.getOperation());
		}
		attributeProviders.add(index, modifier);
		shiftIndexes(modifier.getOperation());
	}

	public void addAll(@NotNull Iterable<AttributeOperationModifier> modifiers) {
		for (AttributeOperationModifier modifier : modifiers) {
			add(modifier);
		}
	}

	public AttributeOperationModifier get(int index) {
		return attributeProviders.get(index);
	}

	public AttributeOperationModifier getFirst() {
		return attributeProviders.getFirst();
	}

	public AttributeOperationModifier getLast() {
		return attributeProviders.getLast();
	}

	@Override
	@NotNull
	public Iterator<AttributeOperationModifier> iterator() {
		return attributeProviders.iterator();
	}

	@NotNull
	public Stream<AttributeOperationModifier> stream() {
		return attributeProviders.stream();
	}

	public boolean remove(@NotNull AttributeOperationModifier modifier) {
		if (!attributeProviders.remove(modifier)) {
			return false;
		}
		unshiftIndexes(modifier.getOperation());
		return true;
	}

	public int size() {
		return attributeProviders.size();
	}

	public boolean isEmpty() {
		return attributeProviders.isEmpty();
	}

	public boolean hasSet() {
		// SETがあるはずの場所までスキップして検索する。sizeとの比較でも今はできるけど、ModifierOperationの今後の変更に強くする
		return attributeProviders.stream()
			.skip(operationIndexes.get(ModifierOperation.SET))
			.anyMatch(p -> p.getOperation() == ModifierOperation.SET);
	}

	public List<AttributeOperationModifier> toList() {
		return Collections.unmodifiableList(attributeProviders);
	}

	public List<AttributeOperationModifier> getModifiers(ModifierOperation operation) {
		return attributeProviders.stream()
			.skip(operationIndexes.get(operation))
			.filter(p -> p.getOperation() == operation)
			.toList();
	}

	private void shiftIndexes(ModifierOperation targetOperation) {
		boolean flag = true;
		for (ModifierOperation operation : ModifierOperation.getSorted()) {
			if (operation.equals(targetOperation)) {
				flag = false;
			}
			if (flag) {
				continue;
			}
			operationIndexes.computeIfPresent(operation, (k, v) -> v + 1);
		}
	}

	private void unshiftIndexes(ModifierOperation targetOperation) {
		boolean flag = true;
		for (ModifierOperation operation : ModifierOperation.getSorted()) {
			if (operation.equals(targetOperation)) {
				flag = false;
			}
			if (flag) {
				continue;
			}
			operationIndexes.computeIfPresent(operation, (k, v) -> v - 1);
		}
	}
}
