package net.logiench.shardCore.data.item.module.requirement;

import com.google.gson.reflect.TypeToken;
import net.logiench.logienchlibv2.api.minecraft.data.ContainerKey;
import net.logiench.shardCore.ShardCore;
import net.logiench.shardCore.core.item.system.module.context.ContextKey;
import net.logiench.shardCore.core.item.system.module.params.GenParamKey;
import net.logiench.shardCore.core.item.system.module.params.UpdateParamKey;
import net.logiench.shardCore.core.itemRequirement.base.ItemRequirement;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class RequirementsKeys {
	public static final ContextKey<List<ItemRequirement<?>>> CTX_REQUIREMENTS_KEY = new ContextKey<>("requirements");

	static final ContainerKey<Short, Short> PDC_REQ_LORE_INDEX =
		new ContainerKey<>(PersistentDataType.SHORT, ShardCore.getInstance(), "req_lore_index");

	public static final GenParamKey<List<ItemRequirement<?>>> GEN_SET_REQUIREMENTS = new GenParamKey<>("set_requirements", new TypeToken<>() {});
	public static final GenParamKey<List<ItemRequirement<?>>> GEN_ADD_REQUIREMENTS = new GenParamKey<>("add_requirements", new TypeToken<>() {});
	public static final GenParamKey<List<String>> GEN_REMOVE_REQUIREMENT_KEYNAMES = new GenParamKey<>("remove_requirement_keynames", new TypeToken<>() {});

	public static final UpdateParamKey<List<ItemRequirement<?>>> UDT_SET_REQUIREMENTS = new UpdateParamKey<>("set_requirements", new TypeToken<>() {});
	public static final UpdateParamKey<List<ItemRequirement<?>>> UDT_ADD_REQUIREMENTS = new UpdateParamKey<>("add_requirements", new TypeToken<>() {});
	public static final UpdateParamKey<List<String>> UDT_REMOVE_REQUIREMENT_KEYNAMES = new UpdateParamKey<>("remove_requirement_keynames", new TypeToken<>() {});
}
