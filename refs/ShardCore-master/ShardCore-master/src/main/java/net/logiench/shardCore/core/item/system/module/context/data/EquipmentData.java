package net.logiench.shardCore.core.item.system.module.context.data;

import net.logiench.shardCore.core.item.base.def.EquipmentItem;
import net.logiench.shardCore.core.item.system.generator.ItemDataBuilder;
import net.logiench.shardCore.core.item.system.module.context.Context;
import net.logiench.shardCore.core.item.system.module.context.ContextKey;
import net.logiench.shardCore.core.item.system.module.context.GenerationContext;

public class EquipmentData {
	private static final ContextKey<ItemDataBuilder> BUILDER_KEY = new ContextKey<>("item_data_builder");

	public static ItemDataBuilder getItemDataBuilder(GenerationContext<? extends EquipmentItem> context) {
		return context.get(BUILDER_KEY);
	}

	public static void setItemDataBuilder(Context<? extends EquipmentItem> context) {
		context.put(BUILDER_KEY, new ItemDataBuilder(context.getItem()));
	}
}
