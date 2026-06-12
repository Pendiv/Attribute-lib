package net.logiench.logienchlib.api.menu.handler

/**
 * InventoryMenuに対するイベントをひとまとめに定義するinterfaceです。
 *
 * 繰り返し使用する処理を定義したクラスを作成して使用します。
 */
interface InventoryMenuHandler {

	/** インベントリが開かれたときに呼ばれます */
	fun onOpen(ctx: MenuOpenContext) {}

	/** インベントリがクリックされたときに呼ばれます */
	fun onClick(ctx: MenuClickContext) {}

	/** インベントリでドラッグが行われたときに呼ばれます */
	fun onDrag(ctx: MenuDragContext) {}

	/** インベントリが閉じられたときに呼ばれます */
	fun onClose(ctx: MenuCloseContext) {}
}
