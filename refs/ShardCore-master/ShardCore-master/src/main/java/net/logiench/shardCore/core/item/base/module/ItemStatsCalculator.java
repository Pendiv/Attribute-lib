package net.logiench.shardCore.core.item.base.module;

import net.logiench.shardCore.core.item.base.def.ShardItem;
import net.logiench.shardCore.core.item.system.module.context.CalculationContext;

import java.util.random.RandomGenerator;

public interface ItemStatsCalculator<I extends ShardItem> {

	void calculate(RandomGenerator random, CalculationContext<? extends I> context);
}
