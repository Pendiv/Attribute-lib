package net.logiench.shardLib.core.data;

import net.logiench.shardLib.ShardLib;
import net.logiench.shardLib.api.data.CustomDataContainerAPI;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

public class CustomDataContainerAPIImpl implements CustomDataContainerAPI {
	private Map<String, String> values = new HashMap<>();

	@Override
	@NotNull
	public Set<String> getKeys() {
		return values.keySet();
	}

	@Override
	@NotNull
	public List<Object> getValues() {
		return values.values().stream()
			.map(json -> ShardLib.getGson().fromJson(ShardLib.getGson().fromJson(json, DataWrapper.class).json(), Object.class))
			.toList();
	}

	@Override
	@NotNull
	public <T> Optional<T> remove(@NotNull String key, @NotNull Class<T> type) {
		return cast(values.remove(key), type);
	}

	@Override
	@NotNull
	public Optional<Object> remove(@NotNull String key) {
		return remove(key, Object.class);
	}

	@Override
	public boolean has(@NotNull String key) {
		return values.containsKey(key);
	}

	@Override
	@NotNull
	public <T> Optional<T> get(@NotNull String key, @NotNull Class<T> type) {
		return cast(values.get(key), type);
	}

	@Override
	@NotNull
	public Optional<Object> get(@NotNull String key) {
		String json = values.get(key);
		if (json == null) {
			return Optional.empty();
		}
		DataWrapper wrapper = ShardLib.getGson().fromJson(json, DataWrapper.class);
		return Optional.ofNullable(ShardLib.getGson().fromJson(wrapper.json(), Object.class));
	}

	@Override
	@NotNull
	public Optional<Class<?>> getClass(@NotNull String key) {
		String json = values.get(key);
		if (json == null) {
			return Optional.empty();
		}
		DataWrapper wrapper = ShardLib.getGson().fromJson(json, DataWrapper.class);
		try {
			return Optional.of(Class.forName(wrapper.className()));
		} catch (ClassNotFoundException ignored) {
			return Optional.empty();
		}
	}

	@Override
	public <T> void set(@NotNull String key, T value) {
		values.put(key, ShardLib.getGson().toJson(
			new DataWrapper(
				value.getClass().getName(),
				ShardLib.getGson().toJson(value)
			)
		));
	}

	@Override
	@NotNull
	public <T> T getOrDefault(@NotNull String key, @NotNull Class<T> type, @NotNull T defaultValue) {
		return get(key, type).orElse(defaultValue);
	}

	@Override
	public <T> T edit(@NotNull String key, @NotNull Class<T> type, @NotNull Function<Optional<T>, T> edit) {
		T v = edit.apply(get(key, type));
		set(key, v);
		return v;
	}

	@NotNull
	private static <T> Optional<T> cast(String json, @NotNull Class<T> type) {
		if (json == null) {
			return Optional.empty();
		}
		DataWrapper wrapper = ShardLib.getGson().fromJson(json, DataWrapper.class);
		try {
			if (type.isAssignableFrom(Class.forName(wrapper.className()))) {
				return Optional.ofNullable(ShardLib.getGson().fromJson(wrapper.json(), type));
			}
		} catch (ClassNotFoundException ignored) {
		}
		return Optional.empty();
	}

	public String toGson() {
		return ShardLib.getGson().toJson(this);
	}

	public static Optional<CustomDataContainerAPI> fromGson(String json) {
		if (json == null) {
			return Optional.empty();
		}
		try {
			CustomDataContainerAPIImpl dataContainer = ShardLib.getGson().fromJson(json, CustomDataContainerAPIImpl.class);
			return Optional.of(dataContainer);
		} catch (Exception ignored) {
			return Optional.empty();
		}
	}

	@Override
	@NotNull
	public CustomDataContainerAPIImpl clone() {
		try {
			CustomDataContainerAPIImpl data = (CustomDataContainerAPIImpl) super.clone();
			data.values = new HashMap<>(values);
			return data;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}
}
