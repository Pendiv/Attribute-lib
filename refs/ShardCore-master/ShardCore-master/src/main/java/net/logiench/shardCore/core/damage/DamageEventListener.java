package net.logiench.shardCore.core.damage;

import com.google.inject.Inject;
import com.google.inject.Injector;
import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import net.logiench.logienchlibv2.api.minecraft.time.Timer;
import net.logiench.shardCore.core.loot.system.LootItemGenerateProvider;
import net.logiench.shardCore.core.mob.base.ShardMob;
import net.logiench.shardCore.core.mob.system.loader.MobLoader;
import net.logiench.shardCore.data.stats.keys.CoreStats;
import net.logiench.shardCore.register.MobLootTableRegistry;
import net.logiench.shardCore.register.MobRegistry;
import net.logiench.shardLib.api.ShardLibAPI;
import net.logiench.shardLib.api.ShardLibProvider;
import net.logiench.shardLib.api.attribute.AttributeAPI;
import net.logiench.shardLib.api.mob.MobCharacterAPI;
import net.logiench.shardLib.api.player.PlayerCharacterAPI;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class DamageEventListener implements Listener {
	private final MobRegistry mobRegistry;
	private final MobLootTableRegistry mobLootTableRegistry;

	@Inject
	public DamageEventListener(Injector injector) {
		this.mobRegistry = injector.getInstance(MobRegistry.class);
		this.mobLootTableRegistry = injector.getInstance(MobLootTableRegistry.class);
	}

	/*
	より詳細なイベントである、EntityDamageBy...では、内部ステータス上でのダメージ量をev.setDamage()する
	全てのダメージで呼び出されるEntityDamageEventでは、内部的なステータスダメージを適応 & 表面的な体力の割合減少を行う

	エンティティが受けたダメージを確認するには、データHPならHIGHまで、実際のHPならMONITOR、
	ただし実際のHPはダメージによって減少させられず、直接的に書き換えられるため実質使用できません。
	 */

	// できるだけ早くデータHPのダメージ値に修正する
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onEntityDamageByEntity(EntityDamageByEntityEvent ev) {
		// 無敵時間中のダメージ更新を無効化する
		if (ev.getEntity() instanceof LivingEntity target) {
			if (target.getNoDamageTicks() > 10) {
				ev.setCancelled(true);
				return;
			}
		}

		DamageType type = DamageType.PHYSICS;
		Entity sourceEntity = ev.getDamager();
		if (sourceEntity instanceof Projectile projectile) {
			if (projectile.getShooter() instanceof Entity e) {
				sourceEntity = e;
				type = DamageType.PROJECTILE;
			}
		}
		AttributeAPI source = getAttributeAPI(sourceEntity);
		if (source == null) {
			return;
		}
		AttributeAPI target = getAttributeAPI(ev.getEntity());

		CalculateDamage damage = new CalculateDamage(ev.getDamager(), source, target, type);
		ev.setDamage(damage.getDamage());
	}

	// できるだけ遅く実際のHPに適応する
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	private void onEntityDamage(EntityDamageEvent ev) {
		if (!(ev.getEntity() instanceof LivingEntity entity)) {
			return;
		}
		AttributeAPI attributeAPI;
		if (entity instanceof Player p) {
			Optional<PlayerCharacterAPI> characterAPI = ShardLibProvider.get().getPlayerAPI().getCharacterAPI(p);
			if (characterAPI.isEmpty()) {
				return;
			}
			attributeAPI = characterAPI.get().getAttributeAPI();
		} else {
			Optional<MobCharacterAPI> characterAPI = ShardLibProvider.get().getMobAPI().getCharacterAPI(entity);
			if (characterAPI.isEmpty()) {
				return;
			}
			attributeAPI = characterAPI.get().getAttributeAPI();
		}
		double damage = ev.getDamage();
		double newDataHp = attributeAPI.subtractBaseAttribute(CoreStats.HP, damage);
		attributeAPI.recalculateStats();

		if (newDataHp <= 0) {
			entity.setHealth(0);
		} else {
			double maxDataHp = attributeAPI.getFinalAttribute(CoreStats.MAX_HP);
			AttributeInstance instance = entity.getAttribute(Attribute.MAX_HEALTH);

			if (instance != null) {
				double maxHealth = instance.getValue();
				double newHealth = newDataHp * (maxHealth / maxDataHp);
				// 現在の体力を新しい体力で上書きする。見た目のために一番小さいダメージを与える
				entity.setHealth(newHealth);
				ev.setDamage(0);
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	private void onEntityDeath(EntityDeathEvent ev) {
		Entity entity = ev.getEntity();
		if (entity instanceof Player) {
			return;
		}
		MobLoader loader = MobLoader.of(entity);
		if (loader == null) {
			return;
		}
		ShardMob shardMob = mobRegistry.get(loader.getId());
		LootItemGenerateProvider lootItem = mobLootTableRegistry.get(shardMob.getLootTableId());
		if (lootItem != null) {
			List<SuperItemStack> lootItems = lootItem.generateItem(null);
			World world = entity.getWorld();
			Location loc = entity.getLocation();
			Timer.startTimer(i -> {
				world.dropItem(loc, lootItems.get(i).build());
				world.playSound(loc, Sound.ENTITY_ITEM_PICKUP, 1, 1.2f);
			}, 12, 3, lootItems.size());
		}
	}

	@Nullable
	private AttributeAPI getAttributeAPI(Entity e) {
		ShardLibAPI api = ShardLibProvider.get();
		if (e instanceof Player p) {
			return api.getPlayerAPI().getCharacterAPI(p)
				.map(PlayerCharacterAPI::getAttributeAPI).orElse(null);
		}
		return api.getMobAPI().getCharacterAPI(e)
			.map(MobCharacterAPI::getAttributeAPI)
			.orElse(null);
	}
}
