package net.logiench.shardCore.core.item.base.gem;

@FunctionalInterface
public interface GemAction<C> {
	void execute(C context);
}
