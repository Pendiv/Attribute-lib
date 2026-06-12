package net.logiench.shardCore.data.skill.def;

import net.logiench.shardCore.core.skill.base.ActiveSkill;
import net.logiench.shardCore.core.skill.system.SkillContext;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ActiveTestSkill extends ActiveSkill {
	//	private boolean isJump = false;
	private final Map<UUID, Integer> damagedEntities = new HashMap<>();

	public ActiveTestSkill(SkillContext context) {
		super(context);
	}

	@Override
	public boolean start() {
		Player player = context.player();
		Location loc = player.getLocation().clone();
		loc.setPitch(0);

		Vector dir = loc.getDirection();
		dir.multiply(3f);
		dir.setY(0.15);
		player.setVelocity(dir);
		return true;
	}

	@Override
	protected boolean tick() {
		Player player = context.player();
		Vector v = player.getVelocity();
		if (v.length() < 0.5) {
			return false;
		}
		// ジャンプを発動のタイミングと同時に行うとY方向のベクターを追加できてしまう
		// 飛びすぎるので距離を減らす。通常のジャンプは約0.42
		// これすると頭ぶつけたみたいになるから無し
		/*if (!isJump && v.getY() > 0.2) {
			isJump = true;
			v.setY(0.2);
			player.setVelocity(v);
			player.sendMessage("ジャンプしました");
		}*/
		player.playSound(player, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1f, 1.4f);
		player.getNearbyEntities(1, 1, 1).forEach(entity -> {
			if (entity instanceof LivingEntity livingEntity) {
				Integer lastAttackedTick = damagedEntities.get(livingEntity.getUniqueId());
				// 同じエンティティに対して0.5秒未満でダメージを与えないようにする
				if (lastAttackedTick != null && (activeTicks() - lastAttackedTick) < 10) {
					return;
				}
				livingEntity.damage(20);
				damagedEntities.put(livingEntity.getUniqueId(), activeTicks());
			}
		});

		return activeTicks() < 10;
	}

	@Override
	public void onFinish() {
		context.player().sendMessage("stop Skill");
	}
}
