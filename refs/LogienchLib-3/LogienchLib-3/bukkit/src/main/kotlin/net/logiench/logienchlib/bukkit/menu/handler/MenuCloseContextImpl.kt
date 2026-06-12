package net.logiench.logienchlib.bukkit.menu.handler

import net.logiench.logienchlib.api.menu.InventoryMenu
import net.logiench.logienchlib.api.menu.handler.MenuCloseContext
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryCloseEvent

class MenuCloseContextImpl(
	override val player: Player,
	override val menu: InventoryMenu,
	override val originalEvent: InventoryCloseEvent,
) : MenuCloseContext {

	override val reason: InventoryCloseEvent.Reason = originalEvent.reason
}