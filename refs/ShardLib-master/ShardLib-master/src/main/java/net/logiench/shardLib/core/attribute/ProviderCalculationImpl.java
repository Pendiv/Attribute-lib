package net.logiench.shardLib.core.attribute;

import net.logiench.shardLib.api.attribute.data.CalculationContext;
import net.logiench.shardLib.api.attribute.data.ProviderCalculation;

import java.util.function.ToDoubleFunction;

public record ProviderCalculationImpl(
	String key,
	ToDoubleFunction<CalculationContext> function
) implements ProviderCalculation {
}
