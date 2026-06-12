package net.logiench.shardCore.core.itemRequirement.base;

import net.logiench.shardCore.core.item.base.def.EquipmentItem;
import net.logiench.shardCore.core.item.system.module.context.CalculationContext;

import java.util.Collection;
import java.util.random.RandomGenerator;

public interface RequirementResolver<D extends RequirementDef<D>, I extends EquipmentItem> {
	Class<I> getContextDataType();

	Collection<ItemRequirement<?>> resolver(RandomGenerator random, D def, CalculationContext<? extends I> context);
}
