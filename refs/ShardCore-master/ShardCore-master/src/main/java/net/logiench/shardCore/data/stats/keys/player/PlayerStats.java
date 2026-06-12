package net.logiench.shardCore.data.stats.keys.player;

import net.logiench.shardCore.core.stats.base.AttributeEnum;

public interface PlayerStats {
	AttributeEnum XP_BONUS = new AttributeEnum("xp_bonus", "§7XP Bonus: ", "%.0f%%");
	AttributeEnum ROOT_BONUS = new AttributeEnum("root_bonus", "§7Root Bonus: ", "%.0f%%");

	AttributeEnum MAX_MANA = new AttributeEnum("max_mana", "§bMana §7Limit: ", "%.0f");
	AttributeEnum MANA = new AttributeEnum("mana", "§bMana: ", "%.0f");
	AttributeEnum MANA_REGEN = new AttributeEnum("mana_regen", "§7Mana Regen: ", "%.0f");

	// DAMAGE
	AttributeEnum NATURAL_DAMAGE = new AttributeEnum("natural_damage", "§6Natural §7Damage: ", "%.0f");
	AttributeEnum FIRE_DAMAGE = new AttributeEnum("fire_damage", "§cFire §7Damage: ", "%.0f");
	AttributeEnum WATER_DAMAGE = new AttributeEnum("water_damage", "§bWater §7Damage: ", "%.0f");
	AttributeEnum EARTH_DAMAGE = new AttributeEnum("earth_damage", "§2Earth §7Damage: ", "%.0f");
	AttributeEnum HOLY_DAMAGE = new AttributeEnum("holy_damage", "§dHoly §7Damage: ", "%.0f");
	AttributeEnum DARK_DAMAGE = new AttributeEnum("dark_damage", "§5Dark §7Damage: ", "%.0f");
	// NaturalDamage: プレイヤーとMobで名前を統一したい(わかりにくい), でも、プレイヤーの時はPercentがあるせいで FinalNaturalDamage * NaturalDamagePercent = NaturalDamage が最終結果になる
	// Mobの時はNaturalDamage, CoreStats
	AttributeEnum NATURAL_DAMAGE_PERCENT = new AttributeEnum("final_natural_damage_percent", "§7Final §6Natural §7Damage: ", "%.0f%%");
	AttributeEnum FIRE_DAMAGE_PERCENT = new AttributeEnum("final_fire_damage_percent", "§7Final §cFire §7Damage: ", "%.0f%%");
	AttributeEnum WATER_DAMAGE_PERCENT = new AttributeEnum("final_water_damage_percent", "§7Final §bWater §7Damage: ", "%.0f%%");
	AttributeEnum EARTH_DAMAGE_PERCENT = new AttributeEnum("final_earth_damage_percent", "§7Final §2Earth §7Damage: ", "%.0f%%");
	AttributeEnum HOLY_DAMAGE_PERCENT = new AttributeEnum("final_holy_damage_percent", "§7Final §dHoly §7Damage: ", "%.0f%%");
	AttributeEnum DARK_DAMAGE_PERCENT = new AttributeEnum("final_dark_damage_percent", "§7Final §5Dark §7Damage: ", "%.0f%%");

	// DEFENSE
	AttributeEnum FIRE_DEFENSE = new AttributeEnum("fire_defense", "§cFire §7Defense: ", "%.0f");
	AttributeEnum WATER_DEFENSE = new AttributeEnum("water_defense", "§bWater §7Defense: ", "%.0f");
	AttributeEnum EARTH_DEFENSE = new AttributeEnum("earth_defense", "§2Earth §7Defense: ", "%.0f");
	AttributeEnum HOLY_DEFENSE = new AttributeEnum("holy_defense", "§dHoly §7Defense: ", "%.0f");
	AttributeEnum DARK_DEFENSE = new AttributeEnum("dark_defense", "§5Dark §7Defense: ", "%.0f");

	AttributeEnum FIRE_DEFENSE_PERCENT = new AttributeEnum("final_fire_defense_percent", "§7Final §cFire §7Defense: ", "%.0f%%");
	AttributeEnum WATER_DEFENSE_PERCENT = new AttributeEnum("final_water_defense_percent", "§7Final §bWater §7Defense: ", "%.0f%%");
	AttributeEnum EARTH_DEFENSE_PERCENT = new AttributeEnum("final_earth_defense_percent", "§7Final §2Earth §7Defense: ", "%.0f%%");
	AttributeEnum HOLY_DEFENSE_PERCENT = new AttributeEnum("final_holy_defense_percent", "§7Final §dHoly §7Defense: ", "%.0f%%");
	AttributeEnum DARK_DEFENSE_PERCENT = new AttributeEnum("final_dark_defense_percent", "§7Final §5Dark §7Defense: ", "%.0f%%");

	AttributeEnum MAX_HP_PERCENT = new AttributeEnum("max_hp_percent", "§4HP §7Limit: ", "%.0f%%");

	AttributeEnum HP_REGEN_PERCENT = new AttributeEnum("hp_regen_percent", "§7HP Regen: ", "%.0f%%");

	AttributeEnum MAX_MANA_PERCENT = new AttributeEnum("max_mana_percent", "§bMana §7Limit: ", "%.0f%%");

	AttributeEnum MANA_REGEN_PERCENT = new AttributeEnum("mana_regen_percent", "§7Mana Regen: ", "%.0f%%");

	AttributeEnum SKILL_DAMAGE_PERCENT = new AttributeEnum("skill_damage_percent", "§7Skill Damage: ", "%.0f%%");


	AttributeEnum STRENGTH = new AttributeEnum("strength", "strength: ", "%.0f");
	AttributeEnum INTELLIGENCE = new AttributeEnum("intelligence", "intelligence: ", "%.0f");
	AttributeEnum DEXTERITY = new AttributeEnum("dexterity", "dexterity: ", "%.0f");
}
