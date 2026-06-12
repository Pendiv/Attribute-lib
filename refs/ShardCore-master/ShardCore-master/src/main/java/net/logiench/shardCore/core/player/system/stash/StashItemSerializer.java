package net.logiench.shardCore.core.player.system.stash;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import kotlin.Pair;
import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import net.logiench.shardCore.ShardCore;
import net.logiench.shardCore.core.item.base.def.ShardItem;
import net.logiench.shardCore.core.item.base.module.ItemModule;
import net.logiench.shardCore.core.item.system.data.ItemDataHandler;
import net.logiench.shardCore.core.item.system.data.ItemSerializer;
import net.logiench.shardCore.core.item.system.data.SerializedItemData;
import net.logiench.shardCore.core.item.system.generator.ItemGenerationResult;
import net.logiench.shardCore.core.item.system.loader.ItemInspector;
import net.logiench.shardCore.core.item.system.loader.ItemLoader;
import net.logiench.shardCore.core.item.system.module.context.BaseContext;
import net.logiench.shardCore.core.item.system.module.context.Context;
import net.logiench.shardCore.db.service.StashItemData;
import net.logiench.shardCore.register.ItemRegistry;
import net.logiench.shardLib.api.ShardLibProvider;
import net.logiench.shardLib.api.item.ItemAPI;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

@Singleton
public class StashItemSerializer {

	private static final ItemAPI ITEM_API = ShardLibProvider.get().getItemAPI();

	private final ItemSerializer serializer;
	private final ItemRegistry registry;
	private final ItemInspector inspector;
	private final ItemDataHandler handler;

	@Inject
	private StashItemSerializer(ItemSerializer serializer, ItemRegistry registry, ItemInspector inspector, ItemDataHandler handler) {
		this.serializer = serializer;
		this.registry = registry;
		this.inspector = inspector;
		this.handler = handler;
	}

	@Nullable
	@Contract("null -> null")
	public SuperItemStack deserialize(@Nullable StashItemData data) {
		if (data == null) {
			return null;
		}
		ItemGenerationResult<?> result = serializer.deserialize(data.getItemData(), data.getItemDataJson());
		// ここで使う値はDBから来るもので、失敗は異常なのでログやDiscordへの通知を行う
		if (result == null) {
			ShardCore.getPLogger().warning("StashItemDeserializer: result == null");
			return null;
		}
		Pair<SuperItemStack, ? extends Context<?>> resultPair = result.resultPair();
		if (resultPair == null) {
			ShardCore.getPLogger().warning("StashItemDeserializer: item == null");
			return null;
		}
		SuperItemStack item = resultPair.getFirst();
		if (generateChecksum(data.getItemData(), item, resultPair.getSecond()) != data.getItemChecksum()) {
			ShardCore.getPLogger().warning("StashItemDeserializer: checksum が一致しません");
			return null;
		}
		return item;
	}

	@Nullable
	@Contract("null, _ -> null")
	public StashItemData serialize(@Nullable ItemLoader loader, int amount) {
		if (loader == null) {
			return null;
		}
		ShardItem itemData = registry.get(loader.getId());
		if (itemData == null) {
			return null;
		}
		int checksum = generateChecksum(itemData, loader);
		SerializedItemData serialized = serializer.serialize(loader);
		if (serialized == null) {
			return StashItemData.createNew(itemData, amount, checksum);
		}
		return StashItemData.createNew(itemData, amount, serialized.genParamsJson(), checksum);
	}


	/**
	 * 入力されたアイテムをハッシュ化します。再起動してもハッシュは同じものになります。
	 * 復元したアイテムが前回と同じものかの判定に使用します。
	 *
	 * @param data   生成するアイテムの元となったデータ
	 * @param loader ハッシュを生成するアイテム
	 * @return 生成されたハッシュ
	 */
	private int generateChecksum(ShardItem data, @NotNull ItemLoader loader) {
		return generateChecksum(data, loader.getLoadedItem(), inspector.inspect(loader));
	}

	private int generateChecksum(@NotNull ShardItem data, @NotNull SuperItemStack item, @NotNull BaseContext context) {
		int checksum = item.getType().getKey().toString().hashCode();

		if (item.getItemMeta().hasCustomName()) {
			checksum = 31 * checksum + item.getName().hashCode();
		}

		AtomicInteger atomicChecksum = new AtomicInteger(0);
		handler.runModules(data, ItemModule::getProcessor, p -> {
			atomicChecksum.set(31 * atomicChecksum.get() + p.checksum(context));
		});
		checksum = 31 * checksum + atomicChecksum.get();

		return checksum;
	}
}
