package net.logiench.shardCore.data.item.def.equipment.armor.chestplate

import net.logiench.logienchlibv2.api.minecraft.text.ChatColor
import net.logiench.logienchlibv2.api.minecraft.text.ComponentUtil
import net.logiench.shardCore.core.item.base.def.ChestplateItem
import net.logiench.shardCore.core.item.base.def.Rarity
import net.logiench.shardCore.core.itemRequirement.base.RequirementDef
import net.logiench.shardCore.core.stats.base.AttributeEnum
import net.logiench.shardCore.data.itemRequirement.MaxLevelReqType.MaxLevelDef
import net.logiench.shardCore.data.stats.keys.CoreStats
import java.util.*


class ObsidianChestplate : ChestplateItem() {
	override val id = "obsidian_chestplate"

	override val name = ComponentUtil.text(ChatColor.DARK_PURPLE.toString() + "§l黒曜石の胸当て")!!

	override val rarity = Rarity.MYTHIC

	override val uniqueBaseStats = TreeMap<AttributeEnum, Double>().apply {
		put(CoreStats.ATTACK_SPEED, 10.0)
		put(CoreStats.FINAL_DARK_DAMAGE, 20.0)
	}

	override val mainStats = TreeMap<AttributeEnum, Double>().apply {
		put(CoreStats.WALK_SPEED, 50.0)
	}

	override val requirementDefs = listOf<RequirementDef<*>>(
//		MinLevelDef(5),
		MaxLevelDef(14),
	)
}
