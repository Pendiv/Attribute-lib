package net.logiench.logienchlib.api.menu

import net.logiench.logienchlib.api.menu.handler.InventoryMenuHandler
import net.logiench.logienchlib.api.menu.module.InventoryMenuModule
import org.bukkit.entity.Player
import java.util.function.Consumer

/**
 * InventoryMenuを構築するBuilderインターフェースです。
 *
 * タイトル・サイズ・CancelPolicyなど「インベントリ自体の構造的設定」のみを担当します。
 * イベントリスナーの登録やアイテム配置は、[build] または [open] で取得した [InventoryMenu] 側で行います。
 *
 * ---
 *
 * ### Java での使い方（メソッドチェーン）
 * ```java
 * InventoryMenu.chest(Component.text("ショップ"), 3)
 *     .cancelPolicy(CancelPolicy.DEFAULT)
 *     .open(player, menu -> {
 *         menu.setItem(13, diamondItem, ctx -> ctx.cancel());
 *         menu.onClose(ctx -> ctx.getPlayer().sendMessage("閉じた"));
 *     });
 * ```
 *
 * ### Kotlin での使い方（DSL）
 * ```kotlin
 * InventoryMenu.chest("ショップ", 3) {
 *     cancelPolicy(CancelPolicy.DEFAULT)
 * }.open(player) {
 *     setItem(1, 2, diamondItem) { cancel() }
 *     onClose { player.sendMessage("閉じた") }
 * }
 * ```
 */
interface InventoryMenuBuilder {

	// ================================================================
	// 構造的設定（インベントリ自体の定義）
	// ================================================================

	/**
	 * キャンセルポリシーを設定します。
	 * デフォルトは [CancelPolicy.DEFAULT]（topのみキャンセル）です。
	 */
	fun cancelPolicy(policy: CancelPolicy): InventoryMenuBuilder

	/**
	 * 複数のイベントをまとめた [InventoryMenuHandler] をBuilder段階で追加します。
	 *
	 * 再利用可能なハンドラークラスをメニューの構造定義の一部として組み込む場合に使用します。
	 * 単体イベントの追加は [open] 後の [InventoryMenu] 側で [InventoryMenu.onClick] 等を使用してください。
	 */
	fun addHandler(handler: InventoryMenuHandler): InventoryMenuBuilder

	/**
	 * モジュールをBuilder段階で追加します。
	 */
	fun addModule(module: InventoryMenuModule): InventoryMenuBuilder

	// ================================================================
	// ビルド・オープン
	// ================================================================

	/**
	 * [InventoryMenu] を生成して返します。
	 * イベント登録やアイテム配置は返された [InventoryMenu] 側で行います。
	 */
	fun build(): InventoryMenu

	/**
	 * [build] を行い、[setup] でメニューをセットアップしてから、指定プレイヤーに対して開きます。
	 *
	 * `build() → アイテム操作 → open()` の流れを1ステップで完結させるためのメソッドです。
	 *
	 * ### Java での使い方（Consumer）
	 * ```java
	 * builder.open(player, menu -> {
	 *     menu.setItem(13, item, ctx -> ctx.cancel());
	 *     menu.onClose(ctx -> ctx.getPlayer().sendMessage("閉じた"));
	 * });
	 * ```
	 *
	 * @param player 開く対象のプレイヤー
	 * @param setup  build後のメニューに対して実行するセットアップ処理
	 * @return 生成・表示されたInventoryMenu
	 */
	fun open(player: Player, setup: Consumer<InventoryMenu>): InventoryMenu {
		val menu = build()
		setup.accept(menu)
		return menu.open(player)
	}

	/**
	 * Kotlin DSL: [open] のDSL版。
	 *
	 * `build()` 後の [InventoryMenu] をレシーバーとするラムダでセットアップできます。
	 *
	 * ```kotlin
	 * InventoryMenu.chest("ショップ", 3) {
	 *     cancelPolicy(CancelPolicy.DEFAULT)
	 * }.open(player) {
	 *     // this: InventoryMenu
	 *     setItem(1, 2, diamondItem) { cancel() }
	 *     onClick { cancel() }
	 * }
	 * ```
	 */
	@JvmSynthetic
	fun open(player: Player, setup: InventoryMenu.() -> Unit): InventoryMenu =
		open(player, Consumer { setup(it) })

	/**
	 * セットアップなしで [build] → [InventoryMenu.open] を行います。
	 *
	 * @param player 開く対象のプレイヤー
	 * @return 生成・表示されたInventoryMenu
	 */
	fun open(player: Player): InventoryMenu = open(player, Consumer { })
}
