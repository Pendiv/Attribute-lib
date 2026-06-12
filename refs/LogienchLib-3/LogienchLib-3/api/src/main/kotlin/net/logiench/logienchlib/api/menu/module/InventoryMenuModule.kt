package net.logiench.logienchlib.api.menu.module

import net.logiench.logienchlib.api.menu.InventoryMenu
import net.logiench.logienchlib.api.menu.handler.InventoryMenuHandler

/**
 * InventoryMenuに機能を追加する拡張モジュールの基底インターフェースです。
 *
 * 複雑な機能（ページング、アニメーションなど）をモジュールとして実装し、
 * [InventoryMenu.addModule] で注入することで使用します。
 *
 * ### 実装例
 * ```kotlin
 * class MyModule : InventoryMenuModule {
 *     override fun onAttach(menu: InventoryMenu) {
 *         // メニューへの初期セットアップ
 *     }
 *     override fun onClick(ctx: MenuClickContext) {
 *         // クリック時の追加処理
 *     }
 * }
 * ```
 */
interface InventoryMenuModule : InventoryMenuHandler {

	/**
	 * このモジュールがメニューに注入されたときに呼ばれます。
	 * 初期アイテムの配置やイベント設定はここで行います。
	 */
	fun onAttach(menu: InventoryMenu)

	/**
	 * [InventoryMenu.refresh] が呼び出されたときに呼ばれます（省略可）。
	 *
	 * モジュールが管理するアイテムを再描画する際に使用します。
	 * 例えば [PageModule] はここで現在ページのアイテムを再配置します。
	 */
	fun onMenuRefresh(menu: InventoryMenu) {}
}
