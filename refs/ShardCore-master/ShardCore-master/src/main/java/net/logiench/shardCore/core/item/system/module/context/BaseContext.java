package net.logiench.shardCore.core.item.system.module.context;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.Nullable;

public interface BaseContext {

	@Nullable
	@Unmodifiable
	<V> V get(ContextKey<V> key);

	@Contract(value = "_, !null -> !null")
	@Unmodifiable
	<V> V get(ContextKey<V> key, V defaultValue);
}
