package net.logiench.shardCore.core.menu.server;

import net.kyori.adventure.text.Component;
import net.logiench.shardCore.core.menu.util.SimpleMenu;
import org.bukkit.entity.Player;

public class NotificationMenu extends SimpleMenu {
	private static final Component TITLE = Component.text("Notification");

	public NotificationMenu(Player player) {
		super(player);
	}

	@Override
	protected void initMenu() {

	}

	@Override
	public Component getTitle() {
		return TITLE;
	}

	@Override
	public int getSize() {
		return 54;
	}
}
