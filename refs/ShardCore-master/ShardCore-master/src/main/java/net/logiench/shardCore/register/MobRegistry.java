package net.logiench.shardCore.register;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.logiench.shardCore.core.mob.base.ShardMob;
import net.logiench.shardCore.util.ClassUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class MobRegistry {
	private static final String MOB_PATH = "net.logiench.shardCore.data.mob";

	private final Map<Class<? extends ShardMob>, ShardMob> mobs = new HashMap<>();
	private final Map<String, ShardMob> idMobs = new HashMap<>();

	@Inject
	private MobRegistry() {
		for (Class<? extends ShardMob> clazz : ClassUtils.findSubClasses(ShardMob.class, MOB_PATH)) {
			register(clazz);
		}
	}

	protected <T extends ShardMob> void register(@NotNull Class<T> clazz) {
		if (mobs.containsKey(clazz)) {
			return;
		}
		T instance = ClassUtils.initialize(clazz);
		if (instance != null) {
			mobs.put(clazz, instance);
			idMobs.put(instance.getId(), instance);
		}
	}

	public <T extends ShardMob> T get(@NotNull Class<T> clazz) {
		ShardMob mob = mobs.get(clazz);
		if (mob == null) {
			return null;
		}
		return clazz.cast(mob);
	}

	public ShardMob get(String id) {
		return idMobs.get(id);
	}

	@Unmodifiable
	public Collection<ShardMob> getAllItems() {
		return Collections.unmodifiableCollection(mobs.values());
	}
}
