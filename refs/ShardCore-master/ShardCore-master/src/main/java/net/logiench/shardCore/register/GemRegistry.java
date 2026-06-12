package net.logiench.shardCore.register;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.logiench.shardCore.core.item.base.def.GemItem;
import net.logiench.shardCore.core.item.base.gem.GemActionRegistry;
import net.logiench.shardCore.util.ClassUtils;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class GemRegistry {
	private static final String GEM_PATH = "net.logiench.shardCore.data.item.def.gem";

	private final Map<Class<? extends GemItem>, String> gems = new HashMap<>();
	private final Map<String, GemItem> idGems = new HashMap<>();
	private final Map<Class<? extends GemItem>, GemActionRegistry> gemActions = new HashMap<>();

	@Inject
	public GemRegistry() {
		for (Class<? extends GemItem> clazz : ClassUtils.findSubClasses(GemItem.class, GEM_PATH)) {
			register(clazz);
		}
	}

	protected <T extends GemItem> void register(@NotNull Class<T> clazz) {
		if (gems.containsKey(clazz)) {
			return;
		}
		GemItem instance = ClassUtils.initialize(clazz);
		if (instance == null) {
			return;
		}
		GemActionRegistry registry = new GemActionRegistry();
		instance.registerAction(registry);

		gems.put(clazz, instance.getId());
		idGems.put(instance.getId(), instance);
		gemActions.put(clazz, registry);
	}

	public @Nullable GemActionRegistry getActionRegistry(@NotNull Class<? extends GemItem> clazz) {
		return gemActions.get(clazz);
	}

	public @Nullable <T extends GemItem> T get(@NotNull Class<T> clazz) {
		String id = gems.get(clazz);
		if (id == null) {
			return null;
		}
		return clazz.cast(idGems.get(id));
	}

	public @Nullable GemItem get(@NotNull NamespacedKey key) {
		return get(key.getKey());
	}

	public @Nullable GemItem get(String id) {
		return idGems.get(id);
	}

	public String getId(@NotNull Class<? extends GemItem> clazz) {
		return gems.get(clazz);
	}

	@Unmodifiable
	public Collection<GemItem> getAllItems() {
		return Collections.unmodifiableCollection(idGems.values());
	}
}
