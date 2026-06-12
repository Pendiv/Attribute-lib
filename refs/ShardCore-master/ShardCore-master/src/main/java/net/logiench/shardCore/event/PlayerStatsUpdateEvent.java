package net.logiench.shardCore.event;

import lombok.Getter;
import net.logiench.shardCore.core.player.system.PlayerCharacter;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerStatsUpdateEvent extends PlayerEvent {
	private static final HandlerList handlers = new HandlerList();

	@Getter
	private final PlayerCharacter character;

	public PlayerStatsUpdateEvent(@NotNull PlayerCharacter character) {
		super(character.player());
		this.character = character;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
