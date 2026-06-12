package net.logiench.logienchlibv2.api.cache.master.player;

import net.logiench.logienchlibv2.api.cache.key.player.SingleKey;
import net.logiench.logienchlibv2.base.cache.CacheManage;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("unused")
public final class SingleData extends CacheManage {

	@SuppressWarnings("unchecked")
	public static <T> T put(UUID uuid, SingleKey<T> key, T value) {
		return (T) playerDataSingle.computeIfAbsent(uuid, k -> new HashMap<>()).put(key, value);
	}

	public static <T> T put(Player player, SingleKey<T> key, T value) {
		return put(player.getUniqueId(), key, value);
	}

	public static boolean hasNotKey(UUID uuid, SingleKey<?> key) {
		return !hasKey(uuid, key);
	}

	public static boolean hasNotKey(Player player, SingleKey<?> key) {
		return hasNotKey(player.getUniqueId(), key);
	}

	public static boolean hasKey(UUID uuid, SingleKey<?> key) {
		return getMapOrEmpty(uuid).containsKey(key);
	}

	public static boolean hasKey(Player player, SingleKey<?> key) {
		return hasKey(player.getUniqueId(), key);
	}

	public static boolean hasValue(UUID uuid, Object value) {
		return getMapOrEmpty(uuid).containsValue(value);
	}

	public static boolean hasValue(Player player, Object value) {
		return hasValue(player.getUniqueId(), value);
	}

	@SuppressWarnings("unchecked")
	public static <T> T get(UUID uuid, SingleKey<T> key) {
		return (T) getMapOrEmpty(uuid).get(key);
	}

	public static <T> T get(Player player, SingleKey<T> key) {
		return get(player.getUniqueId(), key);
	}

	@SuppressWarnings("unchecked")
	public static <T> T getOrDefault(UUID uuid, SingleKey<T> key, T defaultValue) {
		return (T) getMapOrEmpty(uuid).getOrDefault(key, defaultValue);
	}

	public static <T> T getOrDefault(Player player, SingleKey<T> key, T defaultValue) {
		return getOrDefault(player.getUniqueId(), key, defaultValue);
	}

	@SuppressWarnings("unchecked")
	public static <T> T remove(UUID uuid, SingleKey<T> key) {
		return (T) getMapOrEmpty(uuid).remove(key);
	}

	public static <T> T remove(Player player, SingleKey<T> key) {
		return remove(player.getUniqueId(), key);
	}

	private static Map<SingleKey<?>, Object> getMapOrEmpty(UUID uuid) {
		return playerDataSingle.getOrDefault(uuid, Collections.emptyMap());
	}
}
