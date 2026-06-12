package net.logiench.shardCore.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * 中立な、未確定な状態のプレイヤーが増加した際に呼び出されます。
 * <p>以下の2つの場合に呼び出されます</p>
 * <ol>
 *     <li>新しくプレイヤーがワールドに参加した</nl>
 *     <li>既存のプレイヤーが職業切り替えのためにUnloadした</nl>
 * </ol>
 * このイベントは{@link ProfileUnloadedEvent}よりも後に呼び出されます
 */
public class ProfileNeutralizeEvent extends PlayerEvent {

	private static final HandlerList HANDLER_LIST = new HandlerList();

	public ProfileNeutralizeEvent(@NotNull Player player) {
		super(player);
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}

	@NotNull
	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}
}
