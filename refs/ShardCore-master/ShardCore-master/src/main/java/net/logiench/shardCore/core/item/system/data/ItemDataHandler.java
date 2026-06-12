package net.logiench.shardCore.core.item.system.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import lombok.Getter;
import net.logiench.logienchlibv2.api.minecraft.data.ContainerKey;
import net.logiench.shardCore.ShardCore;
import net.logiench.shardCore.core.item.base.def.ShardItem;
import net.logiench.shardCore.core.item.base.module.ItemModule;
import net.logiench.shardCore.core.item.system.module.params.GenParamsTypeAdapters;
import net.logiench.shardCore.core.item.system.module.params.GenerationParameters;
import net.logiench.shardCore.register.ModuleRegistry;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

@Singleton
public class ItemDataHandler {
	public static final ContainerKey<String, String> ITEM_ID = new ContainerKey<>(PersistentDataType.STRING, ShardCore.getInstance(), "id");
	public static final ContainerKey<String, String> ITEM_PARAMS = new ContainerKey<>(PersistentDataType.STRING, ShardCore.getInstance(), "gen_params");
	public static final ContainerKey<PersistentDataContainer, PersistentDataContainer> ITEM_LORE_INDEXES = new ContainerKey<>(PersistentDataType.TAG_CONTAINER, ShardCore.getInstance(), "lore_indexes");

	@Getter
	private final Gson gson;
	private final ModuleRegistry moduleRegistry;

	@Inject
	private ItemDataHandler(Injector injector, ModuleRegistry moduleRegistry) {
		this.moduleRegistry = moduleRegistry;
		GsonBuilder builder = new GsonBuilder()
			.enableComplexMapKeySerialization();
		GenParamsTypeAdapters.applyGsonBuilder(injector, builder);
		this.gson = builder.create();
	}


	/**
	 * 入力されたアイテムのデータクラスからインスタンス化されたモジュールをすべて取得します
	 *
	 * @param data モジュールをロードするアイテムのデータ
	 * @param <I>  アイテムのデータクラス
	 */
	@SuppressWarnings("unchecked")
	public <I extends ShardItem> List<? extends ItemModule<? super I>> getModules(I data) {
		// Class<ItemModule>のキャストはアイテムのデータを登録する時点でチェック済み
		return data.getModules().stream()
			.map(m -> moduleRegistry.get((Class<? extends ItemModule<? super I>>) m))
			.toList();
	}

	/**
	 * 入力されたアイテムのデータクラスからインスタンス化されたモジュールをすべて取得し、
	 * モジュールから機能をロードするgetterの返り値がnullではなかった場合にrunnerを実行します。
	 *
	 * @param data   モジュールをロードするアイテムのデータ
	 * @param getter モジュールから機能を取り出す関数。返り値がnullではない場合はrunnerを実行する。
	 * @param runner getterの返り値がnullではない場合にその値を受け取り実行する関数
	 * @param <I>    アイテムのデータクラス
	 * @param <M>    getterが取得する型
	 */
	public <I extends ShardItem, M> void runModules(I data, Function<ItemModule<? super I>, M> getter, Consumer<@NotNull M> runner) {
		runModules(getModules(data), getter, runner);
	}

	/**
	 * 入力されたアイテムのデータクラスからインスタンス化されたモジュールをすべて取得し、
	 * モジュールから機能をロードするgetterの返り値がnullではなかった場合に、
	 * 対象のモジュールとロードされた機能を受け取るrunnerを実行します。
	 *
	 * @param data   モジュールをロードするアイテムのデータ
	 * @param getter モジュールから機能を取り出す関数。返り値がnullではない場合はrunnerを実行する。
	 * @param runner getterの返り値がnullではない場合に、対象のモジュールとその値を受け取り実行する関数
	 * @param <I>    アイテムのデータクラス
	 * @param <M>    getterが取得する型
	 */
	public <I extends ShardItem, M> void runModules(I data, Function<ItemModule<? super I>, M> getter, BiConsumer<ItemModule<? super I>, @NotNull M> runner) {
		runModules(getModules(data), getter, runner);
	}

	/**
	 * 入力されたモジュールのリストから、モジュールごとに機能をロードするgetterの返り値がnullではなかった場合にrunnerを実行します。
	 *
	 * @param modules ロード済みのモジュールリスト
	 * @param getter  モジュールから機能を取り出す関数。返り値がnullではない場合はrunnerを実行する。
	 * @param runner  getterの返り値がnullではない場合にその値を受け取り実行する関数
	 * @param <I>     アイテムのデータクラス
	 * @param <M>     getterが取得する型
	 */
	public <I extends ShardItem, M> void runModules(List<? extends ItemModule<? super I>> modules, Function<ItemModule<? super I>, M> getter, Consumer<@NotNull M> runner) {
		runModules(modules, getter, (a, b) -> runner.accept(b));
	}

	/**
	 * 入力されたモジュールのリストから、モジュールごとに機能をロードするgetterの返り値がnullではなかった場合に、
	 * 対象のモジュールとロードされた機能を受け取るrunnerを実行します。
	 *
	 * @param modules ロード済みのモジュールリスト
	 * @param getter  モジュールから機能を取り出す関数。返り値がnullではない場合はrunnerを実行する。
	 * @param runner  getterの返り値がnullではない場合に、対象のモジュールとその値を受け取り実行する関数
	 * @param <I>     アイテムのデータクラス
	 * @param <M>     getterが取得する型
	 */
	public <I extends ShardItem, M> void runModules(List<? extends ItemModule<? super I>> modules, Function<ItemModule<? super I>, M> getter, BiConsumer<ItemModule<? super I>, @NotNull M> runner) {
		for (ItemModule<? super I> module : modules) {
			M result = getter.apply(module);
			if (result != null) {
				runner.accept(module, result);
			}
		}
	}

	public String serializeParams(GenerationParameters params) {
		return gson.toJson(params);
	}

	@Contract("null -> null")
	public GenerationParameters deserializeParams(String json) {
		return gson.fromJson(json, GenerationParameters.class);
	}
}
