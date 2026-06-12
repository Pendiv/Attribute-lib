package net.logiench.shardLib.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * ShardLibがリロードされ、それが成功し、完了したことを表すイベント
 */
public class ShardLibReloadSuccessEvent extends Event {
	private final static HandlerList handlerList = new HandlerList();

	@NotNull
	public static HandlerList getHandlerList() {
		return handlerList;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return handlerList;
	}
}
