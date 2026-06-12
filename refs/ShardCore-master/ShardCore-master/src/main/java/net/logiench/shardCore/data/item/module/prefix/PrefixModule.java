package net.logiench.shardCore.data.item.module.prefix;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.logiench.logienchlibv2.api.minecraft.text.ChatColor;
import net.logiench.logienchlibv2.api.minecraft.text.ComponentUtil;
import net.logiench.shardCore.core.item.base.def.EquipmentItem;
import net.logiench.shardCore.core.item.base.def.ItemGroup;
import net.logiench.shardCore.core.item.base.module.*;
import net.logiench.shardCore.core.item.base.module.tools.LoreSection;
import net.logiench.shardCore.core.item.base.module.tools.StructuredLore;
import net.logiench.shardCore.core.item.system.generator.ItemDataBuilder;
import net.logiench.shardCore.core.item.system.loader.ItemLoader;
import net.logiench.shardCore.core.item.system.module.context.*;
import net.logiench.shardCore.core.item.system.module.context.data.EquipmentData;
import net.logiench.shardCore.core.stats.base.AttributeEnum;
import net.logiench.shardCore.data.prefix.Prefix;
import net.logiench.shardCore.register.PrefixRegistry;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

@Singleton
public class PrefixModule implements ItemModule<EquipmentItem> {

	private final PrefixLogic logic;

	@Inject
	private PrefixModule(PrefixRegistry prefixRegistry) {
		this.logic = new PrefixLogic(prefixRegistry);
	}

	@Override
	public Class<EquipmentItem> getTargetType() {
		return EquipmentItem.class;
	}

	@Override
	public String getModuleKey() {
		return "prefix";
	}

	@Override
	public @Nullable ItemReader<EquipmentItem> getReader() {
		return logic;
	}

	@Override
	public @Nullable ItemStatsUpdater<EquipmentItem> getUpdater() {
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
	private static class PrefixLogic implements
		ItemReader<EquipmentItem>, ItemStatsUpdater<EquipmentItem>, ItemStatsCalculator<EquipmentItem>, ItemProcessor<EquipmentItem>, LoreProvider<EquipmentItem> {

		private final PrefixRegistry prefixRegistry;

		@Inject
		private PrefixLogic(PrefixRegistry prefixRegistry) {
			this.prefixRegistry = prefixRegistry;
		}

		@Override
		public void read(@NonNull ItemLoader loader, @NonNull EquipmentItem data, @NonNull ReadContext context) {
			Prefix prefix = prefixRegistry.get(loader.getLoadedItem().getItemData(PrefixKeys.PDC_PREFIX));
			if (prefix != null) {
				context.put(PrefixKeys.CTX_PREFIX, prefix);
			}
		}

		@Override
		public void update(UpdateContext<? extends EquipmentItem> context) {
			Prefix prefix = context.getUParams().get(PrefixKeys.UDT_SET_PREFIX);
			if (prefix == null) {
				return;
			}
			context.put(PrefixKeys.CTX_PREFIX, prefix);
			context.editGParams(p -> p.put(PrefixKeys.GEN_SET_PREFIX, prefix));
		}

		@Override
		public void calculate(RandomGenerator random, CalculationContext<? extends EquipmentItem> context) {
			ItemGroup itemGroup = context.getData().getItemType().getGroup();

			Prefix prefix = context.getGParams().get(PrefixKeys.GEN_SET_PREFIX);
			if (prefix != null) {
				// このPrefixを付与できるアイテムか確認
				if (!prefix.getTargetItemGroups().contains(itemGroup)) {
					throw new IllegalArgumentException("[PrefixModule] 指定されたPrefixはこのアイテムに付与できません。 " +
						"PrefixId: '" + prefix.getId() + "',　対象アイテムのグループ: " + itemGroup.name());
				}
			} else {
				// 外部指定がない場合は抽選処理
				List<? extends Prefix> prefixes = prefixRegistry.getPrefixes(itemGroup);
				if (prefixes.isEmpty()) {
					// このアイテムグループにはPrefixが存在しない
					return;
				}
				prefix = prefixes.get(random.nextInt(prefixes.size()));
			}
			context.put(PrefixKeys.CTX_PREFIX, prefix);
		}

		@Override
		public void process(GenerationContext<? extends EquipmentItem> context) {
			Prefix prefix = context.get(PrefixKeys.CTX_PREFIX);
			if (prefix == null) {
				return;
			}
			context.getItem().setItemData(PrefixKeys.PDC_PREFIX, prefix.getId());

			ItemDataBuilder builder = EquipmentData.getItemDataBuilder(context);
			builder.addStats(prefix.getAdditionalEffects());
			builder.addStats(prefix.getAdditionalEffects());
		}

		@Override
		public void updateLore(StructuredLore structuredLore, GenerationContext<? extends EquipmentItem> context) {
			Prefix prefix = context.get(PrefixKeys.CTX_PREFIX);
			if (prefix == null) {
				return;
			}

			List<Component> lore = structuredLore.getOrCreateSection(LoreSection.PREFIX);
			lore.add(ComponentUtil.NOT_ITALIC.append(Component.text("<<"))
				.append(prefix.getName())
				.append(Component.text(">>"))
				.color(ChatColor.YELLOW));
			for (Map.Entry<AttributeEnum, Double> entry : prefix.getAdditionalEffects().entrySet()) {
				lore.add(entry.getKey().toDisplay(entry.getValue()));
			}
		}

		@Override
		public int checksum(BaseContext context) {
			Prefix prefix = context.get(PrefixKeys.CTX_PREFIX);
			if (prefix == null) {
				return 0;
			}
			return prefix.getId().hashCode();
		}
	}
}
