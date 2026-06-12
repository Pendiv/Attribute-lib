package net.logiench.logienchlibv2.base.cache;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class KeyOriginal<T> {
	public final NamespacedKey key;
	public final Type type;
	public final Class<T> clazz;

	protected KeyOriginal(@NotNull NamespacedKey key, @NotNull KeyOriginal.Type type, @NotNull Class<T> clazz) {
		this.key = key;
		this.type = type;
		this.clazz = clazz;
	}

	protected KeyOriginal(@NotNull JavaPlugin plugin, @NotNull String key, @NotNull KeyOriginal.Type type, @NotNull Class<T> clazz) {
		this(new NamespacedKey(plugin, key), type, clazz);
	}

	public enum Type {
		Server,
		Single,
		List,
		Map
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof KeyOriginal<?> other) {
			return other.key.equals(this.key) && other.type.equals(this.type) && other.clazz.equals(this.clazz);
		}
		return super.equals(obj);
	}
}
