package net.logiench.shardCore.data.item.def.equipment.weapon

import net.kyori.adventure.text.Component
import net.logiench.shardCore.core.item.base.def.Rarity
import net.logiench.shardCore.core.item.base.def.WeaponItem
import net.logiench.shardCore.core.itemRequirement.base.RequirementDef
import net.logiench.shardCore.core.stats.base.AttributeEnum
import net.logiench.shardCore.data.stats.keys.CoreStats
import net.logiench.shardCore.data.stats.keys.player.PlayerStats
import java.util.*

class TestWeapon : WeaponItem() {
	override val id = "test_weapon"

	override val name = Component.text("TestWeapon")

	override val rarity = Rarity.COMMON

	override val uniqueBaseStats = TreeMap<AttributeEnum, Double>().apply {
		put(CoreStats.ATTACK_SPEED, 2.0) // -> 4dps (4Total Damage/s)
		put(CoreStats.FINAL_NATURAL_DAMAGE, 1.5)
		put(CoreStats.FINAL_WATER_DAMAGE, 0.5)
	}

	override val mainStats = TreeMap<AttributeEnum, Double>().apply {
		put(PlayerStats.HP_REGEN_PERCENT, 10.0)
	}

	override val requirementDefs = listOf<RequirementDef<*>>()
}
