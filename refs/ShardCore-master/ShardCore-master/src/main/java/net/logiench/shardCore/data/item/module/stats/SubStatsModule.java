package net.logiench.shardCore.data.item.module.stats;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.logiench.logienchlibv2.api.random.RandomRecord;
import net.logiench.logienchlibv2.api.random.UniqueSerialChoice;
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
import net.logiench.shardCore.core.item.system.module.params.GenerationParameters;
import net.logiench.shardCore.core.stats.base.AttributeEnum;
import net.logiench.shardCore.core.stats.base.view.AttributeValue;
import net.logiench.shardCore.core.stats.base.view.SubStatsView;
import net.logiench.shardCore.data.item.module.level.LevelKeys;
import net.logiench.shardCore.data.item.module.level.LevelModule;
import net.logiench.shardCore.register.StatsRegistry;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

@Singleton
public class SubStatsModule implements ItemModule<EquipmentItem> {

	private final SubStatsLogic logic;

	@Inject
	private SubStatsModule(StatsRegistry statsRegistry) {
		this.logic = new SubStatsLogic(statsRegistry);
	}

	@Override
	public Class<EquipmentItem> getTargetType() {
		return EquipmentItem.class;
	}

	@Override
	public String getModuleKey() {
		return "sub_stats";
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


	@SuppressWarnings("ClassCanBeRecord")
	private static class SubStatsLogic implements
		ItemReader<EquipmentItem>, ItemStatsCalculator<EquipmentItem>, ItemProcessor<EquipmentItem>, LoreProvider<EquipmentItem> {

		private final StatsRegistry statsRegistry;

		private SubStatsLogic(StatsRegistry statsRegistry) {
			this.statsRegistry = statsRegistry;
		}

		@Override
		public void read(@NonNull ItemLoader loader, @NonNull EquipmentItem data, @NonNull ReadContext context) {
			String valueString = loader.getLoadedItem().getItemData(StatsKeys.PDC_SUB_STATS);
			if (valueString == null) {
				return;
			}
			NavigableSet<AttributeValue> choiceStats = new TreeSet<>();
			for (String attributeValue : valueString.split(",")) {
				String[] keyAndValue = attributeValue.split("=");
				if (keyAndValue.length != 2) {
					continue;
				}
				AttributeEnum key = statsRegistry.get(keyAndValue[0]);
				if (key == null) {
					throw new IllegalStateException("[SubStatsLogic] ステータスのキーが見つかりません。 key: " + keyAndValue[0]);
				}
				try {
					double value = Double.parseDouble(keyAndValue[1]);
					choiceStats.add(new AttributeValue(key, value));
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("[SubStatsLogic] ステータスの値をDoubleに変換できません。 value: " + keyAndValue[1]);
				}
			}
			context.put(StatsKeys.CTX_SUB_STATS, choiceStats);
		}

		@Override
		public void calculate(RandomGenerator random, CalculationContext<? extends EquipmentItem> context) {
			GenerationParameters params = context.getGParams();
			// サブステータスが選択される一覧(View)を取得する
			SubStatsView view = params.get(StatsKeys.GEN_SUB_STATS_VIEW, SubStatsView.DEFAULT);
			// 個数はRarity + param
			int subStatsCount = context.getData().getRarity().getSubStatsCount()
				+ params.get(StatsKeys.GEN_ADDITIONAL_SUB_STATS_AMOUNT, 0);

			// サブステータスの抽選先（重複なし）
			UniqueSerialChoice<AttributeValue>.Table subStatsTable = view.getTable();

			NavigableSet<AttributeValue> choiceStats = new TreeSet<>();
			for (int i = 0; i < subStatsCount; i++) {
				RandomRecord<AttributeValue> record = subStatsTable.getRandom();
				if (record == null) {
					// サブステータスが足りない場合はここに来る
					// 警告が必要かを確認
					break;
				}
				choiceStats.add(record.value());
			}
			context.put(StatsKeys.CTX_SUB_STATS, Collections.unmodifiableNavigableSet(choiceStats));
		}

		@Override
		public void process(GenerationContext<? extends EquipmentItem> context) {
			NavigableSet<AttributeValue> choiceStats = context.get(StatsKeys.CTX_SUB_STATS, Collections.emptyNavigableSet());
			ItemDataBuilder builder = EquipmentData.getItemDataBuilder(context);

			for (AttributeValue value : choiceStats) {
				builder.addStat(value.key(), value.value());
			}

			context.getItem().setItemData(StatsKeys.PDC_SUB_STATS, choiceStats.stream()
				.map(value -> value.key().getId() + "=" + value.value())
				.collect(Collectors.joining(",")));
		}

		@Override
		public void updateLore(StructuredLore structuredLore, GenerationContext<? extends EquipmentItem> context) {
			NavigableSet<AttributeValue> choiceStats = context.get(StatsKeys.CTX_SUB_STATS);
			Long level = context.get(LevelKeys.CTX_LEVEL);
			if (choiceStats == null) {
				throw new IllegalStateException("[SubStatsModule] 抽選されたSubStatsのデータが存在しません");
			}
			if (level == null) {
				throw new IllegalStateException("[SubStatsModule] レベルのデータが存在しません。このモジュールは " + LevelModule.class.getSimpleName() + " との依存が必要です");
			}

			List<Component> lore = structuredLore.getOrCreateSection(LoreSection.SUB_STATS);
			for (AttributeValue attributeValue : choiceStats) {
				// レベルでスケーリングして適応
				double value = attributeValue.key().getScalingValue(attributeValue.value(), level);
				lore.add(attributeValue.key().toDisplay(value));
			}
		}

		@Override
		public int checksum(BaseContext context) {
			NavigableSet<AttributeValue> choiceStats = context.get(StatsKeys.CTX_SUB_STATS);
			if (choiceStats == null) {
				return 0;
			}
			int checksum = 0;
			for (var stat : choiceStats) {
				checksum = checksum * 31 + stat.key().getId().hashCode();
				checksum = checksum * 31 + Double.hashCode(stat.value());
			}
			return checksum;
		}
	}
}
