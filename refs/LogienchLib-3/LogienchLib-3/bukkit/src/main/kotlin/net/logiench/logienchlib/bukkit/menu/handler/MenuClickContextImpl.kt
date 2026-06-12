package net.logiench.logienchlib.bukkit.menu.handler

import net.logiench.logienchlib.api.menu.InventoryMenu
import net.logiench.logienchlib.api.menu.handler.MenuClickContext
import org.bukkit.entity.Player
import org.bukkit.event.inventory.*
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryView
import org.bukkit.inventory.ItemStack

class MenuClickContextImpl(
	override val player: Player,
	override val menu: InventoryMenu,
	override val originalEvent: InventoryClickEvent,
) : MenuClickContext {

	override val slot: Int = originalEvent.slot
	override val rawSlot: Int = originalEvent.rawSlot
	override val slotType: InventoryType.SlotType = originalEvent.slotType
	override val clickType: ClickType = originalEvent.click
	override val action: InventoryAction = originalEvent.action
	override val clickedInventory: Inventory? = originalEvent.clickedInventory
	override val currentItem: ItemStack?
		get() = originalEvent.currentItem // currentItemはsetできるのでgetterで定義
	override val cursor: ItemStack = originalEvent.cursor
	override val isCursorEmpty: Boolean = cursor.isEmpty
	override val view: InventoryView = originalEvent.view

	override val isTopInventory: Boolean = clickedInventory == view.topInventory
	override val isBottomInventory: Boolean = clickedInventory == view.bottomInventory
	override val isHotbar: Boolean = if (isBottomInventory) (slot in 0..8) else false

	override val isLeftClick: Boolean = originalEvent.isLeftClick
	override val isRightClick: Boolean = originalEvent.isRightClick
	override val isShiftClick: Boolean = originalEvent.isShiftClick

	override val isOffhand: Boolean = clickType == ClickType.SWAP_OFFHAND
	override val isDrop: Boolean = test(
		action,
		InventoryAction.DROP_ALL_SLOT,
		InventoryAction.DROP_ONE_SLOT,
		InventoryAction.DROP_ALL_CURSOR,
		InventoryAction.DROP_ONE_CURSOR
	)
	override val isMoveToOtherInventory: Boolean = action == InventoryAction.MOVE_TO_OTHER_INVENTORY
	override val isPickup: Boolean = test(
		action, InventoryAction.PICKUP_ALL, InventoryAction.PICKUP_ONE,
		InventoryAction.PICKUP_HALF, InventoryAction.PICKUP_SOME
	)
	override val isPlace: Boolean =
		test(action, InventoryAction.PLACE_ALL, InventoryAction.PLACE_ONE, InventoryAction.PLACE_SOME)

	private var cancel: Boolean = false

	/** InventoryActionが指定された引数のどれかにマッチしているかチェックします */
	private fun test(action: InventoryAction, vararg actions: InventoryAction): Boolean = actions.any { it == action }

	override fun cancel() {
		this.cancel = true
	}

	override fun uncancel() {
		this.cancel = false
	}

	override val isCancelled: Boolean
		get() = this.cancel

	override fun closeMenu() {
		player.closeInventory(InventoryCloseEvent.Reason.PLUGIN)
	}

	override fun openMenu(other: InventoryMenu) {
		other.open(player)
	}
}