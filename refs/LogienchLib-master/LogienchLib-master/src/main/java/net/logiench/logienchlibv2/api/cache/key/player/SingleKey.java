package net.logiench.logienchlibv2.api.cache.key.player;

import net.logiench.logienchlibv2.base.cache.KeyOriginal;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public final class SingleKey<T> extends KeyOriginal<T> {
	public SingleKey(@NotNull NamespacedKey key, @NotNull Class<T> clazz) {
		super(key, Type.Single, clazz);
	}

	public SingleKey(@NotNull JavaPlugin plugin, @NotNull String key, @NotNull Class<T> clazz) {
		super(plugin, key, Type.Single, clazz);
	}
}
