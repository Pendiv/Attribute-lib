package net.logiench.logienchlib.bukkit.menu.handler

import net.logiench.logienchlib.api.menu.InventoryMenu
import net.logiench.logienchlib.api.menu.handler.MenuDragContext
import org.bukkit.entity.Player
import org.bukkit.event.inventory.DragType
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.InventoryView
import org.bukkit.inventory.ItemStack

class MenuDragContextImpl(
	override val player: Player,
	override val menu: InventoryMenu,
	override val originalEvent: InventoryDragEvent,
) : MenuDragContext {

	override val view: InventoryView = originalEvent.view
	override val dragType: DragType = originalEvent.type
	override val newItems: Map<Int, ItemStack> = originalEvent.newItems
	override val rawSlots: Set<Int> = originalEvent.rawSlots
	override val affectsTopInventory: Boolean = run {
		val topInventory = view.topInventory
		return@run rawSlots.any { view.getInventory(it) == topInventory }
	}
	override val affectsBottomInventory: Boolean = run {
		val bottomInventory = view.bottomInventory
		return@run rawSlots.any { view.getInventory(it) == bottomInventory }
	}
	override val oldCursor: ItemStack = originalEvent.oldCursor

	private var cancel: Boolean = false

	override fun cancel() {
		this.cancel = true
	}

	override fun uncancel() {
		this.cancel = false
	}

	override val isCancelled: Boolean
		get() = this.cancel
}