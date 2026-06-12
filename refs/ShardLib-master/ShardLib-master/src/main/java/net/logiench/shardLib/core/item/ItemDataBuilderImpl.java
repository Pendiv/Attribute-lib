package net.logiench.shardLib.core.item;

import net.logiench.shardLib.api.item.ItemData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ItemDataBuilderImpl implements ItemData.Builder {
	private final Map<String, Double> baseStats = new HashMap<>();

	@Override
	@NotNull
	public ItemDataBuilderImpl setBaseStat(@NotNull String id, double value) {
		baseStats.put(id, value);
		return this;
	}

	@Override
	@NotNull
	public ItemDataBuilderImpl setBaseStats(@NotNull Map<String, Double> baseStats) {
		this.baseStats.putAll(baseStats);
		return this;
	}

	@Override
	@NotNull
	public ItemDataBuilderImpl clearBaseStats() {
		baseStats.clear();
		return this;
	}

	@Override
	@NotNull
	public ItemDataImpl build() {
		return new ItemDataImpl(baseStats);
	}
}
