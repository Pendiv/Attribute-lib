package net.logiench.logienchlibv2.api.minecraft.menu.input;

import org.bukkit.entity.Player;

public final class InputMenuEvent {
	private final Player p;
	private final String text;

	InputMenuEvent(Player p, String text) {
		this.p = p;
		this.text = text;
	}

	public Player getPlayer() {
		return p;
	}

	public String getText() {
		return text;
	}
}
