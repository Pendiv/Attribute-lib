package net.logiench.shardLib.core.item;

import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import net.logiench.shardLib.ShardLib;
import net.logiench.shardLib.api.item.ItemData;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

public class ItemDataImpl implements ItemData {
	private transient String id;
	private final Map<String, Double> baseStats;

	public ItemDataImpl(Map<String, Double> baseStats) {
		this.baseStats = Map.copyOf(baseStats);
	}

	@Override
	@NotNull
	public Map<String, Double> getBaseStats() {
		return baseStats;
	}

	@Override
	public @NotNull Optional<Double> getBaseStat(String id) {
		return Optional.ofNullable(baseStats.get(id));
	}

	@Override
	@NotNull
	public Builder toBuilder() {
		return new ItemDataBuilderImpl()
			.setBaseStats(baseStats);
	}

	public String toGson() {
		return ShardLib.getGson().toJson(this);
	}

	public static Optional<ItemData> fromGson(SuperItemStack item) {
		if (item == null) {
			return Optional.empty();
		}
		String json = item.getItemData(ItemDataKey.STATS);
		if (!item.hasItemData(ItemDataKey.IS_SHARD_ITEM) || json == null) {
			return Optional.empty();
		}
		try {
			ItemDataImpl itemData = ShardLib.getGson().fromJson(json, ItemDataImpl.class);
			return Optional.of(itemData);
		} catch (Exception ignored) {
			return Optional.empty();
		}
	}
}
