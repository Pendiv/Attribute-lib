package net.logiench.logienchlib.api.menu.handler

import net.logiench.logienchlib.api.menu.InventoryMenu
import org.bukkit.entity.Player
import org.bukkit.event.inventory.DragType
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.InventoryView
import org.bukkit.inventory.ItemStack

/**
 * ドラッグイベントのコンテキストです。
 */
interface MenuDragContext {

	/** ドラッグを行ったプレイヤー */
	val player: Player

	/** 現在表示されているインベントリのビュー */
	val view: InventoryView

	/** このイベントが発生したメニュー */
	val menu: InventoryMenu

	/** ドラッグ操作の種類（均等分割 or 1つずつ配置） */
	val dragType: DragType

	/**
	 * ドラッグ後に各スロットに配置されるアイテムのマップ。
	 * キーはrawSlot番号。
	 */
	val newItems: Map<Int, ItemStack>

	/** ドラッグ対象スロットのrawSlot番号セット */
	val rawSlots: Set<Int>

	/** ドラッグがメニュー側（topインベントリ）のスロットに影響するかどうか */
	val affectsTopInventory: Boolean

	/** ドラッグがプレイヤーインベントリ側（bottomインベントリ）のスロットに影響するかどうか */
	val affectsBottomInventory: Boolean

	/** ドラッグ前にカーソルに持っていたアイテム（なければnull） */
	val oldCursor: ItemStack?

	// ----------------------------------------------------------------
	// キャンセル操作
	// ----------------------------------------------------------------

	/** このドラッグイベントをキャンセルする */
	fun cancel()

	/** このドラッグイベントのキャンセルを解除する */
	fun uncancel()

	/** 現在キャンセル状態かどうか */
	val isCancelled: Boolean

	/** 元のBukkitイベント */
	val originalEvent: InventoryDragEvent
}
