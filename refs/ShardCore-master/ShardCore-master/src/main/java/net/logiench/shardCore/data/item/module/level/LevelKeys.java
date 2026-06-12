package net.logiench.shardCore.data.item.module.level;

import net.logiench.logienchlibv2.api.minecraft.data.ContainerKey;
import net.logiench.shardCore.ShardCore;
import net.logiench.shardCore.core.item.system.module.context.ContextKey;
import net.logiench.shardCore.core.item.system.module.params.GenParamKey;
import net.logiench.shardCore.core.item.system.module.params.UpdateParamKey;
import org.bukkit.persistence.PersistentDataType;

public class LevelKeys {
	public static final ContextKey<Long> CTX_LEVEL = new ContextKey<>("level");
	public static final GenParamKey<Long> GEN_LEVEL = new GenParamKey<>("level", Long.class);

	static final ContainerKey<Long, Long> PDC_LEVEL = new ContainerKey<>(PersistentDataType.LONG, ShardCore.getInstance(), "level");

	public static final UpdateParamKey<Long> UDT_ADD_LEVEL = new UpdateParamKey<>("add_level", Long.class);
	public static final UpdateParamKey<Long> UDT_SET_LEVEL = new UpdateParamKey<>("set_level", Long.class);
}
