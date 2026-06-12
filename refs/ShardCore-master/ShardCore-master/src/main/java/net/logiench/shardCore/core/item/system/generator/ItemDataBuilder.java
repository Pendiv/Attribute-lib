package net.logiench.shardCore.core.item.system.generator;

import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import net.logiench.shardCore.core.stats.base.AttributeEnum;
import net.logiench.shardLib.api.ShardLibProvider;
import net.logiench.shardLib.api.item.ItemAPI;
import net.logiench.shardLib.api.item.ItemData;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

public class ItemDataBuilder {
	private static final ItemData.Builder EMPTY_BUILDER;

	static {
		ItemAPI api = ShardLibProvider.get().getItemAPI();
		EMPTY_BUILDER = api.getItemData(api.generate(Material.STONE)).orElseThrow()
			.toBuilder().clearBaseStats();
	}

	private final Map<String, Double> editedStats;

	public ItemDataBuilder(SuperItemStack item) {
		this(ShardLibProvider.get().getItemAPI().getItemData(item).orElseThrow());
	}

	public ItemDataBuilder(ItemData itemData) {
		this.editedStats = new HashMap<>(itemData.getBaseStats());
	}

	public double getStat(String key) {
		return getStat(key, 0);
	}

	public double getStat(String key, double defaultValue) {
		return editedStats.getOrDefault(key, defaultValue);
	}

	public void setStat(String key, double value) {
		editedStats.put(key, value);
	}

	public void setStats(Map<AttributeEnum, Double> stats) {
		for (Map.Entry<AttributeEnum, Double> entry : stats.entrySet()) {
			setStat(entry.getKey().getId(), entry.getValue());
		}
	}

	/**
	 * 現在その要素がnullの場合その値を設定します
	 *
	 * @return 編集後の値
	 */
	public Double setIfAbsent(String key, double value) {
		return editedStats.putIfAbsent(key, value);
	}

	public double addStat(AttributeEnum key, double value) {
		return addStat(key, 0, value);
	}

	public double addStat(AttributeEnum key, double defaultValue, double value) {
		return addStat(key.getId(), defaultValue, value);
	}

	public double addStat(String key, double value) {
		return addStat(key, 0, value);
	}

	public double addStat(String key, double defaultValue, double value) {
		double replaceValue = editedStats.getOrDefault(key, defaultValue) + value;
		editedStats.put(key, replaceValue);
		return replaceValue;
	}

	public void addStats(Map<AttributeEnum, Double> stats) {
		for (Map.Entry<AttributeEnum, Double> entry : stats.entrySet()) {
			addStat(entry.getKey().getId(), entry.getValue());
		}
	}

	void applyData(SuperItemStack item) {
		// ItemData.Builder がcloneできないので同時に使用しないようにして対応
		synchronized (EMPTY_BUILDER) {
			ShardLibProvider.get().getItemAPI().setItemData(item, EMPTY_BUILDER.setBaseStats(editedStats).build());
			EMPTY_BUILDER.clearBaseStats();
		}
	}
}
