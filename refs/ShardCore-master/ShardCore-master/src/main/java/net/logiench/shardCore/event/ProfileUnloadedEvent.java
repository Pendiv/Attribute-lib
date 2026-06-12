package net.logiench.shardCore.event;

import lombok.Getter;
import net.logiench.shardCore.db.repository.Job;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * プレイヤーがその職業をやめた(切り替えのためにリセットや退出した)際に呼び出されます
 */
public class ProfileUnloadedEvent extends PlayerEvent {

	private static final HandlerList HANDLER_LIST = new HandlerList();
	@Getter
	private final int lastProfileId;
	@Getter
	private final Job lastJob;

	public ProfileUnloadedEvent(@NotNull Player player, int lastProfileId, @NotNull Job lastJob) {
		super(player);
		this.lastProfileId = lastProfileId;
		this.lastJob = lastJob;
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
