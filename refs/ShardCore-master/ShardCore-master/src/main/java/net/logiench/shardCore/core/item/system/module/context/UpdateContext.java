package net.logiench.shardCore.core.item.system.module.context;

import net.logiench.shardCore.core.item.base.def.ShardItem;
import net.logiench.shardCore.core.item.system.module.params.GenerationParameters;
import net.logiench.shardCore.core.item.system.module.params.UpdateParameters;

import java.util.function.Consumer;

public interface UpdateContext<T extends ShardItem> extends BaseContext {

	T getData();

	UpdateParameters getUParams();

	void editGParams(Consumer<GenerationParameters> editor);

	<V> void put(ContextKey<V> key, V value);
}
