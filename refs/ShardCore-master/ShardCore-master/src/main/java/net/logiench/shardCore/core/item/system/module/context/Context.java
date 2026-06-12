package net.logiench.shardCore.core.item.system.module.context;

import lombok.Getter;
import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import net.logiench.shardCore.core.item.base.def.EquipmentItem;
import net.logiench.shardCore.core.item.base.def.ShardItem;
import net.logiench.shardCore.core.item.system.module.context.data.EquipmentData;
import net.logiench.shardCore.core.item.system.module.params.GenerationParameters;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.HashMap;
import java.util.Map;

public class Context<T extends ShardItem> implements ReadContext, UnidentifiedContext<T>, CalculationContext<T>, GenerationContext<T> {

	@Getter(onMethod_ = {@Override})
	private final SuperItemStack item;
	@Getter(onMethod_ = {@Override})
	private final T data;
	@Getter(onMethod_ = {@Override, @Unmodifiable})
	private final GenerationParameters gParams;
	private final Map<ContextKey<?>, Object> results = new HashMap<>();

	@SuppressWarnings("unchecked")
	public Context(SuperItemStack item, T data, @Nullable GenerationParameters gParams) {
		this.item = item;
		this.data = data;
		this.gParams = gParams == null ? GenerationParameters.empty() : gParams;

		// すべてのアイテムに統一して必要な処理だからここで

		// EquipmentItem以上だったらアイテムデータビルダーを作成。適応はItemGeneratorで行う
		if (data instanceof EquipmentItem) {
			EquipmentData.setItemDataBuilder((Context<? extends EquipmentItem>) this);
		}
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
