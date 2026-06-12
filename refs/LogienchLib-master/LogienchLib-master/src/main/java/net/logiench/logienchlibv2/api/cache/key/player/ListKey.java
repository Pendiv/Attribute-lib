package net.logiench.logienchlibv2.api.cache.key.player;

import net.logiench.logienchlibv2.base.cache.KeyOriginal;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public final class ListKey<T> extends KeyOriginal<T> {
	public ListKey(@NotNull NamespacedKey key, @NotNull Class<T> clazz) {
		super(key, Type.List, clazz);
	}

	public ListKey(@NotNull JavaPlugin plugin, @NotNull String key, @NotNull Class<T> clazz) {
		super(plugin, key, Type.List, clazz);
	}
}
