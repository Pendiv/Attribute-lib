package net.logiench.logienchlib.bukkit.menu

import net.kyori.adventure.text.Component
import net.logiench.logienchlib.api.menu.CancelPolicy
import net.logiench.logienchlib.api.menu.InventoryMenu
import net.logiench.logienchlib.api.menu.InventoryMenuBuilder
import net.logiench.logienchlib.api.menu.handler.InventoryMenuHandler
import net.logiench.logienchlib.api.menu.module.InventoryMenuModule
import org.bukkit.Bukkit
import org.bukkit.event.inventory.InventoryType

class InventoryMenuBuilderImpl private constructor(
	private val title: Component,
	private val type: InventoryType?,
	private val size: Int,
	private val menuManager: InventoryMenuManager
) : InventoryMenuBuilder {

	constructor(title: Component, size: Int, menuManager: InventoryMenuManager) : this(title, null, size, menuManager)

	constructor(title: Component, type: InventoryType, menuManager: InventoryMenuManager) : this(
		title,
		type,
		type.defaultSize,
		menuManager
	)

	private var cancelPolicy: CancelPolicy = CancelPolicy.DEFAULT
	private val handlers: MutableList<InventoryMenuHandler> = mutableListOf()
	private val modules: MutableList<InventoryMenuModule> = mutableListOf()

	override fun cancelPolicy(policy: CancelPolicy): InventoryMenuBuilder {
		this.cancelPolicy = policy
		return this
	}

	override fun addHandler(handler: InventoryMenuHandler): InventoryMenuBuilder {
		handlers += handler
		return this
	}

	override fun addModule(module: InventoryMenuModule): InventoryMenuBuilder {
		modules += module
		return this
	}

	override fun build(): InventoryMenu {
		val inventory = if (type == null) {
			Bukkit.createInventory(null, size, title)
		} else {
			Bukkit.createInventory(null, type, title)
		}

		return InventoryMenuImpl(inventory, cancelPolicy, menuManager).apply {
			handlers.forEach { addHandler(it) }
			modules.forEach { addModule(it) }
		}
	}
}