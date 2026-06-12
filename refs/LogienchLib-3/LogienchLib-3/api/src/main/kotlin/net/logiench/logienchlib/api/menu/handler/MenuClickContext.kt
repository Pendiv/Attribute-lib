package net.logiench.logienchlib.api.menu.handler

import net.logiench.logienchlib.api.menu.InventoryMenu
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryView
import org.bukkit.inventory.ItemStack

/**
 * クリックイベントのコンテキストです。
 *
 * BukkitのInventoryClickEventをラップし、よく使う判定・操作をまとめています。
 * 元のイベントへのアクセスは [originalEvent] から行えます。
 */
interface MenuClickContext {

	// ----------------------------------------------------------------
	// 基本情報
	// ----------------------------------------------------------------

	/** クリックしたプレイヤー */
	val player: Player

	/**
	 * クリックされたスロット番号。
	 * メニュー（top）側のスロットは 0 始まり。
	 * プレイヤーインベントリ（bottom）側は [rawSlot] で判断できます。
	 */
	val slot: Int

	/** インベントリビュー全体での生スロット番号 */
	val rawSlot: Int

	/** クリックされたスロットの種類 */
	val slotType: InventoryType.SlotType

	/** クリックの種類 */
	val clickType: ClickType

	/** 動作の種類 */
	val action: InventoryAction

	/** クリックされたインベントリ（インベントリ外をクリックしていればnull） */
	val clickedInventory: Inventory?

	/** クリックされたスロットに入っていたアイテム（なければnull） */
	val currentItem: ItemStack?

	/** カーソルに持っているアイテム（なければ[ItemStack.isEmpty]がtrue） */
	val cursor: ItemStack

	/** カーソルに持っているアイテムがないか */
	val isCursorEmpty: Boolean

	/** 現在表示されているインベントリのビュー */
	val view: InventoryView

	/** このイベントが発生したメニュー */
	val menu: InventoryMenu

	// ----------------------------------------------------------------
	// 位置判定（よく使う条件分岐）
	// ----------------------------------------------------------------

	/** クリックされたスロットがメニュー側（topインベントリ）かどうか */
	val isTopInventory: Boolean

	/** クリックされたスロットがプレイヤーインベントリ側（bottomインベントリ）かどうか */
	val isBottomInventory: Boolean

	/** クリックされたスロットがホットバーかどうか */
	val isHotbar: Boolean

	// ----------------------------------------------------------------
	// クリック種別の判定
	// ----------------------------------------------------------------

	val isLeftClick: Boolean
	val isRightClick: Boolean
	val isShiftClick: Boolean

	// ----------------------------------------------------------------
	// 動作の判定
	// ----------------------------------------------------------------

	val isOffhand: Boolean
	val isDrop: Boolean
	val isMoveToOtherInventory: Boolean
	val isPickup: Boolean
	val isPlace: Boolean

	// ----------------------------------------------------------------
	// キャンセル操作
	// ----------------------------------------------------------------

	/** このクリックイベントをキャンセルする */
	fun cancel()

	/** このクリックイベントのキャンセルを解除する */
	fun uncancel()

	/** 現在キャンセル状態かどうか */
	val isCancelled: Boolean

	// ----------------------------------------------------------------
	// メニュー操作
	// ----------------------------------------------------------------

	/** メニューを閉じる */
	fun closeMenu()

	/**
	 * 別のメニューを開く。
	 * 現在のメニューは自動的に閉じられます。
	 */
	fun openMenu(other: InventoryMenu)

	// ----------------------------------------------------------------
	// 元イベントへのアクセス
	// ----------------------------------------------------------------

	/** 元のBukkitイベント。高度な操作が必要な場合に使用してください。 */
	val originalEvent: InventoryClickEvent
}
