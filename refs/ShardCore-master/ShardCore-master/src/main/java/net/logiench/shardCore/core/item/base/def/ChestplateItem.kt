package net.logiench.shardCore.core.item.base.def

import net.logiench.shardCore.data.item.module.gem.GemModule
import net.logiench.shardCore.data.item.module.level.LevelModule
import net.logiench.shardCore.data.item.module.name.NameModule
import net.logiench.shardCore.data.item.module.prefix.PrefixModule
import net.logiench.shardCore.data.item.module.rarity.RarityModule
import net.logiench.shardCore.data.item.module.requirement.EquipmentReqModule
import net.logiench.shardCore.data.item.module.stats.MainStatsModule
import net.logiench.shardCore.data.item.module.stats.SubStatsModule
import net.logiench.shardCore.data.item.module.stats.UniqueStatsModule

abstract class ChestplateItem : ArmorItem(ItemType.CHEST_PLATE) {

	override val modules = listOf(
		LevelModule::class.java,
		UniqueStatsModule::class.java,
		MainStatsModule::class.java,
		EquipmentReqModule::class.java,
		GemModule::class.java,
		SubStatsModule::class.java,
		PrefixModule::class.java,
		RarityModule::class.java,
		NameModule::class.java
	)
}
