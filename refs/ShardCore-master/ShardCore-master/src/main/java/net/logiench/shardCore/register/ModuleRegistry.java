package net.logiench.shardCore.register;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import net.logiench.shardCore.core.item.base.module.ItemModule;
import net.logiench.shardCore.util.ClassUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class ModuleRegistry {
	private static final String MODULE_PATH = "net.logiench.shardCore.data.item.module";

	private final Map<Class<? extends ItemModule<?>>, ItemModule<?>> modules = new HashMap<>();

	@Inject
	@SuppressWarnings("rawtypes")
	public ModuleRegistry(Injector injector) {
		for (Class<? extends ItemModule> clazz : ClassUtils.findSubClasses(ItemModule.class, MODULE_PATH)) {
			register(injector, clazz);
		}
	}

	private <T extends ItemModule<?>> void register(Injector injector, @NotNull Class<T> clazz) {
		if (modules.containsKey(clazz)) {
			return;
		}
		T module = injector.getInstance(clazz);
		if (module == null) {
			return;
		}
		modules.put(clazz, module);
	}

	public @Nullable <T extends ItemModule<?>> T get(@NotNull Class<T> clazz) {
		ItemModule<?> module = modules.get(clazz);
		if (module == null) {
			return null;
		}
		return clazz.cast(module);
	}

	@Unmodifiable
	public Collection<ItemModule<?>> getAllItems() {
		return Collections.unmodifiableCollection(modules.values());
	}
}
