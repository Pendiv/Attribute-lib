package net.logiench.shardCore.data.stats.keys.player;

import net.logiench.shardCore.core.stats.base.AttributeEnum;
import net.logiench.shardCore.data.stats.keys.CoreStats;

import java.util.List;

public interface PlayerStatsOverride {
	AttributeEnum MAX_HP = CoreStats.MAX_HP.override(stats -> {
		return stats.get(CoreStats.LEVEL.getId()) * 20 + 20;
	}, List.of(CoreStats.LEVEL.getId()));
}
