package net.logiench.logienchlibv2.api.minecraft.data;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * アイテムの{@link NamespacedKey}と{@link PersistentDataType}をつなげた、ミスを減らすためのレコードクラスです
 *
 * @param type ItemDataで使用される型
 * @param key  ItemDataで使うためのキー
 * @param <T>  {@link PersistentDataType} の第1ジェネリクス型
 * @param <Z>  {@link PersistentDataType} の第2ジェネリクス型
 */
public record ContainerKey<T, Z>(@NotNull PersistentDataType<T, Z> type, @NotNull NamespacedKey key) {
	/**
	 * @param type   ItemDataで使用される型
	 * @param plugin 自身のプラグイン
	 * @param key    キー
	 */
	public ContainerKey(@NotNull PersistentDataType<T, Z> type, @NotNull Plugin plugin, @NotNull String key) {
		this(type, new NamespacedKey(plugin, key));
	}
}
