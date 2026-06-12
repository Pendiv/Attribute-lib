package net.logiench.shardCore.core.item.system.generator;

public interface Key<T> {
	@SuppressWarnings("unchecked")
	default T cast(Object object) {
		return (T) object;
	}
}
