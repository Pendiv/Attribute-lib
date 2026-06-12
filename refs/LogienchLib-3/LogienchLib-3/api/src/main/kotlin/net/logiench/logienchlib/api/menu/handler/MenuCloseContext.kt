package net.logiench.logienchlib.api.menu.handler

import net.logiench.logienchlib.api.menu.InventoryMenu
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryCloseEvent

/**
 * インベントリが閉じられたときのコンテキストです。
 */
interface MenuCloseContext {

	/** メニューを閉じたプレイヤー */
	val player: Player

	/** 閉じられたメニュー */
	val menu: InventoryMenu

	/** クローズの理由 */
	val reason: InventoryCloseEvent.Reason

	/** 元のBukkitイベント */
	val originalEvent: InventoryCloseEvent
}
