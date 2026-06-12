package net.logiench.shardCore.core.itemRequirement.base;

import org.jspecify.annotations.NonNull;

public record ItemRequirement<T>(RequirementType<T> type, T value) implements Comparable<ItemRequirement<?>> {
	@Override
	public int compareTo(@NonNull ItemRequirement<?> o) {
		return type.getKeyName().compareTo(o.type.getKeyName());
	}
}
