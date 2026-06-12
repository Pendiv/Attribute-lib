package net.logiench.shardCore.core.item.system.module.context;

import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import net.logiench.shardCore.core.item.base.def.ShardItem;

public interface GenerationContext<T extends ShardItem> extends BaseContext {

	T getData();

	SuperItemStack getItem();
}
