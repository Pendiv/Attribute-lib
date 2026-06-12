package net.logiench.shardCore.data.prefix;

import net.kyori.adventure.text.Component;
import net.logiench.shardCore.core.item.base.def.ItemGroup;
import net.logiench.shardCore.core.stats.base.AttributeEnum;

import java.util.List;
import java.util.Map;

public interface Prefix {
	String getId();

	Component getName();

	List<ItemGroup> getTargetItemGroups();

	Map<AttributeEnum, Double> getAdditionalEffects();
}
