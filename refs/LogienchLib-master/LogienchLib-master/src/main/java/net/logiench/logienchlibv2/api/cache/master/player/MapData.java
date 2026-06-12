package net.logiench.logienchlibv2.api.cache.master.player;

import net.logiench.logienchlibv2.api.cache.key.player.MapKey;
import net.logiench.logienchlibv2.base.cache.CacheManage;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@SuppressWarnings("unused")
public final class MapData extends CacheManage {

	@SuppressWarnings("unchecked")
	public static <K, V> Map<K, V> setMap(UUID uuid, MapKey<K, V> mKey, Map<K, V> map) {
		return (Map<K, V>) getMapMapOrCreate(uuid).put(mKey, (Map<Object, Object>) map);
	}

	public static <K, V> Map<K, V> setMap(Player player, MapKey<K, V> mKey, Map<K, V> map) {
		return setMap(player.getUniqueId(), mKey, map);
	}

	@SuppressWarnings("unchecked")
	public static <K, V> V put(UUID uuid, MapKey<K, V> mKey, K key, V value) {
		return (V) getMapMapOrCreate(uuid).computeIfAbsent(mKey, k -> new HashMap<>()).put(key, value);
	}

	public static <K, V> V put(Player player, MapKey<K, V> mKey, K key, V value) {
		return put(player.getUniqueId(), mKey, key, value);
	}

	public static <K, V> void putAll(UUID uuid, MapKey<K, V> mKey, Map<K, V> map) {
		getMapMapOrCreate(uuid).computeIfAbsent(mKey, k -> new HashMap<>()).putAll(map);
	}

	public static <K, V> void putAll(Player player, MapKey<K, V> mKey, Map<K, V> map) {
		putAll(player.getUniqueId(), mKey, map);
	}

	@SuppressWarnings("unchecked")
	public static <K, V> Map<K, V> getMap(UUID uuid, MapKey<K, V> key) {//computeIfAbsent(key, k -> new HashMap<>())
		return (Map<K, V>) getMapMapOrEmpty(uuid).get(key);
	}

	public static <K, V> Map<K, V> getMap(Player player, MapKey<K, V> key) {
		return getMap(player.getUniqueId(), key);
	}

	@SuppressWarnings("unchecked")
	public static <K, V> Map<K, V> getMapOrDefault(UUID uuid, MapKey<K, V> key) {//computeIfAbsent(key, k -> new HashMap<>())
		return (Map<K, V>) getMapMapOrEmpty(uuid).get(key);
	}

	public static <K, V> Map<K, V> getMapOrDefault(Player player, MapKey<K, V> key) {
		return getMapOrDefault(player.getUniqueId(), key);
	}

	@SuppressWarnings("unchecked")
	public static <K, V> V get(UUID uuid, MapKey<K, V> mKey, K key) {
		return (V) getMapMapOrEmpty(uuid).getOrDefault(mKey, Collections.emptyMap()).get(key);
	}

	public static <K, V> V get(Player player, MapKey<K, V> mKey, K key) {
		return get(player.getUniqueId(), mKey, key);
	}

	@SuppressWarnings("unchecked")
	public static <K, V> V getOrDefault(UUID uuid, MapKey<K, V> mKey, K key, V defaultValue) {
		return (V) getMapMapOrEmpty(uuid).getOrDefault(mKey, Collections.emptyMap()).getOrDefault(key, defaultValue);
	}

	public static <K, V> V getOrDefault(Player player, MapKey<K, V> mKey, K key, V defaultValue) {
		return getOrDefault(player.getUniqueId(), mKey, key, defaultValue);
	}

	@NotNull
	@SuppressWarnings("unchecked")
	public static <K, V> Collection<V> getValues(UUID uuid, MapKey<K, V> mKey) {
		return (Collection<V>) getMapMapOrEmpty(uuid).getOrDefault(mKey, Collections.emptyMap()).values();
	}

	@NotNull
	public static <K, V> Collection<V> getValues(Player player, MapKey<K, V> mKey) {
		return getValues(player.getUniqueId(), mKey);
	}

	@NotNull
	@SuppressWarnings("unchecked")
	public static <K> Set<K> getKeys(UUID uuid, MapKey<K, ?> key) {
		return (Set<K>) getMapMapOrEmpty(uuid).getOrDefault(key, Collections.emptyMap()).keySet();
	}

	@NotNull
	public static <K> Set<K> getKeys(Player player, MapKey<K, ?> key) {
		return getKeys(player.getUniqueId(), key);
	}

	public static boolean hasMap(UUID uuid, MapKey<?, ?> key) {
		return getMapMapOrEmpty(uuid).containsKey(key);
	}

	public static boolean hasMap(Player player, MapKey<?, ?> key) {
		return hasMap(player.getUniqueId(), key);
	}

	public static boolean hasNotMap(UUID uuid, MapKey<?, ?> key) {
		return !hasMap(uuid, key);
	}

	public static boolean hasNotMap(Player player, MapKey<?, ?> key) {
		return hasNotMap(player.getUniqueId(), key);
	}

	public static <K, V> boolean hasValue(UUID uuid, MapKey<K, V> key, V value) {
		return getMapMapOrEmpty(uuid).getOrDefault(key, Collections.emptyMap()).containsValue(value);
	}

	public static <K, V> boolean hasValue(Player player, MapKey<K, V> key, V value) {
		return hasValue(player.getUniqueId(), key, value);
	}

	public static <K, V> boolean hasKey(UUID uuid, MapKey<K, V> mKey, K key) {
		return getMapMapOrEmpty(uuid).getOrDefault(mKey, Collections.emptyMap()).containsKey(key);
	}

	public static <K, V> boolean hasKey(Player player, MapKey<K, V> mKey, K key) {
		return hasKey(player.getUniqueId(), mKey, key);
	}

	@SuppressWarnings("unchecked")
	public static <K, V> V remove(UUID uuid, MapKey<K, V> mKey, K key) {
		return (V) getMapMapOrEmpty(uuid).getOrDefault(mKey, Collections.emptyMap()).remove(key);
	}

	public static <K, V> V remove(Player player, MapKey<K, V> mKey, K key) {
		return remove(player.getUniqueId(), mKey, key);
	}

	@SuppressWarnings("unchecked")
	public static <K, V> Map<K, V> removeMap(UUID uuid, MapKey<K, V> key) {
		return (Map<K, V>) getMapMapOrEmpty(uuid).remove(key);
	}

	public static <K, V> Map<K, V> removeMap(Player player, MapKey<K, V> key) {
		return removeMap(player.getUniqueId(), key);
	}

	private static Map<MapKey<?, ?>, Map<Object, Object>> getMapMapOrCreate(UUID uuid) {
		return playerDataMap.computeIfAbsent(uuid, k -> new HashMap<>());
	}

	private static Map<MapKey<?, ?>, Map<Object, Object>> getMapMapOrEmpty(UUID uuid) {
		return playerDataMap.getOrDefault(uuid, Collections.emptyMap());
	}
}
