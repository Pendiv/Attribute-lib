package net.logiench.shardCore.core.item.system._weapon;

import net.logiench.logienchlibv2.api.minecraft.time.Delay;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class BowAttack implements WeaponAction {

	@Override
	public void attack(Player player) {
		Location location = player.getLocation();
		// 1つめのspeedが矢の速度、早すぎると向きが正常に表示される前に飛んで行って違和感。2つめがたぶんランダムの矢の広がり
		player.playSound(location, Sound.ENTITY_ARROW_SHOOT, 1f, 1f);
		Vector direction = location.getDirection().clone();

		for (int i = 1; i < 20; i++) {
			Arrow arrow = player.getWorld().spawnArrow(player.getEyeLocation(), direction.clone(), 5, 5);
			arrow.setShooter(player);
			arrow.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
			//			Timer.startTimer(a -> arrow.setVelocity(arrow.getVelocity().add(direction.multiply(a+1))), 10, 10, 8);
			Delay.on(arrow::remove, 100);
		}
	}
}