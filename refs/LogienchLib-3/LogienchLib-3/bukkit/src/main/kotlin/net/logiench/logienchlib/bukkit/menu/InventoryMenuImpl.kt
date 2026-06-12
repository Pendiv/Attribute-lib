package net.logiench.logienchlib.bukkit.menu

import net.logiench.logienchlib.api.menu.CancelPolicy
import net.logiench.logienchlib.api.menu.InventoryMenu
import net.logiench.logienchlib.api.menu.handler.*
import net.logiench.logienchlib.api.menu.module.InventoryMenuModule
import net.logiench.logienchlib.api.timer.LTaskPool
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.function.Consumer

class InventoryMenuImpl(
	private val inventory: Inventory,
	val cancelPolicy: CancelPolicy,
	private val menuManager: InventoryMenuManager
) : InventoryMenu {

	internal val openListeners: MutableList<Consumer<MenuOpenContext>> = mutableListOf()
	internal val closeListeners: MutableList<Consumer<MenuCloseContext>> = mutableListOf()
	internal val clickListeners: MutableList<Consumer<MenuClickContext>> = mutableListOf()
	internal val dragListeners: MutableList<Consumer<MenuDragContext>> = mutableListOf()
	internal val refreshListeners: MutableList<Consumer<InventoryMenu>> = mutableListOf()

	internal val modules: MutableList<InventoryMenuModule> = mutableListOf()
	internal val slotEvents: MutableMap<Int, Consumer<MenuClickContext>> = mutableMapOf()

	override val taskPool: LTaskPool = LTaskPool.create()
	override fun getInventory(): Inventory = inventory

	override fun setItem(
		slot: Int,
		item: ItemStack?
	): InventoryMenu {
		inventory.setItem(slot, item)
		return this
	}

	override fun setItem(
		slot: Int,
		item: ItemStack?,
		onClick: Consumer<MenuClickContext>
	): InventoryMenu {
		slotEvents[slot] = onClick
		return setItem(slot, item)
	}

	override fun fillBorder9(item: ItemStack?): InventoryMenu {
		val size = inventory.size
		// インベントリの総スロット数から行数を計算 (1行あたり9スロット)
		val rows = size / 9

		// 1行未満、またはチェスト型ではない特殊なインベントリの場合は処理スキップ
		if (rows < 1 || size % 9 != 0) {
			return this
		}

		val lastRowStart = (rows - 1) * 9 // 最下行の開始インデックス

		for (i in 0 until size) {
			// 条件：最上行 | 最下行 | 左端 | 右端
			val isTopRow = i < 9
			val isBottomRow = i >= lastRowStart
			val isLeftEdge = i % 9 == 0
			val isRightEdge = i % 9 == 8

			if (isTopRow || isBottomRow || isLeftEdge || isRightEdge) {
				inventory.setItem(i, item)
			}
		}

		return this
	}

	override fun fill(item: ItemStack?): InventoryMenu {
		inventory.contents = Array(inventory.size) { item }
		return this
	}

	override fun clear(slot: Int): InventoryMenu {
		inventory.clear(slot)
		return this
	}

	override fun clearAll(): InventoryMenu {
		inventory.clear()
		return this
	}

	override fun onClick(action: Consumer<MenuClickContext>): InventoryMenu {
		clickListeners += action
		return this
	}

	override fun onOpen(action: Consumer<MenuOpenContext>): InventoryMenu {
		openListeners += action
		return this
	}

	override fun onClose(action: Consumer<MenuCloseContext>): InventoryMenu {
		closeListeners += action
		return this
	}

	override fun onDrag(action: Consumer<MenuDragContext>): InventoryMenu {
		dragListeners += action
		return this
	}

	override fun addHandler(handler: InventoryMenuHandler): InventoryMenu {
		openListeners + Consumer { handler.onOpen(it) }
		clickListeners + Consumer { handler.onClick(it) }
		dragListeners + Consumer { handler.onDrag(it) }
		closeListeners + Consumer { handler.onClose(it) }
		return this
	}

	override fun addModule(module: InventoryMenuModule): InventoryMenu {
		modules += module
		module.onAttach(this)
		return this
	}

	override fun <T : InventoryMenuModule> getModule(type: Class<T>): T? {
		@Suppress("UNCHECKED_CAST")
		return modules.find { type.isInstance(it) } as? T
	}

	override fun open(player: Player): InventoryMenu {
		menuManager.register(player, this)
		player.openInventory(inventory)
		return this
	}

	override fun refresh(): InventoryMenu {
		refreshListeners.forEach { it.accept(this) }
		modules.forEach { it.onMenuRefresh(this) }
		return this
	}

	override fun onRefresh(action: Consumer<InventoryMenu>): InventoryMenu {
		refreshListeners += action
		return this
	}
}