package net.logiench.logienchlibv2.api.cache.key.player;

import net.logiench.logienchlibv2.base.cache.KeyOriginal;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public final class MapKey<K, V> extends KeyOriginal<V> {
	private final Class<V> vClazz;

	public MapKey(@NotNull NamespacedKey key, @NotNull Class<K> kClass, @NotNull Class<V> vClazz) {
		super(key, Type.Map, vClazz);
		this.vClazz = vClazz;
	}

	public MapKey(@NotNull JavaPlugin plugin, @NotNull String key, @NotNull Class<K> kClass, @NotNull Class<V> vClazz) {
		super(plugin, key, Type.Map, vClazz);
		this.vClazz = vClazz;
	}

	@Override
	public boolean equals(Object obj) {
		boolean result = super.equals(obj);
		if (result && obj instanceof MapKey<?, ?> other) {
			return other.vClazz.equals((this.vClazz));
		}
		return false;
	}
}
