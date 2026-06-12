package net.logiench.shardCore.listener;

import com.google.inject.Inject;
import net.logiench.logienchlibv2.api.minecraft.time.Task;
import net.logiench.logienchlibv2.api.minecraft.time.Timer;
import net.logiench.shardCore.ShardCore;
import net.logiench.shardCore.config.data.LimboPlayerConfigState;
import net.logiench.shardCore.core.player.system.PlayerSessionManager;
import net.logiench.shardCore.event.ProfileLoadEvent;
import net.logiench.shardCore.event.ProfileNeutralizeEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import java.util.List;

/**
 *
 */
public class LimboProtectionListener implements Listener {

	// Limboの際に0に設定するAttribute
	private static final List<Attribute> SET_ZERO_ATTRIBUTES = List.of(
		Attribute.ATTACK_DAMAGE, Attribute.CAMERA_DISTANCE,
		Attribute.BLOCK_INTERACTION_RANGE, Attribute.ENTITY_INTERACTION_RANGE, Attribute.JUMP_STRENGTH
	);
	private static final NamespacedKey LIMBO_ATTRIBUTE_KEY = new NamespacedKey(ShardCore.getInstance(), "limbo");

	private final PlayerSessionManager sessionManager;
	private final LimboPlayerConfigState configState;

	@Inject
	private LimboProtectionListener(PlayerSessionManager sessionManager, LimboPlayerConfigState configState) {
		this.sessionManager = sessionManager;
		this.configState = configState;
	}

	/**
	 * Limbo状態のプレイヤーを安全に維持するための毎ティック行われる処理を開始します
	 */
	public void limboTaskStart() {
		Timer.on(() -> {
			for (Player player : Bukkit.getOnlinePlayers()) {
				if (!sessionManager.isInLimbo(player.getUniqueId())) {
					continue;
				}
				limboTask(player);
			}
		}, 1, 1);
	}

	@EventHandler
	private void onProfileNeutralize(ProfileNeutralizeEvent ev) {
		Player player = ev.getPlayer();

		player.setGameMode(GameMode.ADVENTURE);
		player.setGravity(false);
		player.setCollidable(false);
		player.setInvisible(true);
		player.setInvulnerable(true);
		// Neutralize解除時にfly判定でkickされることがあるのでそれの対策
		player.setAllowFlight(true);

		// todo 所持アイテムの全削除
		// ただしまだ保存を作成していないので行わない
		//		player.getInventory().clear();

		for (Attribute attribute : SET_ZERO_ATTRIBUTES) {
			AttributeInstance instance = player.getAttribute(attribute);
			if (instance == null) {
				continue;
			}
			// Transientだから再参加などで消えるけど一応
			if (instance.getModifier(LIMBO_ATTRIBUTE_KEY) != null) {
				continue;
			}
			// setBaseValue(0)でやると戻せなくなる値もあるので x0 をして全て0にする
			instance.addTransientModifier(new AttributeModifier(LIMBO_ATTRIBUTE_KEY,
				-1, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
		}
		Task.on(() -> player.teleport(configState.getSafeLocation()));
	}

	@EventHandler
	private void onProfileLoad(ProfileLoadEvent ev) {
		Player player = ev.getPlayer();

		player.setAllowFlight(false);
		player.setFlying(false);

		player.setGameMode(GameMode.ADVENTURE);
		player.setGravity(true);
		player.setCollidable(true);
		player.setInvisible(false);
		player.setInvulnerable(false);

		for (Attribute attribute : SET_ZERO_ATTRIBUTES) {
			AttributeInstance instance = player.getAttribute(attribute);
			if (instance == null) {
				continue;
			}
			instance.removeModifier(LIMBO_ATTRIBUTE_KEY);
		}

		Location loc = player.getLocation();
		for (int y = loc.getBlockY(); y > loc.getWorld().getMinHeight(); y--) {
			Location test = loc.clone();
			test.setY(y);
			if (test.getBlock().isSolid()) {
				player.teleport(test.add(0, 1, 0));
				break;
			}
		}
	}

	private void limboTask(Player player) {
		player.setVelocity(new Vector(0, 0, 0));
		player.setFlying(false);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	private void onPlayerMove(PlayerMoveEvent ev) {
		Player player = ev.getPlayer();

		// Limbo状態でなければ無視
		if (!sessionManager.isInLimbo(player.getUniqueId())) {
			return;
		}

		Location from = ev.getFrom();
		Location to = ev.getTo();

		// XYZ座標に変化がない（視点移動のみ）場合は許可
		if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) {
			return;
		}

		// 座標のみ移動前の状態に戻し、視点の向き(Yaw/Pitch)は新しい状態を適用する
		Location fixedLoc = from.clone();
		fixedLoc.setYaw(to.getYaw());
		fixedLoc.setPitch(to.getPitch());

		ev.setTo(fixedLoc);
	}

	private void cancelIfLimbo(Entity entity, Cancellable ev) {
		if (entity instanceof Player player && sessionManager.isInLimbo(player.getUniqueId())) {
			ev.setCancelled(true);
		}
	}

	// 職業を選択していないプレイヤーを安全に維持するためのイベント
	@EventHandler(priority = EventPriority.LOWEST)
	public void onDamage(EntityDamageEvent ev) {
		cancelIfLimbo(ev.getEntity(), ev);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onDamageByLimbo(EntityDamageByEntityEvent ev) {
		cancelIfLimbo(ev.getDamager(), ev);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onInteract(PlayerInteractEvent ev) {
		cancelIfLimbo(ev.getPlayer(), ev);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onDropItem(PlayerDropItemEvent ev) {
		cancelIfLimbo(ev.getPlayer(), ev);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onBlockBreak(BlockBreakEvent ev) {
		cancelIfLimbo(ev.getPlayer(), ev);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onBlockPlace(BlockPlaceEvent ev) {
		cancelIfLimbo(ev.getPlayer(), ev);
	}
}
