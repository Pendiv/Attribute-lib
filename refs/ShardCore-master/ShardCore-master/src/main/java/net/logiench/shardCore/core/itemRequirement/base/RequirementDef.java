package net.logiench.shardCore.core.itemRequirement.base;

public interface RequirementDef<D extends RequirementDef<D>> {
	Class<? extends RequirementResolver<D, ?>> getResolverType();
}
