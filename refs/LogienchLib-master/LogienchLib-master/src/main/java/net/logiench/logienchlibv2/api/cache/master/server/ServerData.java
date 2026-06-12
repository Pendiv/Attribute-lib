package net.logiench.logienchlibv2.api.cache.master.server;


import net.logiench.logienchlibv2.api.cache.key.server.ServerKey;
import net.logiench.logienchlibv2.api.minecraft.time.Delay;
import net.logiench.logienchlibv2.api.minecraft.time.TimeTask;
import net.logiench.logienchlibv2.base.cache.CacheManage;
import org.jetbrains.annotations.Range;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public final class ServerData extends CacheManage {

	@SuppressWarnings("unchecked")
	public static <T> T put(ServerKey<T> key, Object k, T v, @Range(from = 0, to = Integer.MAX_VALUE) int deleteTick) {
		if (deleteTick == 0) {
			return null;
		}
		T result = (T) serverData.computeIfAbsent(key, kk -> new HashMap<>()).put(k, v);
		TimeTask task = deleteTimer.put(
				key.toString() + k,
				Delay.on(() -> {
					getMapOrEmpty(key).remove(k);
					deleteTimer.remove(key.toString() + k);
				}, deleteTick)
		);
		if (task != null) {
			task.cancel();
		}
		return result;
	}

	public static <T> boolean hasNotKey(ServerKey<T> key, Object k) {
		return !hasKey(key, k);
	}

	public static <T> boolean hasKey(ServerKey<T> key, Object k) {
		return getMapOrEmpty(key).containsKey(k);
	}

	public static <T> boolean hasValue(ServerKey<T> key, T v) {
		return getMapOrEmpty(key).containsValue(v);
	}

	public static <T> T get(ServerKey<T> key, Object k) {
		return getMapOrEmpty(key).get(k);
	}

	public static <T> void remove(ServerKey<T> key, Object k) {
		T result = getMapOrEmpty(key).remove(k);
		if (result != null) {
			deleteTimer.remove(key.toString() + k).cancel();
		}
	}

	@SuppressWarnings("unchecked")
	private static <K, T> Map<K, T> getMapOrEmpty(ServerKey<T> key) {
		return (Map<K, T>) serverData.getOrDefault(key, Collections.emptyMap());
	}
}
