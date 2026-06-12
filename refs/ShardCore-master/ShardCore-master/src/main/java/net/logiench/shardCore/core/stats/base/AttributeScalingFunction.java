package net.logiench.shardCore.core.stats.base;

@FunctionalInterface
public interface AttributeScalingFunction {
	double get(double value, double level);
}
