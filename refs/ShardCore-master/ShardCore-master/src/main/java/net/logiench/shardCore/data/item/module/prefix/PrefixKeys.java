package net.logiench.shardCore.data.item.module.prefix;

import net.logiench.logienchlibv2.api.minecraft.data.ContainerKey;
import net.logiench.shardCore.ShardCore;
import net.logiench.shardCore.core.item.system.module.context.ContextKey;
import net.logiench.shardCore.core.item.system.module.params.GenParamKey;
import net.logiench.shardCore.core.item.system.module.params.UpdateParamKey;
import net.logiench.shardCore.data.prefix.Prefix;
import org.bukkit.persistence.PersistentDataType;

public class PrefixKeys {
	public static final ContextKey<Prefix> CTX_PREFIX = new ContextKey<>("prefix");

	static final ContainerKey<String, String> PDC_PREFIX = new ContainerKey<>(PersistentDataType.STRING, ShardCore.getInstance(), "prefix");

	public static final GenParamKey<Prefix> GEN_SET_PREFIX = new GenParamKey<>("set_prefix", Prefix.class);

	public static final UpdateParamKey<Prefix> UDT_SET_PREFIX = new UpdateParamKey<>("set_prefix", Prefix.class);
}
