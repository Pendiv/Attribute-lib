package net.logiench.logienchlibv2.api.cache.master.player;

import net.logiench.logienchlibv2.api.cache.key.player.ListKey;
import net.logiench.logienchlibv2.base.cache.CacheManage;
import org.bukkit.entity.Player;

import java.util.*;

@SuppressWarnings("unused")
public final class ListData extends CacheManage {
	public static <T> boolean add(UUID uuid, ListKey<T> key, T value) {
		return getMapOrCreate(uuid)
			.computeIfAbsent(key, k -> new ArrayList<>())
			.add(value);
	}

	public static <T> boolean add(Player player, ListKey<T> key, T value) {
		return add(player.getUniqueId(), key, value);
	}

	@SuppressWarnings("unchecked")
	public static <T> List<T> setList(UUID uuid, ListKey<T> key, List<T> list) {
		return (List<T>) getMapOrCreate(uuid).put(key, (List<Object>) list);
	}

	public static <T> List<T> setList(Player player, ListKey<T> key, List<T> list) {
		return setList(player.getUniqueId(), key, list);
	}

	@SuppressWarnings("unchecked")
	public static <T> List<T> getList(UUID uuid, ListKey<T> key) {
		return (List<T>) getMapOrEmpty(uuid).get(key);
	}

	public static <T> List<T> getList(Player player, ListKey<T> key) {
		return getList(player.getUniqueId(), key);
	}

	@SuppressWarnings("unchecked")
	public static <T> List<T> getListOrDefault(UUID uuid, ListKey<T> key, List<T> defaultList) {
		return (List<T>) getMapOrEmpty(uuid)
			.getOrDefault(key, (List<Object>) defaultList);
	}

	public static <T> List<T> getListOrDefault(Player player, ListKey<T> key, List<T> defaultList) {
		return getListOrDefault(player.getUniqueId(), key, defaultList);
	}

	public static <T> boolean hasValue(UUID uuid, ListKey<T> key, T value) {
		return getList(uuid, key).contains(value);
	}

	public static <T> boolean hasValue(Player player, ListKey<T> key, T value) {
		return hasValue(player.getUniqueId(), key, value);
	}

	public static boolean hasKey(UUID uuid, ListKey<?> key) {
		return getMapOrEmpty(uuid).containsKey(key);
	}

	public static boolean hasKey(Player player, ListKey<?> key) {
		return hasKey(player.getUniqueId(), key);
	}


	public static boolean hasNotKey(UUID uuid, ListKey<?> key) {
		return !hasKey(uuid, key);
	}

	public static boolean hasNotKey(Player player, ListKey<?> key) {
		return hasNotKey(player.getUniqueId(), key);
	}

	public static <T> boolean removeValue(UUID uuid, ListKey<T> key, T value) {
		List<T> list = getList(uuid, key);
		if (list == null) {
			return false;
		}
		boolean removed = list.remove(value);
		if (list.isEmpty()) {
			remove(uuid, key);
		}
		return removed;
	}

	public static <T> boolean removeValue(Player player, ListKey<T> key, T value) {
		return removeValue(player.getUniqueId(), key, value);
	}

	@SuppressWarnings("unchecked")
	public static <T> List<T> remove(UUID uuid, ListKey<T> key) {
		return (List<T>) getMapOrEmpty(uuid).remove(key);
	}

	public static <T> List<T> remove(Player player, ListKey<T> key) {
		return remove(player.getUniqueId(), key);
	}

	private static Map<ListKey<?>, List<Object>> getMapOrCreate(UUID uuid) {
		return playerDataList.computeIfAbsent(uuid, k -> new HashMap<>());
	}

	private static Map<ListKey<?>, List<Object>> getMapOrEmpty(UUID uuid) {
		return playerDataList.getOrDefault(uuid, Collections.emptyMap());
	}
}
