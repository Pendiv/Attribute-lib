package net.logiench.shardCore.core.item.system.module.params;

import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import net.logiench.shardCore.core.item.system.generator.Key;

import java.lang.reflect.Type;

public class UpdateParamKey<T> implements Key<T> {

	@Getter
	private final String key;
	private final TypeToken<T> typeToken;

	public UpdateParamKey(String key, Class<T> type) {
		this(key, TypeToken.get(type));
	}

	public UpdateParamKey(String key, TypeToken<T> typeToken) {
		this.key = key;
		this.typeToken = typeToken;
	}

	public Type getType() {
		return typeToken.getType();
	}

	// Mapのキーとして使うためのメソッド
	@Override
	public String toString() {
		return key;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		UpdateParamKey<?> paramKey = (UpdateParamKey<?>) o;
		return key.equals(paramKey.key);
	}

	@Override
	public int hashCode() {
		return key.hashCode();
	}
}
