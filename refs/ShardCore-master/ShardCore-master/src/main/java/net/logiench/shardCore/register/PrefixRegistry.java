package net.logiench.shardCore.register;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.logiench.shardCore.core.item.base.def.ItemGroup;
import net.logiench.shardCore.data.prefix.Prefix;
import net.logiench.shardCore.util.ClassUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

@Singleton
public class PrefixRegistry {
	private static final String PREFIX_PATH = "net.logiench.shardCore.data.prefix";

	private final Map<ItemGroup, List<Prefix>> groupPrefixes = new HashMap<>();
	private final Map<Class<? extends Prefix>, Prefix> prefixes = new HashMap<>();
	private final Map<String, Prefix> idPrefixes = new HashMap<>();

	@Inject
	protected PrefixRegistry() {
		for (Class<? extends Prefix> clazz : ClassUtils.findSubClasses(Prefix.class, PREFIX_PATH)) {
			register(clazz);
		}
	}

	public List<? extends Prefix> getPrefixes(ItemGroup group) {
		return Collections.unmodifiableList(groupPrefixes.getOrDefault(group, List.of()));
	}

	protected <T extends Prefix> void register(@NotNull Class<T> clazz) {
		if (prefixes.containsKey(clazz)) {
			return;
		}
		T instance = ClassUtils.initialize(clazz);
		if (instance != null) {
			prefixes.put(clazz, instance);
			idPrefixes.put(instance.getId(), instance);
			for (ItemGroup group : instance.getTargetItemGroups()) {
				groupPrefixes.computeIfAbsent(group, a -> new ArrayList<>()).add(instance);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public <T extends Prefix> T get(@NotNull Class<T> clazz) {
		return (T) prefixes.get(clazz);
	}

	public Prefix get(String id) {
		return idPrefixes.get(id);
	}

	@Unmodifiable
	public Collection<Prefix> getAllItems() {
		return Collections.unmodifiableCollection(prefixes.values());
	}
}
