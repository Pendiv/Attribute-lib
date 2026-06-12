package net.logiench.shardLib.core.mob;

import net.logiench.shardLib.ShardLib;
import org.bukkit.NamespacedKey;

public interface MobDataKey {
	NamespacedKey ATTRIBUTE_ID = new NamespacedKey(ShardLib.getInstance(), "attribute_id");
	NamespacedKey STATS = new NamespacedKey(ShardLib.getInstance(), "stats");
	NamespacedKey MODIFIERS = new NamespacedKey(ShardLib.getInstance(), "modifiers");
}
