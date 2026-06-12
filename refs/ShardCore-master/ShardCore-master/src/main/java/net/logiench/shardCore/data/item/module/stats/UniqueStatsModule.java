package net.logiench.shardCore.data.item.module.stats;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.logiench.shardCore.core.item.base.def.EquipmentItem;
import net.logiench.shardCore.core.item.base.module.*;
import net.logiench.shardCore.core.item.base.module.tools.LoreSection;
import net.logiench.shardCore.core.item.base.module.tools.StructuredLore;
import net.logiench.shardCore.core.item.system.generator.ItemDataBuilder;
import net.logiench.shardCore.core.item.system.loader.ItemLoader;
import net.logiench.shardCore.core.item.system.module.context.BaseContext;
import net.logiench.shardCore.core.item.system.module.context.CalculationContext;
import net.logiench.shardCore.core.item.system.module.context.GenerationContext;
import net.logiench.shardCore.core.item.system.module.context.ReadContext;
import net.logiench.shardCore.core.item.system.module.context.data.EquipmentData;
import net.logiench.shardCore.core.stats.base.AttributeEnum;
import net.logiench.shardCore.data.item.module.level.LevelKeys;
import net.logiench.shardCore.data.item.module.level.LevelModule;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

@Singleton
public class UniqueStatsModule implements ItemModule<EquipmentItem> {

	private final UniqueStatsLogic logic;

	@Inject
	private UniqueStatsModule() {
		this.logic = new UniqueStatsLogic();
	}

	@Override
	public Class<EquipmentItem> getTargetType() {
		return EquipmentItem.class;
	}

	@Override
	public String getModuleKey() {
		return "unique_stats";
	}

	@Override
	public @Nullable ItemReader<EquipmentItem> getReader() {
		return logic;
	}

	@Override
	public @Nullable ItemStatsCalculator<EquipmentItem> getCalculator() {
		return logic;
	}

	@Override
	public @Nullable ItemProcessor<EquipmentItem> getProcessor() {
		return logic;
	}

	@Override
	public @Nullable LoreProvider<EquipmentItem> getLoreProvider() {
		return logic;
	}


	private static class UniqueStatsLogic implements ItemReader<EquipmentItem>, ItemStatsCalculator<EquipmentItem>,
		ItemProcessor<EquipmentItem>, LoreProvider<EquipmentItem> {

		private UniqueStatsLogic() {
		}

		@Override
		public void read(@NonNull ItemLoader loader, @NonNull EquipmentItem data, @NonNull ReadContext context) {
			Long level = context.get(LevelKeys.CTX_LEVEL);
			if (level == null) {
				throw new IllegalStateException("[UniqueStatsLogic] レベルのデータが存在しません。このモジュールは " + LevelModule.class.getSimpleName() + " との依存が必要です");
			}

			Map<AttributeEnum, Double> uniqueStats = new LinkedHashMap<>();
			for (Map.Entry<AttributeEnum, Double> entry : data.getUniqueBaseStats().entrySet()) {
				AttributeEnum key = entry.getKey();
				double value = key.getScalingValue(entry.getValue(), level);
				uniqueStats.put(key, value);
			}
			context.put(StatsKeys.CTX_UNIQUE_STATS, uniqueStats);
		}

		@Override
		public void calculate(RandomGenerator random, CalculationContext<? extends EquipmentItem> context) {
			Long level = context.get(LevelKeys.CTX_LEVEL);
			if (level == null) {
				throw new IllegalStateException("[UniqueStatsModule] レベルのデータが存在しません。このモジュールは " + LevelModule.class.getSimpleName() + " との依存が必要です");
			}
			Map<AttributeEnum, Double> uniqueStats = new LinkedHashMap<>();
			for (Map.Entry<AttributeEnum, Double> entry : context.getData().getUniqueBaseStats().entrySet()) {
				AttributeEnum key = entry.getKey();
				double value = key.getScalingValue(entry.getValue(), level);
				uniqueStats.put(key, value);
			}
			context.put(StatsKeys.CTX_UNIQUE_STATS, uniqueStats);
		}

		@Override
		public void process(GenerationContext<? extends EquipmentItem> context) {
			Map<AttributeEnum, Double> uniqueStats = context.get(StatsKeys.CTX_UNIQUE_STATS, Map.of());
			ItemDataBuilder builder = EquipmentData.getItemDataBuilder(context);

			for (Map.Entry<AttributeEnum, Double> entry : uniqueStats.entrySet()) {
				builder.addStat(entry.getKey(), entry.getValue());
			}
		}

		@Override
		public void updateLore(StructuredLore structuredLore, GenerationContext<? extends EquipmentItem> context) {
			Map<AttributeEnum, Double> uniqueStats = context.get(StatsKeys.CTX_UNIQUE_STATS);
			if (uniqueStats == null) {
				throw new IllegalStateException("[UniqueStatsModule] uniqueStatsのデータが存在しません");
			}

			List<Component> lore = structuredLore.getOrCreateSection(LoreSection.UNIQUE_STATS);
			for (Map.Entry<AttributeEnum, Double> entry : uniqueStats.entrySet()) {
				lore.add(entry.getKey().toDisplay(entry.getValue()));
			}
		}

		@Override
		public int checksum(BaseContext context) {
			Map<AttributeEnum, Double> uniqueStats = context.get(StatsKeys.CTX_UNIQUE_STATS);
			if (uniqueStats == null) {
				return 0;
			}
			int checksum = 0;
			for (Map.Entry<AttributeEnum, Double> entry : uniqueStats.entrySet()) {
				checksum = checksum * 31 + entry.getKey().getId().hashCode();
				checksum = checksum * 31 + Double.hashCode(entry.getValue());
			}
			return checksum;
		}
	}
}
