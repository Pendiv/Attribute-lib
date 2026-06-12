package net.logiench.logienchlib.bukkit.menu.handler

import net.logiench.logienchlib.api.menu.InventoryMenu
import net.logiench.logienchlib.api.menu.handler.MenuOpenContext
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryOpenEvent

class MenuOpenContextImpl(
	override val player: Player,
	override val menu: InventoryMenu,
	override val originalEvent: InventoryOpenEvent,
) : MenuOpenContext

