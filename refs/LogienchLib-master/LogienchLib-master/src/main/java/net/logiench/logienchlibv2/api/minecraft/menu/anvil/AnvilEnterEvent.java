package net.logiench.logienchlibv2.api.minecraft.menu.anvil;

import net.logiench.logienchlibv2.api.minecraft.time.Task;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public final class AnvilEnterEvent implements Cancellable {
	private final Inventory inventory;
	private final Player p;
	private final String text;
	private boolean cancelled;

	public AnvilEnterEvent(Inventory inventory, Player p, String text, boolean cancelled) {
		this.inventory = inventory;
		this.p = p;
		this.text = text;
		this.cancelled = cancelled;
	}

	@NotNull
	public Inventory getInventory() {
		return inventory;
	}

	@NotNull
	public Player getPlayer() {
		return p;
	}

	@NotNull
	public String getText() {
		return text;
	}

	public void close() {
		Task.on(p::closeInventory);
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}
}
