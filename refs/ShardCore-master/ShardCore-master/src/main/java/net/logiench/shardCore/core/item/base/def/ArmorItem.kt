package net.logiench.shardCore.core.item.base.def

abstract class ArmorItem(
	itemType: ItemType
) : EquipmentItem(
	itemType
) {
	override val gemSlotSize = 1
}
