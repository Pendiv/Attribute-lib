package net.logiench.logienchlib.bukkit.menu

import jakarta.inject.Singleton
import net.kyori.adventure.text.Component
import net.logiench.logienchlib.api.internal.InventoryMenuService
import net.logiench.logienchlib.api.menu.InventoryMenu
import net.logiench.logienchlib.api.menu.InventoryMenuBuilder
import org.bukkit.event.inventory.InventoryType
import java.util.*

/**
 * [InventoryMenuService] の実装クラスです。
 * [InventoryMenuManager] をインジェクションし、生成するビルダーへ伝搬させます。
 */
@Singleton
class InventoryMenuServiceImpl(
	private val menuManager: InventoryMenuManager
) : InventoryMenuService {

	override fun chest(title: Component, rows: Int): InventoryMenuBuilder {
		return InventoryMenuBuilderImpl(title, rows * 9, menuManager)
	}

	override fun chest(title: String, rows: Int): InventoryMenuBuilder {
		return InventoryMenuBuilderImpl(Component.text(title), rows * 9, menuManager)
	}

	override fun of(type: InventoryType, title: Component): InventoryMenuBuilder {
		return InventoryMenuBuilderImpl(title, type, menuManager)
	}

	override fun get(playerId: UUID): InventoryMenu? {
		return menuManager.getMenu(playerId)
	}
}
