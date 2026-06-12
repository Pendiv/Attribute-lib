package net.logiench.shardCore.event;

import lombok.Getter;
import net.logiench.shardCore.db.repository.Job;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * プレイヤーがどの職業を利用するか選択した際に呼び出されます。
 */
public class ProfileLoadEvent extends PlayerEvent {

	private static final HandlerList HANDLER_LIST = new HandlerList();
	@Getter
	private final int profileId;
	@Getter
	private final Job job;

	public ProfileLoadEvent(@NotNull Player player, int profileId, @NotNull Job job) {
		super(player);
		this.profileId = profileId;
		this.job = job;
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
