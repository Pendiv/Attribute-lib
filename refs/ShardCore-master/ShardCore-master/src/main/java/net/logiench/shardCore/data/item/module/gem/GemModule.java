package net.logiench.shardCore.data.item.module.gem;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.logiench.logienchlibv2.api.minecraft.data.DataContainer;
import net.logiench.logienchlibv2.api.minecraft.text.ChatColor;
import net.logiench.logienchlibv2.api.minecraft.text.ComponentUtil;
import net.logiench.shardCore.core.item.base.def.EquipmentItem;
import net.logiench.shardCore.core.item.base.def.GemItem;
import net.logiench.shardCore.core.item.base.module.*;
import net.logiench.shardCore.core.item.base.module.tools.LoreSection;
import net.logiench.shardCore.core.item.base.module.tools.StructuredLore;
import net.logiench.shardCore.core.item.system.loader.ItemLoader;
import net.logiench.shardCore.core.item.system.module.context.*;
import net.logiench.shardCore.register.GemRegistry;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

@Singleton
public class GemModule implements ItemModule<EquipmentItem> {

	private final GemLogic logic;

	@Inject
	private GemModule(GemRegistry gemRegistry) {
		this.logic = new GemLogic(gemRegistry);
	}

	@Override
	public Class<EquipmentItem> getTargetType() {
		return EquipmentItem.class;
	}

	@Override
	public String getModuleKey() {
		return "gem";
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
	private static class GemLogic implements
		ItemReader<EquipmentItem>, ItemStatsUpdater<EquipmentItem>, ItemStatsCalculator<EquipmentItem>, ItemProcessor<EquipmentItem>, LoreProvider<EquipmentItem> {

		private static final Component EMPTY_SLOT_TEXT = ComponentUtil.NOT_ITALIC.append(Component.text("◇ ", ChatColor.WHITE)).append(Component.text("空きスロット", ChatColor.GRAY)).compact();
		private static final Component FULL_SLOT_TEXT = ComponentUtil.NOT_ITALIC.append(Component.text("◆ ", ChatColor.WHITE));

		private final GemRegistry gemRegistry;

		private GemLogic(GemRegistry gemRegistry) {
			this.gemRegistry = gemRegistry;
		}

		@Override
		public void read(@NonNull ItemLoader loader, @NonNull EquipmentItem data, @NonNull ReadContext context) {
			Integer size = loader.getLoadedItem().getItemData(GemKeys.PDC_GEM_SLOT_SIZE);
			if (size != null) {
				context.put(GemKeys.CTX_GEM_SLOT_SIZE, size);
			}

			PersistentDataContainer container = loader.getLoadedItem().getItemData(GemKeys.PDC_GEM_DATA);
			if (container != null) {
				context.put(GemKeys.CTX_GEM_DATA, container.getKeys().stream()
					.map(gemRegistry::get)
					.filter(Objects::nonNull)
					.sorted()
					.toList());
			} else {
				context.put(GemKeys.CTX_GEM_DATA, List.of());
			}
		}

		@Override
		public void update(UpdateContext<? extends EquipmentItem> context) {
			List<GemItem> setGemItems = context.getUParams().get(GemKeys.UDT_SET_GEM);
			boolean isEdit = setGemItems != null;

			List<GemItem> gemItems = new ArrayList<>(
				Objects.requireNonNullElseGet(setGemItems,
					() -> context.get(GemKeys.CTX_GEM_DATA, List.of()))
			);

			List<GemItem> removeGemItems = context.getUParams().get(GemKeys.UDT_REMOVE_GEM);
			if (removeGemItems != null) {
				gemItems.removeAll(removeGemItems);
				isEdit = true;
			}
			List<GemItem> addGemItems = context.getUParams().get(GemKeys.UDT_ADD_GEM);
			if (addGemItems != null) {
				gemItems.addAll(addGemItems);
				isEdit = true;
			}

			if (isEdit) {
				// ジェムのサイズがスロットサイズを超えていたら変更を適応せずエラー
				int size = context.get(GemKeys.CTX_GEM_SLOT_SIZE, 0);
				if (gemItems.size() > size) {
					throw new IllegalArgumentException("[GemModule] 編集後のジェムスロットがサイズ制限を超えています。制限: " + size + ", サイズ: " + gemItems.size());
				}
				// 表示順をそろえるためにソート
				gemItems.sort(Comparator.naturalOrder());

				List<GemItem> newGemItems = List.copyOf(gemItems);
				context.put(GemKeys.CTX_GEM_DATA, newGemItems);
				context.editGParams(p -> p.put(GemKeys.GEN_DEFAULT_GEM, newGemItems));
			}
		}

		@Override
		public void calculate(RandomGenerator random, CalculationContext<? extends EquipmentItem> context) {
			List<GemItem> defaultGemData = context.getGParams().get(GemKeys.GEN_DEFAULT_GEM, List.of());
			int gemSlotSize = context.getData().getGemSlotSize();
			if (defaultGemData.size() > gemSlotSize) {
				throw new IllegalArgumentException("[GemModule] 指定されたジェムの個数はジェムスロットを超えています");
			}

			context.put(GemKeys.CTX_GEM_DATA, defaultGemData);
			// 将来的にGemSlotSizeが外部から指定できるようになった場合のデータ共有
			context.put(GemKeys.CTX_GEM_SLOT_SIZE, gemSlotSize);
		}

		@Override
		public void process(GenerationContext<? extends EquipmentItem> context) {
			Integer gemSlotSize = context.get(GemKeys.CTX_GEM_SLOT_SIZE);
			if (gemSlotSize == null) {
				throw new IllegalStateException("[GemModule] ジェムスロットのサイズが存在しません");
			}
			context.getItem().setItemData(GemKeys.PDC_GEM_SLOT_SIZE, gemSlotSize);

			List<GemItem> gemData = context.get(GemKeys.CTX_GEM_DATA);
			if (gemData == null || gemData.isEmpty()) {
				return;
			}
			DataContainer gemDataContainer = new DataContainer();
			for (GemItem defaultGem : gemData) {
				// PDCのキーとしてGemを保存、Valueは関係ない
				gemDataContainer.set(defaultGem.getKey(), PersistentDataType.BOOLEAN, true);
			}

			context.getItem().setItemData(GemKeys.PDC_GEM_DATA, gemDataContainer.getPDC());
		}

		@Override
		public void updateLore(StructuredLore structuredLore, GenerationContext<? extends EquipmentItem> context) {
			List<GemItem> gemData = context.get(GemKeys.CTX_GEM_DATA);
			Integer gemSlotSize = context.get(GemKeys.CTX_GEM_SLOT_SIZE);
			if (gemData == null || gemSlotSize == null) {
				return;
			}
			List<Component> lore = structuredLore.getOrCreateSection(LoreSection.GEM);
			int i = 0;
			for (GemItem gem : gemData) {
				lore.add(FULL_SLOT_TEXT.append(gem.getName()));
				i++;
			}
			while (i++ < gemSlotSize) {
				lore.add(EMPTY_SLOT_TEXT);
			}
		}

		@Override
		public int checksum(BaseContext context) {
			int checksum = context.get(GemKeys.CTX_GEM_SLOT_SIZE, 0).hashCode();
			for (GemItem gem : context.get(GemKeys.CTX_GEM_DATA, List.of())) {
				checksum = checksum * 31 + gem.getId().hashCode();
			}
			return checksum;
		}
	}
}
