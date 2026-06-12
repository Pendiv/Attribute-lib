package net.logiench.logienchlibv2.api.cache.key.server;

import net.logiench.logienchlibv2.base.cache.KeyOriginal;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public final class ServerKey<T> extends KeyOriginal<T> {
	public ServerKey(@NotNull NamespacedKey key, @Nullable Class<T> clazz) {
		super(key, Type.Server, clazz);
	}

	public ServerKey(@NotNull JavaPlugin plugin, @NotNull String key, @Nullable Class<T> clazz) {
		super(plugin, key, Type.Server, clazz);
	}
}
