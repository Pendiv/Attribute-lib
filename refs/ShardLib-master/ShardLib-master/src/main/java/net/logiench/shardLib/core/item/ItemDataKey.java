package net.logiench.shardLib.core.item;

import net.logiench.logienchlibv2.api.minecraft.data.ContainerKey;
import net.logiench.shardLib.ShardLib;
import org.bukkit.persistence.PersistentDataType;

public interface ItemDataKey {
	ContainerKey<Byte, Boolean> IS_SHARD_ITEM = new ContainerKey<>(PersistentDataType.BOOLEAN, ShardLib.getInstance(), "is_shard");
	ContainerKey<String, String> STATS = new ContainerKey<>(PersistentDataType.STRING, ShardLib.getInstance(), "stats");
}
