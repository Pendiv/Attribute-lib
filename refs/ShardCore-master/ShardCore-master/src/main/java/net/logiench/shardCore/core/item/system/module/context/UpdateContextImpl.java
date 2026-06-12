package net.logiench.shardCore.core.item.system.module.context;

import lombok.Getter;
import net.logiench.shardCore.core.item.base.def.ShardItem;
import net.logiench.shardCore.core.item.system.module.params.GenerationParameters;
import net.logiench.shardCore.core.item.system.module.params.UpdateParameters;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * <b>重要</b>
 * このクラスでは<code>GenerationParameters</code>を編集可能な状態にする必要があります。
 */
public class UpdateContextImpl<T extends ShardItem> implements UpdateContext<T>, ReadContext {

	@Getter(onMethod = @__(@Override))
	private final T data;
	@Getter
	private final GenerationParameters gParams;
	@Getter(onMethod = @__(@Override))
	private final UpdateParameters uParams;
	private final Map<ContextKey<?>, Object> results = new HashMap<>();

	public UpdateContextImpl(T data, GenerationParameters gParams, UpdateParameters uParams) {
		if (gParams.isImmutable()) {
			throw new IllegalArgumentException("GenerationParametersは編集可能である必要があります");
		}
		this.uParams = uParams;
		this.data = data;
		this.gParams = gParams;
	}

	@Override
	public void editGParams(@NotNull Consumer<GenerationParameters> editor) {
		editor.accept(this.gParams);
	}

	@Override
	public <V> void put(ContextKey<V> key, V value) {
		results.put(key, value);
	}

	@Override
	@Nullable
	@Unmodifiable // 内部的にはUnmodifiableではないかもしれないけど、取得して編集してほしくない
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

	public void merge(ReadContext context) {
		results.putAll(context.getAll());
	}

	@Override
	public Map<ContextKey<?>, Object> getAll() {
		return Map.copyOf(results);
	}
}
