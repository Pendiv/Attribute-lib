package net.logiench.shardCore.core.stats.base.view;

import net.logiench.logienchlibv2.api.random.RandomRecord;
import net.logiench.shardCore.core.stats.base.AttributeEnum;

import java.util.ArrayList;
import java.util.List;

public class SubStatsViewBuilder {
	private final List<RandomRecord<AttributeValue>> data = new ArrayList<>();

	public SubStatsViewBuilder add(AttributeEnum key, double value, double weight) {
		data.add(new RandomRecord<>(new AttributeValue(key, value), weight));
		return this;
	}

	public SubStatsView build() {
		return new SubStatsView(data);
	}
}
