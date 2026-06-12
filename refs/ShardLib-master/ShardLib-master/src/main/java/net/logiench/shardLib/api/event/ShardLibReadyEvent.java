package net.logiench.shardLib.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * ShardLibがConfigやAPIからの定義読み込みまでを完了した際に発生するイベント
 */
public class ShardLibReadyEvent extends Event {
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
