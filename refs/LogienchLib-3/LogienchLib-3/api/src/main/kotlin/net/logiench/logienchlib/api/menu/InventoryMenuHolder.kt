package net.logiench.logienchlib.api.menu

import net.logiench.logienchlib.api.InstanceHolder
import java.util.*

object InventoryMenuHolder {

	/**
	 * プレイヤーが現在開いている[InventoryMenu]を取得します。
	 */
	fun getMenu(playerId: UUID): InventoryMenu? =
		InstanceHolder.inventoryMenuService.get(playerId)

	/**
	 * プレイヤーが現在[InventoryMenu]を開いているかを取得します。
	 */
	fun isOpenMenu(playerId: UUID): Boolean =
		getMenu(playerId) != null
}