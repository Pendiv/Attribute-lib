package net.logiench.shardCore.core.stats.base.view;

import net.logiench.logienchlibv2.api.random.RandomRecord;
import net.logiench.logienchlibv2.api.random.UniqueSerialChoice;
import net.logiench.shardCore.data.stats.keys.CoreStats;
import net.logiench.shardCore.data.stats.keys.player.PlayerStats;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SubStatsView {
	public static final SubStatsView DEFAULT = new SubStatsViewBuilder()
		.add(CoreStats.HP, 10, 10)
		.add(CoreStats.HP_REGEN, 5, 1)
		.add(PlayerStats.STRENGTH, 5, 20)
		.build();

	private final UniqueSerialChoice<AttributeValue> randomRecords;

	SubStatsView(@NotNull List<RandomRecord<AttributeValue>> randomRecords) {
		this.randomRecords = new UniqueSerialChoice<>(randomRecords);
	}

	public UniqueSerialChoice<AttributeValue>.Table getTable() {
		return randomRecords.getUniqueTable();
	}
}
