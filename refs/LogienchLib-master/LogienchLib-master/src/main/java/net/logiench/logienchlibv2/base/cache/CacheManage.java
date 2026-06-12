package net.logiench.logienchlibv2.base.cache;

import net.logiench.logienchlibv2.api.cache.key.player.ListKey;
import net.logiench.logienchlibv2.api.cache.key.player.MapKey;
import net.logiench.logienchlibv2.api.cache.key.player.SingleKey;
import net.logiench.logienchlibv2.api.cache.key.server.ServerKey;
import net.logiench.logienchlibv2.api.minecraft.time.TimeTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
public class CacheManage {
	protected static final Map<UUID, Map<ListKey<?>, List<Object>>> playerDataList = new ConcurrentHashMap<>();
	protected static final Map<UUID, Map<MapKey<?, ?>, Map<Object, Object>>> playerDataMap = new ConcurrentHashMap<>();
	protected static final Map<UUID, Map<SingleKey<?>, Object>> playerDataSingle = new ConcurrentHashMap<>();

	protected static final Map<ServerKey<?>, Map<Object, Object>> serverData = new ConcurrentHashMap<>();

	protected static final Map<String, TimeTask> deleteTimer = new ConcurrentHashMap<>();


	public static void playerJoin(UUID uuid) {
		playerDataList.put(uuid, new HashMap<>());
		playerDataMap.put(uuid, new HashMap<>());
		playerDataSingle.put(uuid, new HashMap<>());
	}

	public static void playerQuit(UUID uuid) {
		playerDataList.remove(uuid);
		playerDataMap.remove(uuid);
		playerDataSingle.remove(uuid);
	}
}
