package net.logiench.shardCore.core.menu;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.logiench.shardCore.ShardCore;
import net.logiench.shardCore.core.menu.util.AbstractMenu;
import net.logiench.shardCore.util.ClassUtils;

import java.io.*;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Singleton
public class MenuStateManager {
	private transient final Set<String> menuClassNames = new HashSet<>();
	private final Set<String> disabledMenus = ConcurrentHashMap.newKeySet();
	private final File dataFile;
	private final Gson gson = new Gson();

	@Inject
	private MenuStateManager() {
		this.dataFile = new File(ShardCore.getInstance().getDataFolder(), "disabled_menu.json");
		this.menuClassNames.addAll(ClassUtils.findSubClasses(
				AbstractMenu.class, "net.logiench.shardCore.core.menu")
			.stream().map(Class::getSimpleName).toList());
		load();
	}

	public boolean isDisabled(Class<? extends AbstractMenu> menuClass) {
		return disabledMenus.contains(menuClass.getSimpleName());
	}

	public void disable(String menuClassName) {
		if (!menuClassNames.contains(menuClassName)) {
			return;
		}
		if (disabledMenus.add(menuClassName)) {
			save();
		}
	}

	public void enable(String menuClassName) {
		if (!menuClassNames.contains(menuClassName)) {
			return;
		}
		if (disabledMenus.remove(menuClassName)) {
			save();
		}
	}

	private void load() {
		if (!dataFile.exists()) {
			return;
		}
		try (Reader reader = new FileReader(dataFile)) {
			Type type = new TypeToken<Set<String>>() {}.getType();
			Set<String> loaded = gson.fromJson(reader, type);
			if (loaded != null) {
				disabledMenus.addAll(loaded);
				// 起動時の警告ログ
				if (!disabledMenus.isEmpty()) {
					Logger logger = ShardCore.getPLogger();
					logger.warning("以下のメニューは無効化されています: ");
					for (String menuClassName : disabledMenus) {
						String message = "  - " + menuClassName;
						if (!menuClassNames.contains(menuClassName)) {
							// 無効化して消したクラスが残り続けるのを警告
							message += " (このクラスは存在しません)";
						}
						logger.warning(message);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void save() {
		try (Writer writer = new FileWriter(dataFile)) {
			gson.toJson(disabledMenus, writer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Set<String> getMenuClassNames() {
		return Collections.unmodifiableSet(menuClassNames);
	}

	public Set<String> getDisabledMenus() {
		return Collections.unmodifiableSet(disabledMenus);
	}

	public Set<String> getEnabledMenus() {
		return menuClassNames.stream()
			.filter(c -> !disabledMenus.contains(c))
			.collect(Collectors.toUnmodifiableSet());
	}

	public boolean hasDisabledMenu() {
		return !disabledMenus.isEmpty();
	}

	public boolean hasEnabledMenu() {
		return disabledMenus.size() != menuClassNames.size();
	}
}
