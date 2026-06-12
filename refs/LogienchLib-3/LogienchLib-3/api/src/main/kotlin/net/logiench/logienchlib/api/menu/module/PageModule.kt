package net.logiench.logienchlib.api.menu.module

import net.logiench.logienchlib.api.menu.handler.MenuClickContext
import org.bukkit.inventory.ItemStack
import java.util.function.Consumer

/**
 * ページ分割機能を提供する標準モジュールです。
 *
 * [InventoryMenuModule] を実装しており、[net.logiench.logienchlib.api.menu.InventoryMenu.addModule]
 * で注入することで使用できます。
 *
 * ### 使い方（Kotlin）
 * ```kotlin
 * menu.addModule(
 *     PageModule.create()
 *         .contentSlots(intArrayOf(10, 11, 12, 13, 14, 15, 16))
 *         .contents(items)
 *         .nextButton(slot = 26, item = nextItem)
 *         .previousButton(slot = 18, item = prevItem)
 * )
 * ```
 *
 * ### 使い方（Java）
 * ```java
 * menu.addModule(
 *     PageModule.create()
 *         .contentSlots(new int[]{10, 11, 12, 13, 14, 15, 16})
 *         .contents(items)
 *         .nextButton(26, nextItem)
 *         .previousButton(18, prevItem)
 * );
 * ```
 *
 * > **Note**: コンテンツアイテムの詳細な型は仕様確定後に実装されます。
 */
interface PageModule : InventoryMenuModule {

	/** 現在のページ番号（0始まり） */
	val currentPage: Int

	/** 総ページ数 */
	val totalPages: Int

	/**
	 * 次のページに移動します。
	 * @return 移動できた場合はtrue、すでに最後のページの場合はfalse
	 */
	fun nextPage(): Boolean

	/**
	 * 前のページに移動します。
	 * @return 移動できた場合はtrue、すでに最初のページの場合はfalse
	 */
	fun previousPage(): Boolean

	/**
	 * 指定ページに移動します。
	 * @param page 0始まりのページ番号
	 * @return 移動できた場合はtrue、範囲外の場合はfalse
	 */
	fun goToPage(page: Int): Boolean

	/**
	 * コンテンツを表示するスロット番号の一覧を設定します。
	 * ここに指定したスロットにページのコンテンツが順番に配置されます。
	 */
	fun contentSlots(slots: IntArray): PageModule

	/**
	 * 表示するコンテンツのアイテムリストを設定します。
	 * 各アイテムにクリックハンドラーを付与できます。
	 */
	fun contents(items: List<Pair<ItemStack, Consumer<MenuClickContext>>>): PageModule

	/**
	 * 「次のページへ」ボタンのスロットとアイテムを設定します。
	 * ボタンをクリックすると自動的に [nextPage] が呼ばれます。
	 */
	fun nextButton(slot: Int, item: ItemStack): PageModule

	/**
	 * 「前のページへ」ボタンのスロットとアイテムを設定します。
	 * ボタンをクリックすると自動的に [previousPage] が呼ばれます。
	 */
	fun previousButton(slot: Int, item: ItemStack): PageModule

	companion object {
		/**
		 * PageModuleを新規作成します。
		 *
		 * @return 設定前の空のPageModuleインスタンス
		 */
		@JvmStatic
		fun create(): PageModule = TODO("InstanceHolderから実装が注入されます")
	}
}

// ----------------------------------------------------------------
// Kotlin DSL拡張（@JvmSynthetic でJavaから隠蔽）
// ----------------------------------------------------------------

/**
 * Kotlin DSL: クリックハンドラーをレシーバーラムダで指定できる [PageModule.contents] の拡張。
 */
@JvmSynthetic
fun PageModule.contents(
	items: List<Pair<ItemStack, MenuClickContext.() -> Unit>>,
): PageModule = contents(items.map { (item, handler) ->
	item to Consumer<MenuClickContext> { handler(it) }
})
