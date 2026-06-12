package net.logiench.shardCore.register;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import net.logiench.shardCore.core.itemRequirement.base.RequirementResolver;
import net.logiench.shardCore.core.itemRequirement.base.RequirementType;
import net.logiench.shardCore.util.ClassUtils;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class RequirementRegistry {
	private static final String REQUIREMENT_PATH = "net.logiench.shardCore.data.itemRequirement";

	private final Map<Class<? extends RequirementType<?>>, RequirementType<?>> requirements = new HashMap<>();
	private final Map<String, RequirementType<?>> keyRequirements = new HashMap<>();

	private final Map<Class<? extends RequirementResolver<?, ?>>, RequirementResolver<?, ?>> resolvers = new HashMap<>();

	@Inject
	@SuppressWarnings("unchecked")
	public RequirementRegistry(Injector injector) {
		for (Class<? extends RequirementType<?>> clazz : ClassUtils.findSubClasses(
			(Class<RequirementType<?>>) (Class<?>) RequirementType.class,
			REQUIREMENT_PATH
		)) {
			register(clazz);
		}
		for (Class<? extends RequirementResolver<?, ?>> clazz : ClassUtils.findSubClasses(
			(Class<RequirementResolver<?, ?>>) (Class<?>) RequirementResolver.class,
			REQUIREMENT_PATH
		)) {
			resolverRegister(injector, clazz);
		}
	}

	protected <T extends RequirementType<?>> void register(@NotNull Class<T> clazz) {
		if (requirements.containsKey(clazz)) {
			return;
		}
		T instance = ClassUtils.initialize(clazz);
		if (instance != null) {
			requirements.put(clazz, instance);
			keyRequirements.put(instance.getKeyName(), instance);
		}
	}

	private <T extends RequirementResolver<?, ?>> void resolverRegister(Injector injector, @NotNull Class<T> clazz) {
		if (resolvers.containsKey(clazz)) {
			return;
		}
		resolvers.put(clazz, injector.getInstance(clazz));
	}

	public RequirementType<?> getType(NamespacedKey key) {
		return getType(key.getKey());
	}

	public RequirementType<?> getType(String key) {
		return keyRequirements.get(key);
	}

	@SuppressWarnings("unchecked")
	public <T extends RequirementType<?>> T getType(@NotNull Class<T> clazz) {
		return (T) requirements.get(clazz);
	}

	@SuppressWarnings("unchecked")
	public <T extends RequirementResolver<?, ?>> T getResolver(@NotNull Class<T> clazz) {
		return (T) resolvers.get(clazz);
	}

	public @Unmodifiable Collection<RequirementType<?>> getAllTypes() {
		return Collections.unmodifiableCollection(requirements.values());
	}
}
