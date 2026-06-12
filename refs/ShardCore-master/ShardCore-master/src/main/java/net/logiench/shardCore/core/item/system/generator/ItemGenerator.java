package net.logiench.shardCore.core.item.system.generator;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.logiench.logienchlibv2.api.minecraft.data.DataContainer;
import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import net.logiench.logienchlibv2.api.minecraft.text.ChatColor;
import net.logiench.logienchlibv2.api.minecraft.text.ComponentUtil;
import net.logiench.logienchlibv2.api.minecraft.text.LoreList;
import net.logiench.shardCore.core.item.base.def.EquipmentItem;
import net.logiench.shardCore.core.item.base.def.ShardItem;
import net.logiench.shardCore.core.item.base.module.ItemModule;
import net.logiench.shardCore.core.item.base.module.tools.LoreSection;
import net.logiench.shardCore.core.item.base.module.tools.StructuredLore;
import net.logiench.shardCore.core.item.system.data.ItemDataHandler;
import net.logiench.shardCore.core.item.system.loader.ItemInspector;
import net.logiench.shardCore.core.item.system.loader.ItemLoader;
import net.logiench.shardCore.core.item.system.module.context.Context;
import net.logiench.shardCore.core.item.system.module.context.InspectContext;
import net.logiench.shardCore.core.item.system.module.context.UpdateContextImpl;
import net.logiench.shardCore.core.item.system.module.context.data.EquipmentData;
import net.logiench.shardCore.core.item.system.module.params.GenerationParameters;
import net.logiench.shardCore.core.item.system.module.params.UpdateParameters;
import net.logiench.shardCore.core.player.system.PlayerCharacter;
import net.logiench.shardCore.register.ItemRegistry;
import net.logiench.shardLib.api.player.PlayerCharacterAPI;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

@Singleton
public class ItemGenerator {

	private static final Component SPLIT_LINE = Component.text(" ".repeat(20), ChatColor.DARK_GRAY, TextDecoration.STRIKETHROUGH);

	private final ItemRegistry itemRegistry;
	private final ItemDataHandler handler;
	private final ItemInspector inspector;

	@Inject
	private ItemGenerator(ItemRegistry itemRegistry, ItemDataHandler handler, ItemInspector inspector) {
		this.itemRegistry = itemRegistry;
		this.handler = handler;
		this.inspector = inspector;
	}

	/**
	 * 見た目だけの軽量なアイテムを作成します。
	 * ステータス計算、Lore生成、PDC書き込みをスキップし、
	 * クライアント表示に必要な情報（名前、モデル）のみを適用します。
	 */
	public <I extends ShardItem> SuperItemStack generateVisual(@NotNull I data, @Nullable GenerationParameters parameters) {
		GenerationParameters generationParameters = GenerationParameters.ofImmutable(parameters);
		SuperItemStack item = data.createItemStack();

		setCustomModel(item, data, generationParameters);
		return item.name(data.getName());// Prefixなどは完全無視
	}

	/**
	 * 未確定のアイテムを作成します。鑑定{@link #appraise(SuperItemStack, GenerationParameters)}を行うことでこのアイテムを元に新規アイテムの生成ができます。
	 */
	public <I extends ShardItem> ItemGenerationResult<I> generateUnidentified(@NotNull I data, @Nullable GenerationParameters parameters) {
		// 指定されたパラメータをコピー & nullを削除
		GenerationParameters generationParameters = GenerationParameters.of(parameters);

		long seed = getSeedAndSet(generationParameters);

		// コピーしたパラメータを編集不可にする
		generationParameters.setImmutable();

		// アイテムを新規作成し、id、データ、モデルを付与。
		// fixme ここのハードコードは変えるべき？
		SuperItemStack item = SuperItemStack.init(Material.CHEST)
			.name(ComponentUtil.NOT_ITALIC.append(data.getRarity().getComponent().append(Component.text("クレート"))))
			.setItemData(ItemDataHandler.ITEM_ID, data.getId());

		setCustomModel(item, data, generationParameters);

		// アイテムにシードを含む情報をすべて保存
		item.setItemData(ItemDataHandler.ITEM_PARAMS, handler.serializeParams(generationParameters));

		List<? extends ItemModule<? super I>> modules = handler.getModules(data);

		// 事前計算を行う
		try {
			// 鑑定前のアイテムは更新で作成されることはありえないからUpdateParametersはnull
			Context<I> context = new Context<>(data.createItemStack(), data, generationParameters);

			handler.runModules(modules, ItemModule::getCalculator, (m, c) ->
				c.calculate(getModuleRandom(m, seed), context));

			// fixme 未鑑定状態でアイテムのLoreを動的に更新するならここも StructuredLore にする必要がある
			LoreList lore = LoreList.create();
			handler.runModules(modules, ItemModule::getUnidentifiedLore, c -> c.updateUnidentifiedLore(lore, context));

			if (!lore.isEmpty()) {
				item.lore(lore);
			}

			return new ItemGenerationResult<>(ItemGenerationResult.State.SUCCESS, item, context);
		} catch (Exception e) {
			return new ItemGenerationResult<>(ItemGenerationResult.State.ERROR, e);
		}
	}

	// --------------------    generate    --------------------

	/**
	 * 鑑定を行います。鑑定はアイテムに保存されたシードをもとに行うため、必ず同じアイテムが生成されます。
	 *
	 * @param item       鑑定する対象のアイテム。このアイテムは参照されるのみで編集されません。
	 * @param parameters 指定することで対象アイテムから取得したパラメータの一部を上書きします。
	 * @return 新規作成されたアイテム、もしくは生成失敗のメッセージ。
	 */
	@Contract(pure = true)
	public ItemGenerationResult<?> appraise(@Nullable SuperItemStack item, @Nullable GenerationParameters parameters) {
		ItemLoader loader = ItemLoader.of(item);
		ParsedItemInfo info = parseItemInfo(loader);
		if (info.isError()) {
			return info.error();
		}
		GenerationParameters itemParameters = inspector.getGenParams(loader);
		if (itemParameters == null) {
			return new ItemGenerationResult<>(ItemGenerationResult.State.INVALID_PARAMS, "このアイテムには鑑定するための情報がありません");
		}
		if (parameters != null) {
			itemParameters.merge(parameters);
		}
		return generate(null, info.data(), itemParameters, null);
	}

	/**
	 * 新規アイテムの生成を行います
	 *
	 * @param dataClass  生成するアイテムのデータのクラス。
	 * @param parameters 新規作成をカスタムするためのパラメータ。
	 * @param <I>        アイテムのデータの型。
	 * @return 新規作成されたアイテム、もしくは生成失敗のメッセージ。
	 */
	public <I extends ShardItem> ItemGenerationResult<I> generateNew(@NotNull Class<I> dataClass, @Nullable GenerationParameters parameters) {
		I data = itemRegistry.get(dataClass);
		if (data == null) {
			return new ItemGenerationResult<>(ItemGenerationResult.State.NOT_FOUND, "'%s' はitemRegistryに登録されていないクラスです".formatted(dataClass.getName()));
		}
		return generateNew(data, parameters);
	}

	/**
	 * 新規アイテムの生成を行います
	 *
	 * @param data       生成するアイテムのデータ。
	 * @param parameters 新規作成をカスタムするためのパラメータ。
	 * @param <I>        アイテムのデータの型。
	 * @return 新規作成されたアイテム、もしくは生成失敗のメッセージ。
	 */
	@Contract(pure = true)
	public <I extends ShardItem> ItemGenerationResult<I> generateNew(@NotNull I data, @Nullable GenerationParameters parameters) {
		return generate(null, data, parameters, null);
	}

	/**
	 * 渡されたアイテムを元に、一部を編集したアイテムを作成します
	 *
	 * @param item       更新する元のアイテム。このアイテムは更新されず、新しく作成されたアイテムが返されます。
	 * @param parameters 更新する内容を指定したパラメータ。
	 * @return 指定されたデータを引き継ぎ、更新、新規作成された新規アイテム。もしくは生成失敗のメッセージ。
	 */
	@Contract(pure = true)
	public ItemGenerationResult<?> update(@Nullable SuperItemStack item, @NotNull UpdateParameters parameters) {
		ItemLoader loader = ItemLoader.of(item);
		ParsedItemInfo info = parseItemInfo(loader);
		if (info.isError()) {
			return info.error();
		}
		return generate(loader, info.data(), inspector.getGenParams(loader), parameters);
	}

	// 引数の指定ミスでエラーになるため、外部からは呼び出せないように

	/**
	 * このメソッドの呼び出し時の注意
	 *
	 * @param itemLoader           引き継ぎ元となるアイテム。これがnullの場合は引継ぎはない
	 * @param data                 生成するアイテムの元データ。必ず必要
	 * @param generationParameters 指定されたデータの箇所を新規作成する場合はそれに関しての命令。それ以外の場合はアイテムの設計図を更新後も引き継ぐためのパラメータ。
	 * @param updateParameters     itemLoaderが指定されていないのにこの値を指定すると、一部データの引き継ぎが前提となってしまう場合がある。
	 *                             そうなると一部のデータが生成されないなど、処理が壊れるため、itemLoaderがnullの場合はupdateParametersは必ずnullの必要がある
	 * @param <I>                  データの実際の型
	 * @return アイテムの生成処理の結果
	 */
	@NotNull
	@Contract(pure = true)
	private <I extends ShardItem> ItemGenerationResult<I> generate(@Nullable ItemLoader itemLoader, @NotNull I data, @Nullable GenerationParameters generationParameters, @Nullable UpdateParameters updateParameters) {
		// itemLoaderがnullなのに、それを対象に引き継いで更新する処理を指定するupdateParametersが存在したら、処理中に壊れる
		if (itemLoader == null && updateParameters != null) {
			return new ItemGenerationResult<>(ItemGenerationResult.State.INVALID_PARAMS, "ItemLoaderがnullなのに、それを引き継ぐ処理(UpdateParameters)がnullではありません");
		}
		// シードが存在しない場合は生成してGenParamsにセット
		GenerationParameters gParams = GenerationParameters.of(generationParameters);
		long seed = getSeedAndSet(gParams);
		// 指定されたパラメータはImmutableにするけどそれを外に伝播させないためコピー
		gParams.setImmutable();
		UpdateParameters uParams = UpdateParameters.ofImmutable(updateParameters);

		SuperItemStack generatedItem = data.createItemStack();

		// アイテムにIDを付与
		generatedItem.setItemData(ItemDataHandler.ITEM_ID, data.getId());
		setCustomModel(generatedItem, data, gParams); // todo 将来的に動的にcustomModelを変更したい場合はこれもモジュール化必須

		List<? extends ItemModule<? super I>> modules = handler.getModules(data);

		// ModuleはParametersで異常な値や、想定しない状態になるとエラーを吐くのでtry catchで全て囲う
		try {
			Context<I> context;

			// ベースとなるアイテムがある場合は読み込んでcontextにマージ
			if (itemLoader != null) {
				InspectContext readerContext = new InspectContext();

				// アイテムのデータをロード
				handler.runModules(modules, ItemModule::getReader, (m, r) -> {
					if (uParams.shouldRead(m)) {
						r.read(itemLoader, data, readerContext);
					}
				});

				// ロードしたアイテムデータをcontextに適応
				// GenerationParametersは必ず編集可能な状態の必要があるから .of で変換
				UpdateContextImpl<I> updateContext = new UpdateContextImpl<>(data, GenerationParameters.of(gParams), uParams);

				updateContext.merge(readerContext);

				// アイテムのデータを更新
				handler.runModules(modules, ItemModule::getUpdater, (m, u) -> {
					if (uParams.shouldUpdate(m)) {
						u.update(updateContext);
					}
				});

				// gParamsは更新処理で変わるから引き継ぐ
				context = new Context<>(generatedItem, data,
					GenerationParameters.ofImmutable(updateContext.getGParams()));
				context.merge(updateContext);

				// アイテムのデータで、必要な箇所のみ新規作成
				List<Class<?>> readerIsNullClasses = new ArrayList<>();
				handler.runModules(modules, ItemModule::getCalculator, (m, c) -> {
					if (uParams.shouldCalculate(m)) {
						c.calculate(getModuleRandom(m, seed), context);
						return;
					}
					// calculateしないのにreaderがnullなのはダメなのでエラーに追加
					if (m.getReader() == null) {
						readerIsNullClasses.add(m.getClass());
					}
				});
				// readerの実装にエラーがあったら処理を止める
				if (!readerIsNullClasses.isEmpty()) {
					return new ItemGenerationResult<>(ItemGenerationResult.State.READER_IS_NULL, "ItemReaderがnullのクラスがあります。"
						+ readerIsNullClasses.stream().map(Class::getSimpleName).collect(Collectors.joining("[", ", ", "]")));
				}
			} else {
				context = new Context<>(generatedItem, data, gParams);
				// loaderがnull、つまりすべてを新規作成する必要があるので単純処理
				handler.runModules(modules, ItemModule::getCalculator, (m, c) ->
					c.calculate(getModuleRandom(m, seed), context));
			}

			// アイテムにデータを適応
			handler.runModules(modules, ItemModule::getProcessor, c -> c.process(context));

			// Loreを作成
			StructuredLore lore = new StructuredLore();
			handler.runModules(modules, ItemModule::getLoreProvider, p -> {
				p.updateLore(lore, context);
			});
			// Loreを適応
			setLore(null, lore, generatedItem);

			generatedItem.setItemData(ItemDataHandler.ITEM_PARAMS, handler.serializeParams(context.getGParams()));

			// EquipmentItem以上の場合はステータス情報をアイテムに適応。ただしこれにはEquipmentItemがShardLibを用いて生成されたアイテムを返すということが必須
			if (context.getData() instanceof EquipmentItem) {
				@SuppressWarnings("unchecked")
				Context<EquipmentItem> c = (Context<EquipmentItem>) context;
				EquipmentData.getItemDataBuilder(c).applyData(generatedItem);
			}

			return new ItemGenerationResult<>(ItemGenerationResult.State.SUCCESS, generatedItem, context);
		} catch (Exception e) {
			return new ItemGenerationResult<>(ItemGenerationResult.State.ERROR, "処理中にエラーが発生しました", e);
		}
	}

	public void updateDynamicLore(@NotNull PlayerCharacter character, @Nullable ItemStack item) {
		updateDynamicLore(character, SuperItemStack.safeInit(item));
	}

	public void updateDynamicLore(@NotNull PlayerCharacter character, @Nullable SuperItemStack item) {
		updateDynamicLore(character, ItemLoader.of(item));
	}

	public void updateDynamicLore(@NotNull PlayerCharacter character, @Nullable ItemLoader loader) {
		if (loader == null) {
			return;
		}
		String id = loader.getId();
		SuperItemStack item = loader.getLoadedItem();

		ShardItem data = itemRegistry.get(id);
		if (data == null) {
			return;
		}
		Player player = character.player();
		PlayerCharacterAPI characterAPI = character.characterAPI();
		Map<LoreSection, Integer> beforeChangeIndexes = inspector.getSectionIndexes(loader);
		StructuredLore lore = new StructuredLore(item.notNullLore(), beforeChangeIndexes, true);

		handler.runModules(data, ItemModule::getDynamicUpdater, (m, u) ->
			u.updateDynamicLore(lore, item, characterAPI, player));
		setLore(beforeChangeIndexes, lore, item);
	}

	private void setLore(@Nullable Map<LoreSection, Integer> beforeChangeIndexes, StructuredLore lore, @NotNull SuperItemStack item) {
		List<Component> createdLore = lore.join(SPLIT_LINE);
		if (!createdLore.isEmpty()) {
			item.lore(createdLore);
			Map<LoreSection, Integer> currentIndexes = lore.getSectionIndexes(true);
			if (!currentIndexes.equals(beforeChangeIndexes)) {
				DataContainer container = new DataContainer();
				for (Map.Entry<LoreSection, Integer> entry : currentIndexes.entrySet()) {
					container.set(new NamespacedKey("i", entry.getKey().name().toLowerCase()),
						PersistentDataType.INTEGER, entry.getValue());
				}
				item.setItemData(ItemDataHandler.ITEM_LORE_INDEXES, container.getPDC());
			}
		} else {
			item.removeLore();
			item.removeItemData(ItemDataHandler.ITEM_LORE_INDEXES);
		}
	}

	private long getSeedAndSet(@NotNull GenerationParameters genParams) {
		Long seed = genParams.get(GenerationParameters.SEED);
		if (seed != null) {
			return seed;
		}
		// ランダムな値を生成するためのシードを外部から設定されていなかった場合は設定
		seed = ThreadLocalRandom.current().nextLong();
		genParams.put(GenerationParameters.SEED, seed);

		return seed;
	}

	private void setCustomModel(@NotNull SuperItemStack item, @NotNull ShardItem data, @Nullable GenerationParameters parameters) {
		NamespacedKey modelKey = data.hasCustomModel() ? data.getKey() : null;

		if (parameters != null) {
			NamespacedKey paramModelKey = parameters.get(GenerationParameters.MODEL);
			if (paramModelKey != null) {
				modelKey = paramModelKey;
			}

			CustomModelData.Builder builder = CustomModelData.customModelData();

			// モデルのデータが存在すれば設定する。一つもない場合はset自体をしない
			if (parameters.ifPresent(GenerationParameters.MODEL_COLORS, builder::addColors)
				|| parameters.ifPresent(GenerationParameters.MODEL_FLAGS, builder::addFlags)
				|| parameters.ifPresent(GenerationParameters.MODEL_FLOATS, builder::addFloats)
				|| parameters.ifPresent(GenerationParameters.MODEL_STRINGS, builder::addStrings)
			) {
				item.build().setData(DataComponentTypes.CUSTOM_MODEL_DATA, builder.build());
			}
		}

		if (modelKey != null) {
			NamespacedKey finalModelKey = modelKey;
			item.editItemMeta(meta -> meta.setItemModel(finalModelKey));
		}
	}

	// generatorに渡すための下準備用UtilityMethod
	private ParsedItemInfo parseItemInfo(@Nullable ItemLoader loader) {
		if (loader == null) {
			return new ParsedItemInfo(new ItemGenerationResult<>(ItemGenerationResult.State.INVALID_PARAMS, "ItemLoaderに変換できません"));
		}
		ShardItem data = itemRegistry.get(loader.getId());
		if (data == null) {
			return new ParsedItemInfo(new ItemGenerationResult<>(ItemGenerationResult.State.NOT_FOUND, "アイテムのデータが見つかりません"));
		}
		return new ParsedItemInfo(data);
	}

	private record ParsedItemInfo(ShardItem data, ItemGenerationResult<?> error) {
		private ParsedItemInfo(@NotNull ShardItem data) {
			this(data, null);
		}

		private ParsedItemInfo(@NotNull ItemGenerationResult<?> error) {
			this(null, error);
		}

		boolean isError() {
			return error != null;
		}
	}

	/**
	 * 各モジュールごとに異なる乱数生成器を生成します
	 */
	private static RandomGenerator getModuleRandom(ItemModule<?> module, long seed) {
		return new SplittableRandom(seed ^ hash64(module.getModuleKey()));
	}

	private static long hash64(String s) {
		long h = 0xcbf29ce484222325L;

		for (int i = 0; i < s.length(); i++) {
			h ^= s.charAt(i);
			h *= 0x100000001b3L;
		}
		return h;
	}
}
