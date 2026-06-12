package net.logiench.shardCore.core.player.system;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.logiench.logienchlibv2.api.minecraft.time.Task;
import net.logiench.shardCore.db.repository.Job;
import net.logiench.shardCore.db.repository.JobBaseEntity;
import net.logiench.shardCore.db.service.PlayerJobBaseService;
import net.logiench.shardCore.event.ProfileLoadEvent;
import net.logiench.shardCore.event.ProfileNeutralizeEvent;
import net.logiench.shardCore.event.ProfileUnloadedEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
public class PlayerSessionManager implements Listener {

	private static final PluginManager PLUGIN_MANAGER = Bukkit.getPluginManager();

	private final PlayerJobBaseService jobBaseService;

	@Inject
	public PlayerSessionManager(PlayerJobBaseService jobBaseService) {
		this.jobBaseService = jobBaseService;
	}

	/**
	 * 指定されたUUIDのプレイヤーがまだ職業を選択していないか
	 *
	 * @param playerId 対象のプレイヤー
	 * @return 職業が選択されていない、または存在しないidの場合はtrue, それ以外の場合はfalse
	 */
	public boolean isInLimbo(@NotNull UUID playerId) {
		return jobBaseService.getProfileId(playerId) == null;
	}

	/**
	 * 職業を選択します
	 *
	 * @param player 選択するプレイヤー
	 * @param job    選択する職業
	 * @return 職業読み込みの結果のboolを持った非同期処理
	 */
	public CompletableFuture<Boolean> loadProfile(Player player, Job job) {
		return jobBaseService.loadProfile(player, job)
			.thenCompose(e -> {
				if (e == null) {
					return CompletableFuture.completedFuture(false);
				}
				return runNextTick(() -> PLUGIN_MANAGER.callEvent(new ProfileLoadEvent(player, e.getProfileId(), job)));
			});
	}

	/**
	 * 職業をアンロードします
	 *
	 * @param player アンロードするプレイヤー
	 * @return 職業アンロードの結果のboolを持った非同期処理
	 */
	public CompletableFuture<Boolean> unloadProfile(Player player) {
		JobBaseEntity table = jobBaseService.getEntity(player.getUniqueId());
		if (table == null) {
			return CompletableFuture.completedFuture(false);
		}
		return runNextTick(() -> PLUGIN_MANAGER.callEvent(new ProfileNeutralizeEvent(player)))
			.thenCompose(v -> jobBaseService.unloadProfile(player))
			.thenCompose(v -> {
				if (v) {
					return runNextTick(() -> PLUGIN_MANAGER.callEvent(
						new ProfileUnloadedEvent(player, table.getProfileId(), table.getJob())));
				}
				return CompletableFuture.completedFuture(false);
			});
	}

	private CompletableFuture<Boolean> runNextTick(Runnable runnable) {
		CompletableFuture<Boolean> future = new CompletableFuture<>();
		Task.on(() -> {
			try {
				runnable.run();
				future.complete(true);
			} catch (Exception e) {
				future.completeExceptionally(e);
			}
		});
		return future;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	private void onPlayerJoin(PlayerJoinEvent ev) {
		PLUGIN_MANAGER.callEvent(new ProfileNeutralizeEvent(ev.getPlayer()));
	}
}
