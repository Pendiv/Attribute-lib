package net.logiench.shardCore.core.item.system.module.context;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class InspectContext implements ReadContext {
	public static final InspectContext EMPTY = new InspectContext() {
		@Override
		public <V> void put(ContextKey<V> key, V value) {
			throw new UnsupportedOperationException();
		}
	};

	final Map<ContextKey<?>, Object> results = new HashMap<>();

	@Override
	public <V> void put(ContextKey<V> key, V value) {
		results.put(key, value);
	}

	@Override
	@Nullable
	@Unmodifiable
	public <V> V get(ContextKey<V> key) {
		return key.cast(results.get(key));
	}

	@Override
	@Contract(value = "_, !null -> !null")
	@Unmodifiable
	public <V> V get(ContextKey<V> key, V defaultValue) {
		Object val = results.get(key);
		if (val == null) {
			return defaultValue;
		}
		try {
			return key.cast(val);
		} catch (ClassCastException e) {
			return defaultValue;
		}
	}

	@Override
	public Map<ContextKey<?>, Object> getAll() {
		return Map.copyOf(results);
	}
}
