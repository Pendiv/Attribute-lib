package net.logiench.shardCore.core.item.system.module.context;

import java.util.Map;

public interface ReadContext extends BaseContext {

	<V> void put(ContextKey<V> key, V value);

	Map<ContextKey<?>, Object> getAll();
}
