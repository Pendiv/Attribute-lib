package net.logiench.shardCore.data.stats.keys;

import net.logiench.shardCore.core.stats.base.AttributeEnum;

public interface CoreStats {
	AttributeEnum LEVEL = new AttributeEnum("level", "§eLevel: ", "%.0f");

	AttributeEnum MAX_HP = new AttributeEnum("max_hp", "§4HP §7Limit: ", "%.0f");
	AttributeEnum HP_REGEN = new AttributeEnum("hp_regen", "§7HP Regen: ", "%.0f");
	AttributeEnum HP = new AttributeEnum("hp", "§dHP: ", "%.0f");

	AttributeEnum ATTACK_SPEED = new AttributeEnum("attack_speed", "§7Attack Speed: ", "%.0f");

	// Final系のステータスは直接的にsetなどは行わない（内部のステータス処理がないMobを除く）
	AttributeEnum FINAL_NATURAL_DAMAGE = new AttributeEnum("final_natural_damage", "§6Natural §7Damage: ", "%.0f");
	AttributeEnum FINAL_FIRE_DAMAGE = new AttributeEnum("final_fire_damage", "§cFire §7Damage: ", "%.0f");
	AttributeEnum FINAL_WATER_DAMAGE = new AttributeEnum("final_water_damage", "§bWater §7Damage: ", "%.0f");
	AttributeEnum FINAL_EARTH_DAMAGE = new AttributeEnum("final_earth_damage", "§2Earth §7Damage: ", "%.0f");
	AttributeEnum FINAL_HOLY_DAMAGE = new AttributeEnum("final_holy_damage", "§dHoly §7Damage: ", "%.0f");
	AttributeEnum FINAL_DARK_DAMAGE = new AttributeEnum("final_dark_damage", "§5Dark §7Damage: ", "%.0f");

	AttributeEnum FINAL_FIRE_DEFENSE = new AttributeEnum("final_fire_defense", "§cFire §7Defense: ", "%.0f");
	AttributeEnum FINAL_WATER_DEFENSE = new AttributeEnum("final_water_defense", "§bWater §7Defense: ", "%.0f");
	AttributeEnum FINAL_EARTH_DEFENSE = new AttributeEnum("final_earth_defense", "§2Earth §7Defense: ", "%.0f");
	AttributeEnum FINAL_HOLY_DEFENSE = new AttributeEnum("final_holy_defense", "§dHoly §7Defense: ", "%.0f");
	AttributeEnum FINAL_DARK_DEFENSE = new AttributeEnum("final_dark_defense", "§5Dark §7Defense: ", "%.0f");

	AttributeEnum CRITICAL_CHANCE = new AttributeEnum("critical_chance", "§7Critical Chance: ", "%.0f%%");
	AttributeEnum CRITICAL_DAMAGE = new AttributeEnum("critical_damage", "§7Critical Damage: ", "%.0f%%");
	AttributeEnum WALK_SPEED = new AttributeEnum("walk_speed", "§7Walk Speed: ", "%.0f");
}
