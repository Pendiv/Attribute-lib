package net.logiench.shardCore.data.item.module.stats;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.logiench.logienchlibv2.api.minecraft.text.ChatColor;
import net.logiench.logienchlibv2.api.minecraft.text.ComponentUtil;
import net.logiench.logienchlibv2.api.minecraft.text.LoreList;
import net.logiench.shardCore.core.item.base.def.EquipmentItem;
import net.logiench.shardCore.core.item.base.module.*;
import net.logiench.shardCore.core.item.base.module.tools.LoreSection;
import net.logiench.shardCore.core.item.base.module.tools.StructuredLore;
import net.logiench.shardCore.core.item.system.generator.ItemDataBuilder;
import net.logiench.shardCore.core.item.system.loader.ItemLoader;
import net.logiench.shardCore.core.item.system.module.context.*;
import net.logiench.shardCore.core.item.system.module.context.data.EquipmentData;
import net.logiench.shardCore.core.stats.base.AttributeEnum;
import net.logiench.shardCore.register.StatsRegistry;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.random.RandomGenerator;

@Singleton
public class MainStatsModule implements ItemModule<EquipmentItem> {

	private final MainStatsLogic logic;

	@Inject
	private MainStatsModule(StatsRegistry statsRegistry) {
		this.logic = new MainStatsLogic(statsRegistry);
	}

	@Override
	public Class<EquipmentItem> getTargetType() {
		return EquipmentItem.class;
	}

	@Override
	public String getModuleKey() {
		return "main_stats";
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

	@Override
	public @Nullable UnidentifiedLoreProvider<EquipmentItem> getUnidentifiedLore() {
		return logic;
	}


	@SuppressWarnings("ClassCanBeRecord")
	private static class MainStatsLogic implements ItemReader<EquipmentItem>, ItemStatsCalculator<EquipmentItem>,
		ItemProcessor<EquipmentItem>, LoreProvider<EquipmentItem>, UnidentifiedLoreProvider<EquipmentItem> {

		private static final double COMPLETENESS_MAX_VALUE = Math.nextUp(100d);

		private final StatsRegistry statsRegistry;

		private MainStatsLogic(StatsRegistry statsRegistry) {
			this.statsRegistry = statsRegistry;
		}

		@Override
		public void read(@NonNull ItemLoader loader, @NonNull EquipmentItem data, @NonNull ReadContext context) {
			String compString = loader.getLoadedItem().getItemData(StatsKeys.PDC_MAIN_COMPLETENESS);
			if (compString == null) {
				return;
			}
			Map<AttributeEnum, Double> mainStatsData = data.getMainStats();
			Map<AttributeEnum, ValueAndCompleteness> mainStats = new TreeMap<>();
			for (String idAndComp : compString.split(",")) {
				String[] idAndCompArray = idAndComp.split("=");
				AttributeEnum key = statsRegistry.get(idAndCompArray[0]);
				if (key == null) {
					throw new IllegalStateException("[MainStatsLogic] ステータスのキーが見つかりません。 key: " + idAndCompArray[0]);
				}
				try {
					double comp = Double.parseDouble(idAndCompArray[1]);
					double value = (mainStatsData.get(key) * comp) / 100d;

					mainStats.put(key, new ValueAndCompleteness(value, comp));
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("[MainStatsLogic] 完成度の値をDoubleに変換できません。 value: " + idAndCompArray[1]);
				}
			}

			context.put(StatsKeys.CTX_MAIN_STATS, mainStats);
		}

		@Override
		public void calculate(RandomGenerator random, CalculationContext<? extends EquipmentItem> context) {
			Map<AttributeEnum, Double> completenessMin = context.getGParams().get(StatsKeys.GEN_COMPLETENESS_MIN, Map.of());
			Map<AttributeEnum, Double> completenessMax = context.getGParams().get(StatsKeys.GEN_COMPLETENESS_MAX, Map.of());

			Map<AttributeEnum, ValueAndCompleteness> mainStats = new LinkedHashMap<>();
			for (Map.Entry<AttributeEnum, Double> entry : context.getData().getMainStats().entrySet()) {
				AttributeEnum key = entry.getKey();
				Double maxValue = completenessMax.get(key);
				if (maxValue != null) {
					maxValue = Math.nextUp(maxValue);
				} else {
					maxValue = COMPLETENESS_MAX_VALUE;
				}
				double minValue = completenessMin.getOrDefault(key, 0d);
				if (minValue > maxValue) {
					throw new IllegalArgumentException("[MainStatsModule] 完成度の下限、上限指定が異常です [Min: " + minValue + ", Max: " + Math.nextDown(maxValue) + "]");
				}
				double completeness = random.nextDouble(minValue, maxValue);
				double value = (entry.getValue() * completeness) / 100d;

				mainStats.put(key, new ValueAndCompleteness(value, completeness));
			}
			context.put(StatsKeys.CTX_MAIN_STATS, mainStats);
		}

		@Override
		public void process(GenerationContext<? extends EquipmentItem> context) {
			Set<Map.Entry<AttributeEnum, ValueAndCompleteness>> entrySet = context.get(StatsKeys.CTX_MAIN_STATS, Map.of()).entrySet();

			ItemDataBuilder builder = EquipmentData.getItemDataBuilder(context);
			List<String> compStringBuilder = new ArrayList<>(entrySet.size());
			for (Map.Entry<AttributeEnum, ValueAndCompleteness> entry : entrySet) {
				builder.addStat(entry.getKey(), entry.getValue().value());
				compStringBuilder.add(entry.getKey().getId() + "=" + entry.getValue().completeness());
			}

			context.getItem().setItemData(StatsKeys.PDC_MAIN_COMPLETENESS, String.join(",", compStringBuilder));
		}

		@Override
		public void updateLore(StructuredLore structuredLore, GenerationContext<? extends EquipmentItem> context) {
			Map<AttributeEnum, ValueAndCompleteness> mainStats = context.get(StatsKeys.CTX_MAIN_STATS);
			if (mainStats == null) {
				throw new IllegalStateException("[MainStatsModule] MainStatsが存在しません");
			}

			List<Component> lore = structuredLore.getOrCreateSection(LoreSection.MAIN_STATS);
			for (Map.Entry<AttributeEnum, ValueAndCompleteness> entry : mainStats.entrySet()) {
				ValueAndCompleteness value = entry.getValue();
				lore.add(entry.getKey().toDisplay(value.value())
					.appendSpace().append(completenessFormatter(value.completeness()))
				);
			}
		}

		@Override
		public int checksum(BaseContext context) {
			Map<AttributeEnum, ValueAndCompleteness> mainStats = context.get(StatsKeys.CTX_MAIN_STATS);
			if (mainStats == null) {
				return 0;
			}
			int checksum = 0;
			for (Map.Entry<AttributeEnum, MainStatsModule.ValueAndCompleteness> entry : mainStats.entrySet()) {
				checksum = checksum * 31 + entry.getKey().getId().hashCode();
				checksum = checksum * 31 + Double.hashCode(entry.getValue().value());
				checksum = checksum * 31 + Double.hashCode(entry.getValue().completeness());
			}
			return checksum;
		}

		@Override
		public void updateUnidentifiedLore(LoreList lore, UnidentifiedContext<? extends EquipmentItem> context) {
			Map<AttributeEnum, ValueAndCompleteness> mainStats = context.get(StatsKeys.CTX_MAIN_STATS, Map.of());
			double aveComp = mainStats.values().stream()
				.mapToDouble(ValueAndCompleteness::completeness).average().orElse(0);
			lore.add(ComponentUtil.NOT_ITALIC
				.append(Component.text("総合完成度: ", NamedTextColor.YELLOW))
				.append(Component.text("%d0%%".formatted(Math.floorDiv((int) aveComp, 10)), NamedTextColor.GREEN)));
		}

		private Component completenessFormatter(double completeness) {
			return Component.text("<%.2f%%>".formatted(completeness), ChatColor.DARK_GRAY);
		}
	}

	public record ValueAndCompleteness(double value, double completeness) {
	}
}
