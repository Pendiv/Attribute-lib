package net.logiench.shardCore.core.stats.base.view;

import net.logiench.shardCore.core.stats.base.AttributeEnum;
import org.jspecify.annotations.NonNull;

public record AttributeValue(AttributeEnum key, double value) implements Comparable<AttributeValue> {
	@Override
	public int compareTo(@NonNull AttributeValue o) {
		return key.compareTo(o.key);
	}
}
