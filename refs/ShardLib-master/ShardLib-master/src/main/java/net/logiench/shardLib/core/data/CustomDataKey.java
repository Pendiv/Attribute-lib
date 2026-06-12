package net.logiench.shardLib.core.data;

import net.logiench.logienchlibv2.api.minecraft.data.ContainerKey;
import net.logiench.shardLib.ShardLib;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

public interface CustomDataKey {
	NamespacedKey CUSTOM_DATA = new NamespacedKey(ShardLib.getInstance(), "data");
	ContainerKey<String, String> CUSTOM_DATA2 = new ContainerKey<>(PersistentDataType.STRING, ShardLib.getInstance(), "data");
}
