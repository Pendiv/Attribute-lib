package net.logiench.shardCore.core.item.system.module.context;

import net.logiench.shardCore.core.item.base.def.ShardItem;

public interface UnidentifiedContext<T extends ShardItem> extends BaseContext {

	T getData();
}
