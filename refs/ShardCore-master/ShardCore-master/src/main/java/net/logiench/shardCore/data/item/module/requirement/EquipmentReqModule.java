package net.logiench.shardCore.data.item.module.requirement;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.logiench.logienchlibv2.api.minecraft.data.DataContainer;
import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import net.logiench.logienchlibv2.api.minecraft.text.ChatColor;
import net.logiench.logienchlibv2.api.minecraft.text.ComponentUtil;
import net.logiench.shardCore.ShardCore;
import net.logiench.shardCore.core.item.base.def.EquipmentItem;
import net.logiench.shardCore.core.item.base.module.*;
import net.logiench.shardCore.core.item.base.module.tools.LoreSection;
import net.logiench.shardCore.core.item.base.module.tools.StructuredLore;
import net.logiench.shardCore.core.item.system.loader.ItemLoader;
import net.logiench.shardCore.core.item.system.module.context.*;
import net.logiench.shardCore.core.itemRequirement.base.ItemRequirement;
import net.logiench.shardCore.core.itemRequirement.base.RequirementDef;
import net.logiench.shardCore.core.itemRequirement.base.RequirementResolver;
import net.logiench.shardCore.core.itemRequirement.base.RequirementType;
import net.logiench.shardCore.register.RequirementRegistry;
import net.logiench.shardLib.api.player.PlayerCharacterAPI;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

@Singleton
public class EquipmentReqModule implements ItemModule<EquipmentItem> {

	private final EquipmentLogic logic;

	@Inject
	private EquipmentReqModule(RequirementRegistry registry) {
		this.logic = new EquipmentLogic(registry);
	}

	@Override
	public Class<EquipmentItem> getTargetType() {
		return EquipmentItem.class;
	}

	@Override
	public String getModuleKey() {
		return "requirement";
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
	public ItemStatsCalculator<EquipmentItem> getCalculator() {
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
	public @Nullable DynamicLoreUpdatable getDynamicUpdater() {
		return logic;
	}


	@SuppressWarnings("ClassCanBeRecord") //うるさい
	private static class EquipmentLogic implements
		ItemReader<EquipmentItem>, ItemStatsUpdater<EquipmentItem>, ItemStatsCalculator<EquipmentItem>, ItemProcessor<EquipmentItem>, LoreProvider<EquipmentItem>, DynamicLoreUpdatable {

		private static final Component CHECK_EMPTY = Component.text("☐ ", ChatColor.GRAY);
		private static final Component CHECK_ALLOW = Component.text("☑ ", ChatColor.GREEN);
		private static final Component CHECK_DENY = Component.text("☒ ", ChatColor.RED);

		private final RequirementRegistry requirementRegistry;

		private EquipmentLogic(RequirementRegistry requirementRegistry) {
			this.requirementRegistry = requirementRegistry;
		}

		// データロード

		@Override
		public void read(@NonNull ItemLoader loader, @NonNull EquipmentItem data, @NonNull ReadContext context) {
			List<ItemRequirement<?>> itemRequirements = new ArrayList<>();

			DataContainer container = RequirementType.getRequireContainer(loader.getLoadedItem());
			for (NamespacedKey key : container.getKeys()) {
				RequirementType<?> type = requirementRegistry.getType(key);
				if (type == null) {
					ShardCore.getPLogger().warning("[EquipmentReqModule] アイテムに保存されている使用条件 '" + key.getKey() + "' がRegistryに存在しません。この項目はスキップされます");
					continue;
				}
				container.get(type.getContainerKey());
				itemRequirements.add(createItemRequirement(container, key));
			}

			context.put(RequirementsKeys.CTX_REQUIREMENTS_KEY, itemRequirements.stream().sorted().toList());
		}

		@Override
		public void update(UpdateContext<? extends EquipmentItem> context) {
			List<ItemRequirement<?>> finalReqs = requirementApplier(
				() -> context.get(RequirementsKeys.CTX_REQUIREMENTS_KEY, List.of()),
				context.getUParams().get(RequirementsKeys.UDT_SET_REQUIREMENTS),
				context.getUParams().get(RequirementsKeys.UDT_ADD_REQUIREMENTS),
				context.getUParams().get(RequirementsKeys.UDT_REMOVE_REQUIREMENT_KEYNAMES)
			);

			context.put(RequirementsKeys.CTX_REQUIREMENTS_KEY, finalReqs);

			// 装備にデフォである条件とか関係無しに、すべて指定しちゃう
			// こうしないと処理がめちゃめんどくさいことになる
			context.editGParams(p -> p.put(RequirementsKeys.GEN_SET_REQUIREMENTS, finalReqs));
		}

		// データ作成

		@Override
		public void calculate(RandomGenerator random, CalculationContext<? extends EquipmentItem> context) {
			List<ItemRequirement<?>> itemRequirements = requirementApplier(
				() -> context.getData().getRequirementDefs().stream().flatMap(reqDef -> {
					// ここでmanagerからclassをもとにインスタンスを手に入れる reqDef.getResolverType()
					var resolver = requirementRegistry.getResolver(reqDef.getResolverType());
					if (resolver == null) {
						throw new IllegalStateException("[EquipmentReqModule] 使用条件 '" + reqDef.getResolverType().getName() + "' がManagerから見つかりません");
					}
					return getRequirements(random, resolver, context, reqDef).stream();
				}).toList(),
				context.getGParams().get(RequirementsKeys.GEN_SET_REQUIREMENTS),
				context.getGParams().get(RequirementsKeys.GEN_ADD_REQUIREMENTS),
				context.getGParams().get(RequirementsKeys.GEN_REMOVE_REQUIREMENT_KEYNAMES)
			);

			context.put(RequirementsKeys.CTX_REQUIREMENTS_KEY, itemRequirements);
		}


		@Override
		public void process(GenerationContext<? extends EquipmentItem> context) {
			List<ItemRequirement<?>> itemRequirements = context.get(RequirementsKeys.CTX_REQUIREMENTS_KEY);
			if (itemRequirements == null) {
				throw new IllegalStateException("[EquipmentReqModule] 使用条件についてのデータが存在しません");
			}
			if (itemRequirements.isEmpty()) {
				return;
			}

			RequirementType.editRequirementContainer(context.getItem(), requireContainer -> {
				for (ItemRequirement<?> requirement : itemRequirements) {
					setRequirementValue(requireContainer, requirement);
				}
			});
		}

		@Override
		public void updateLore(StructuredLore structuredLore, GenerationContext<? extends EquipmentItem> context) {
			List<ItemRequirement<?>> itemRequirements = context.get(RequirementsKeys.CTX_REQUIREMENTS_KEY);
			if (itemRequirements == null) {
				throw new IllegalStateException("[EquipmentReqModule] 使用条件についてのデータが存在しません");
			}
			if (itemRequirements.isEmpty()) {
				return;
			}
			List<Component> lore = structuredLore.getOrCreateSection(LoreSection.EQUIPMENT_REQUIREMENT);
			context.getItem().setItemData(RequirementsKeys.PDC_REQ_LORE_INDEX, (short) lore.size());

			for (ItemRequirement<?> requirement : itemRequirements) {
				lore.add(createRequirementLore(CHECK_EMPTY, requirement));
			}
		}

		@Override
		public int checksum(BaseContext context) {
			List<ItemRequirement<?>> itemRequirements = context.get(RequirementsKeys.CTX_REQUIREMENTS_KEY);
			if (itemRequirements == null) {
				return 0;
			}
			int checksum = 0;
			for (ItemRequirement<?> requirement : itemRequirements) {
				checksum = 31 * checksum + requirement.type().getKeyName().hashCode();
				// valueがEnumだとhashの値が変わってしまう。こうなるとどうしようもないので条件に含められない
				//				checksum = 31 * checksum + requirement.value().hashCode();
			}
			return 0;
		}

		@Override
		public void updateDynamicLore(StructuredLore structuredLore, SuperItemStack item, PlayerCharacterAPI characterAPI, Player player) {
			Short startIndex = item.getItemData(RequirementsKeys.PDC_REQ_LORE_INDEX);
			if (startIndex == null) {
				return;
			}
			List<Component> lore = structuredLore.getSection(LoreSection.EQUIPMENT_REQUIREMENT);
			if (lore == null) {
				return;
			}
			DataContainer requireContainer = RequirementType.getRequireContainer(item);
			for (NamespacedKey key : requireContainer.getKeys().stream()
				.sorted(Comparator.comparing(NamespacedKey::getKey)).toList()) {
				lore.set(startIndex, createDynamicLore(requireContainer, player, characterAPI, key));
				startIndex++;
			}
		}

		/**
		 * できるだけ効率的にRequirementのセット、更新を行います
		 * 削除は先に行われ、その後追加を行います。
		 * 追加において、同じ{@link RequirementType}は許可されません。自動で上書きされます。
		 *
		 * @param defaultReq setがなかった場合のデフォルト値を返すSupplier
		 * @param set        セットする値
		 * @param add        追加する値
		 * @param remove     削除する条件の名前
		 * @return 処理を行い、ソートされた編集不可なList
		 */
		@Unmodifiable
		private List<ItemRequirement<?>> requirementApplier(@NotNull Supplier<@NotNull List<ItemRequirement<?>>> defaultReq, @Nullable List<ItemRequirement<?>> set, @Nullable List<ItemRequirement<?>> add, @Nullable List<String> remove) {
			// add, removeが存在しないから、編集可能なListを作成する必要がない
			if (add == null && remove == null) {
				// setがない場合はdefaultを適応
				if (set == null) {
					set = defaultReq.get();
				}
				return set.stream().sorted().toList();
			}
			// add, removeがあるから編集可能なListを作成して処理
			List<ItemRequirement<?>> editRequirements = new ArrayList<>(set == null ? defaultReq.get() : set);
			if (remove != null) {
				editRequirements.removeIf(r -> remove.contains(r.type().getKeyName()));
			}
			if (add != null) {
				// タイプの重複は許可しないため、削除する
				List<? extends RequirementType<?>> toRemoveTypes = add.stream().map(ItemRequirement::type).toList();
				editRequirements.removeIf(r -> toRemoveTypes.contains(r.type()));

				editRequirements.addAll(add);
			}
			Collections.sort(editRequirements);
			return Collections.unmodifiableList(editRequirements);
		}

		// ---- 生成前のデータ作成 ----
		@SuppressWarnings("unchecked")
		private <D extends RequirementDef<D>> Collection<ItemRequirement<?>> getRequirements(RandomGenerator random, RequirementResolver<D, ?> resolver, CalculationContext<? extends EquipmentItem> ctx, RequirementDef<?> reqDef) {
			if (resolver == null) {
				return Collections.emptyList();
			}
			if (resolver.getContextDataType().isInstance(ctx.getData())) {
				return ((RequirementResolver<D, EquipmentItem>) resolver).resolver(random, (D) reqDef, ctx);
			}
			return Collections.emptyList();
		}

		// ---- 生成時の処理 ----
		private <T> void setRequirementValue(DataContainer requireContainer, ItemRequirement<T> requirement) {
			requireContainer.set(requirement.type().getContainerKey(), requirement.value());
		}

		// ---- 動的Lore変更の処理 ----
		private <T> Component createDynamicLore(DataContainer container, Player player, PlayerCharacterAPI characterAPI, NamespacedKey key) {
			ItemRequirement<T> itemRequirement = createItemRequirement(container, key);
			return createRequirementLore(checkRequirement(itemRequirement.type(), itemRequirement, player, characterAPI),
				itemRequirement);
		}

		@SuppressWarnings("unchecked")
		private <T> ItemRequirement<T> createItemRequirement(DataContainer container, NamespacedKey key) {
			RequirementType<T> requirementType = (RequirementType<T>) requirementRegistry.getType(key);
			return new ItemRequirement<>(
				requirementType,
				container.get(requirementType.getContainerKey())
			);
		}

		private <T> Component createRequirementLore(Component check, ItemRequirement<T> itemRequirement) {
			return ComponentUtil.NOT_ITALIC.append(check).append(itemRequirement.type().getLoreFormat(itemRequirement.value()));
		}

		private <T> Component checkRequirement(RequirementType<T> requirementType, ItemRequirement<T> itemRequirement, Player player, PlayerCharacterAPI characterAPI) {
			return requirementType.check(player, characterAPI, itemRequirement.value()) ? CHECK_ALLOW : CHECK_DENY;
		}
	}
}
