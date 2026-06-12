package net.logiench.shardCore.core.damage._indicator;

import net.kyori.adventure.text.Component;
import net.logiench.logienchlibv2.api.minecraft.text.ComponentUtil;
import net.logiench.logienchlibv2.api.minecraft.time.Delay;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;

public class DamageIndicator {
	private String indicator = "";
	private final long damageValue;

	public DamageIndicator(long damageValue) {
		this.damageValue = damageValue;
	}

	public void setCritical() {
		this.indicator = "§e§l✧";
	}

	public void createIndicator(Location location) {
		ArmorStand stand = location.getWorld().spawn(location, ArmorStand.class, armorStand -> {
			Component component = ComponentUtil.text("§7" + indicator + damageValue);

			armorStand.customName(component);
			armorStand.setCustomNameVisible(true);
			armorStand.setInvisible(true);
			armorStand.setInvulnerable(true);
			armorStand.setGravity(false);
			armorStand.setBasePlate(false);
			armorStand.setMarker(true);
		});
		Delay.on(stand::remove, 30);
	}
}
