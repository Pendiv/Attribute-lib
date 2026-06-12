package net.logiench.shardCore.core.item.base.def

import net.logiench.shardCore.core.item.base.module.ItemModule

abstract class WeaponItem : EquipmentItem(ItemType.SWORD) {
	override val gemSlotSize = 2

	override val modules = listOf<Class<out ItemModule<*>>>()
}
