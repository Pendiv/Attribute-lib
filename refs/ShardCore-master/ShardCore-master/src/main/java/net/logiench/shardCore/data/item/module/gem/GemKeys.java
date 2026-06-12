package net.logiench.shardCore.data.item.module.gem;

import com.google.gson.reflect.TypeToken;
import net.logiench.logienchlibv2.api.minecraft.data.ContainerKey;
import net.logiench.shardCore.ShardCore;
import net.logiench.shardCore.core.item.base.def.EquipmentItem;
import net.logiench.shardCore.core.item.base.def.GemItem;
import net.logiench.shardCore.core.item.system.module.context.ContextKey;
import net.logiench.shardCore.core.item.system.module.params.GenParamKey;
import net.logiench.shardCore.core.item.system.module.params.UpdateParamKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class GemKeys {

	public static final ContextKey<List<GemItem>> CTX_GEM_DATA = new ContextKey<>("gem_data");
	public static final ContextKey<Integer> CTX_GEM_SLOT_SIZE = new ContextKey<>("gem_slot_size");

	/**
	 * 指定したジェムがアイテムに適応されます。
	 * lengthが{@link EquipmentItem#getGemSlotSize()}を超えるとエラーになります。
	 * スロットよりも数が少ない場合はそこは空きスロットになります。
	 */
	public static final GenParamKey<List<GemItem>> GEN_DEFAULT_GEM = new GenParamKey<>("default_gem", new TypeToken<>() {});

	public static final UpdateParamKey<List<GemItem>> UDT_ADD_GEM = new UpdateParamKey<>("add_gem", new TypeToken<>() {});
	public static final UpdateParamKey<List<GemItem>> UDT_REMOVE_GEM = new UpdateParamKey<>("remove_gem", new TypeToken<>() {});
	public static final UpdateParamKey<List<GemItem>> UDT_SET_GEM = new UpdateParamKey<>("set_gem", new TypeToken<>() {});

	static final ContainerKey<PersistentDataContainer, PersistentDataContainer> PDC_GEM_DATA =
		new ContainerKey<>(PersistentDataType.TAG_CONTAINER, ShardCore.getInstance(), "gem");
	static final ContainerKey<Integer, Integer> PDC_GEM_SLOT_SIZE =
		new ContainerKey<>(PersistentDataType.INTEGER, ShardCore.getInstance(), "gem_slot_size");
}
