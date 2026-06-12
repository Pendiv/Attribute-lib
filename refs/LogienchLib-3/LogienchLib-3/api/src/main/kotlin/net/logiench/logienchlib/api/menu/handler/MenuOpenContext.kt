package net.logiench.logienchlib.api.menu.handler

import net.logiench.logienchlib.api.menu.InventoryMenu
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryOpenEvent

/**
 * インベントリが開かれたときのコンテキストです。
 */
interface MenuOpenContext {

	/** メニューを開いたプレイヤー */
	val player: Player

	/** 開かれたメニュー */
	val menu: InventoryMenu

	/** 元のBukkitイベント */
	val originalEvent: InventoryOpenEvent
}
