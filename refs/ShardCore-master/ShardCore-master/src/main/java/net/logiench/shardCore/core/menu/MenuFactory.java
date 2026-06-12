package net.logiench.shardCore.core.menu;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import net.logiench.shardCore.core.menu.util.AbstractMenu;
import org.bukkit.entity.Player;

import java.util.function.Function;
import java.util.function.Supplier;

@Singleton
public class MenuFactory {
	private final Injector injector;

	@Inject
	public MenuFactory(Injector injector) {
		this.injector = injector;
	}

	public <T extends AbstractMenu> T create(Function<Player, T> constructor, Player player) {
		T menu = constructor.apply(player);
		injector.injectMembers(menu);
		return menu;
	}

	public <T extends AbstractMenu> T create(Supplier<T> constructor) {
		T menu = constructor.get();
		injector.injectMembers(menu);
		return menu;
	}

	/*public <T extends AbstractMenu> T create(BiFunction<Player, Inventory, T> constructor, Player player, Inventory inventory) {
		T menu = constructor.apply(player, inventory);
		injector.injectMembers(menu);
		return menu;
	}

	public <T extends AbstractMenu> T create(BiFunction<Player, AbstractMenu, T> constructor, Player player, AbstractMenu oldMenu) {
		T menu = constructor.apply(player, oldMenu);
		injector.injectMembers(menu);
		return menu;
	}*/
}
