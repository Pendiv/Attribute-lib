package net.logiench.shardCore.core.item.system.module.context;

import net.logiench.shardCore.core.item.base.def.ShardItem;
import net.logiench.shardCore.core.item.system.module.params.GenerationParameters;
import org.jetbrains.annotations.Unmodifiable;

public interface CalculationContext<T extends ShardItem> extends BaseContext {

	T getData();

	@Unmodifiable
	GenerationParameters getGParams();

	<V> void put(ContextKey<V> key, V value);
}
