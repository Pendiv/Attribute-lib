package net.logiench.shardLib.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * ShardLibのリロードが開始される前のイベント。
 * リロードが失敗した場合は呼び出されません。
 * これが呼び出されても、リロードは失敗する可能性があります。
 */
public class ShardLibReloadStartEvent extends Event {
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
