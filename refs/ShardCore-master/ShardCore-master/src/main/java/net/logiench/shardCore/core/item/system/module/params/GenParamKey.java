package net.logiench.shardCore.core.item.system.module.params;

import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import net.logiench.shardCore.core.item.system.generator.Key;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GenParamKey<T> implements Key<T> {
	// 全てのキーを管理するレジストリ (文字列 -> ParamKey)
	private static final Map<String, GenParamKey<?>> REGISTRY = new ConcurrentHashMap<>();

	@Getter
	private final String key;
	private final TypeToken<T> typeToken;

	// クラス型で登録する場合 (例: Long.class)
	public GenParamKey(String key, Class<T> type) {
		this(key, TypeToken.get(type));
	}

	// ジェネリクス型で登録する場合 (例: new TypeToken<Map<Enum, Double>>(){})
	public GenParamKey(String key, TypeToken<T> typeToken) {
		this.key = key;
		this.typeToken = typeToken;
		// 自動登録 (重複キーチェックを入れても良い)
		if (REGISTRY.put(key, this) != null) {
			throw new RuntimeException("Duplicate key: " + key);
		}
	}

	public Type getType() {
		return typeToken.getType();
	}

	/**
	 * 文字列キーから、型情報を持つ正規のParamKeyを取得します
	 */
	public static GenParamKey<?> getByKey(String key) {
		return REGISTRY.get(key);
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
		GenParamKey<?> paramKey = (GenParamKey<?>) o;
		return key.equals(paramKey.key);
	}

	@Override
	public int hashCode() {
		return key.hashCode();
	}
}