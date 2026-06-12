package net.logiench.shardCore.data.prefix;

import net.kyori.adventure.text.Component;
import net.logiench.logienchlibv2.api.minecraft.text.ChatColor;
import net.logiench.logienchlibv2.api.minecraft.text.ComponentUtil;
import net.logiench.shardCore.core.item.base.def.ItemGroup;
import net.logiench.shardCore.core.stats.base.AttributeEnum;
import net.logiench.shardCore.data.stats.keys.CoreStats;

import java.util.List;
import java.util.Map;

public class Power implements Prefix {
	@Override
	public String getId() {
		return "power";
	}

	@Override
	public Component getName() {
		return ComponentUtil.text(ChatColor.DARK_GRAY + "Tattered");
	}

	@Override
	public List<ItemGroup> getTargetItemGroups() {
		return List.of(ItemGroup.values());
	}

	@Override
	public Map<AttributeEnum, Double> getAdditionalEffects() {
		return Map.of(
			CoreStats.ATTACK_SPEED, -0.2d
			//			ItemStats.
		);
	}
}
