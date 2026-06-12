package net.logiench.logienchlib.api.menu

import net.kyori.adventure.text.Component
import net.logiench.logienchlib.api.InstanceHolder
import net.logiench.logienchlib.api.menu.handler.*
import net.logiench.logienchlib.api.menu.module.InventoryMenuModule
import net.logiench.logienchlib.api.timer.LTaskPool
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import java.util.function.Consumer

/**
 * インベントリGUIメニューのメインインターフェースです。
 *
 * アイテムの配置・クリックイベント登録・タスク管理・モジュール操作を担当します。
 * インベントリ自体の構成（タイトル・サイズ）は [InventoryMenuBuilder] で行います。
 *
 * ---
 *
 * ### Java での使い方
 * ```java
 * InventoryMenu.chest(Component.text("ショップ"), 3)
 *     .cancelPolicy(CancelPolicy.DEFAULT)
 *     .open(player, menu -> {
 *         menu.setItem(13, diamondItem, ctx -> {
 *             ctx.cancel();
 *             ctx.getPlayer().sendMessage("クリック!");
 *         });
 *     });
 * ```
 *
 * ### Kotlin での使い方（DSL）
 * ```kotlin
 * InventoryMenu.chest("ショップ", 3) {
 *     cancelPolicy(CancelPolicy.DEFAULT)
 * }.open(player) {
 *     setItem(1, 5, diamondItem) {
 *         cancel()
 *         player.sendMessage("クリック!")
 *     }
 * }
 * ```
 */
interface InventoryMenu : InventoryHolder {

    // ================================================================
    // スロット操作
    // ================================================================

    /**
     * 指定スロットにアイテムをセットします。
     *
     * @param slot スロット番号（0始まり）
     * @param item セットするアイテム（nullでクリア）
     */
    fun setItem(slot: Int, item: ItemStack?): InventoryMenu

    /**
     * row（行）・col（列）でスロットを指定してアイテムをセットします。
     * row・colはともに1始まりです（例: 1行1列 = スロット0）。
     *
     * @param row 行番号（1始まり）
     * @param col 列番号（1始まり、チェスト型は1〜9）
     * @param item セットするアイテム（nullでクリア）
     */
    fun setItem(row: Int, col: Int, item: ItemStack?): InventoryMenu =
        setItem((row - 1) * 9 + (col - 1), item)

    /**
     * 指定スロットにアイテムとクリックハンドラーをセットします。
     *
     * ### Java での使い方（Consumer）
     * ```java
     * menu.setItem(13, item, ctx -> ctx.cancel());
     * ```
     *
     * @param slot    スロット番号（0始まり）
     * @param item    セットするアイテム（nullでクリア）
     * @param onClick クリック時に実行するConsumer
     */
    fun setItem(slot: Int, item: ItemStack?, onClick: Consumer<MenuClickContext>): InventoryMenu

    /**
     * row/col でスロットを指定してアイテムとクリックハンドラーをセットします。
     *
     * @param row     行番号（1始まり）
     * @param col     列番号（1始まり）
     * @param item    セットするアイテム（nullでクリア）
     * @param onClick クリック時に実行するConsumer
     */
    fun setItem(
        row: Int,
        col: Int,
        item: ItemStack?,
        onClick: Consumer<MenuClickContext>,
    ): InventoryMenu = setItem((row - 1) * 9 + (col - 1), item, onClick)

    /**
     * 複数のスロットに同じアイテムとクリックハンドラーを一括登録します。
     *
     * @param slots   スロット番号の配列
     * @param item    セットするアイテム（nullでクリア）
     * @param onClick クリック時に実行するConsumer
     */
    fun setItems(slots: IntArray, item: ItemStack?, onClick: Consumer<MenuClickContext>): InventoryMenu {
        slots.forEach { setItem(it, item, onClick) }
        return this
    }


    /**
     * Kotlin DSL: [InventoryMenu.setItem] のDSL版。
     * クリックハンドラーをレシーバーラムダで記述できます。
     *
     * ```kotlin
     * menu.setItem(13, diamondItem) {
     *     cancel()
     *     player.sendMessage("クリック!")
     * }
     * ```
     */
    @JvmSynthetic
    fun setItem(
        slot: Int,
        item: ItemStack?,
        onClick: MenuClickContext.() -> Unit,
    ): InventoryMenu = setItem(slot, item, Consumer { onClick(it) })

    /**
     * Kotlin DSL: row/col 指定の [InventoryMenu.setItem] のDSL版。
     *
     * ```kotlin
     * menu.setItem(row = 2, col = 5, item = goldItem) {
     *     cancel()
     * }
     * ```
     */
    @JvmSynthetic
    fun setItem(
        row: Int,
        col: Int,
        item: ItemStack?,
        onClick: MenuClickContext.() -> Unit,
    ): InventoryMenu = setItem(row, col, item, Consumer { onClick(it) })

    /**
     * Kotlin DSL: 複数スロット一括登録の [InventoryMenu.setItems] のDSL版。
     *
     * ```kotlin
     * menu.setItems(intArrayOf(10, 11, 12), glassItem) {
     *     cancel()
     * }
     * ```
     */
    @JvmSynthetic
    fun setItems(
        slots: IntArray,
        item: ItemStack?,
        onClick: MenuClickContext.() -> Unit,
    ): InventoryMenu = setItems(slots, item, Consumer { onClick(it) })

    /**
     * Kotlin DSL: 複数イベントを一度にまとめて登録するショートハンドです。
     * 個別に登録したい場合は [onOpen] / [onClick] / [onDrag] / [onClose] を使用してください。
     *
     * ```kotlin
     * menu.handle(
     *     open  = { player.sendMessage("開いた") },
     *     click = { cancel() },
     * )
     * ```
     */
    @JvmSynthetic
    fun handle(
        open: (MenuOpenContext.() -> Unit)? = null,
        click: (MenuClickContext.() -> Unit)? = null,
        drag: (MenuDragContext.() -> Unit)? = null,
        close: (MenuCloseContext.() -> Unit)? = null,
    ): InventoryMenu {
        open?.let { onOpen(it) }
        click?.let { onClick(it) }
        drag?.let { onDrag(it) }
        close?.let { onClose(it) }
        return this
    }


    /**
     * 複数のスロットに同じアイテムを一括登録します（クリックハンドラーなし）。
     *
     * @param slots スロット番号の配列
     * @param item  セットするアイテム（nullでクリア）
     */
    fun setItems(slots: IntArray, item: ItemStack?): InventoryMenu {
        slots.forEach { setItem(it, item) }
        return this
    }

    /**
     * インベントリの外枠（border）をアイテムで埋めます。
     * この処理は横幅9スロットのインベントリを対象としています。
     *
     * @param item 埋めるアイテム（nullでクリア）
     */
    fun fillBorder9(item: ItemStack?): InventoryMenu

    /**
     * すべてのスロットをアイテムで埋めます。
     *
     * @param item 埋めるアイテム（nullでクリア）
     */
    fun fill(item: ItemStack?): InventoryMenu

    /**
     * 指定スロットのアイテムをクリアします。
     *
     * @param slot クリアするスロット番号
     */
    fun clear(slot: Int): InventoryMenu

    /** すべてのスロットのアイテムをクリアします */
    fun clearAll(): InventoryMenu

    // ================================================================
    // イベントリスナー登録（単体登録）
    // ================================================================

    /**
     * クリックイベントのリスナーを追加します。
     *
     * ### Java
     * ```java
     * menu.onClick(ctx -> ctx.cancel());
     * ```
     *
     * ### Kotlin（DSLは [onClick] 拡張関数を使用）
     */
    fun onClick(action: Consumer<MenuClickContext>): InventoryMenu

    /**
     * openイベントのリスナーを追加します。
     *
     * ### Java
     * ```java
     * menu.onOpen(ctx -> ctx.getPlayer().sendMessage("開いた"));
     * ```
     */
    fun onOpen(action: Consumer<MenuOpenContext>): InventoryMenu

    /**
     * closeイベントのリスナーを追加します。
     *
     * ### Java
     * ```java
     * menu.onClose(ctx -> ctx.getPlayer().sendMessage("閉じた"));
     * ```
     */
    fun onClose(action: Consumer<MenuCloseContext>): InventoryMenu

    /**
     * dragイベントのリスナーを追加します。
     *
     * ### Java
     * ```java
     * menu.onDrag(ctx -> ctx.cancel());
     * ```
     */
    fun onDrag(action: Consumer<MenuDragContext>): InventoryMenu

    // Kotlin DSL版（@JvmSynthetic でJavaから隠蔽）

    @JvmSynthetic
    fun onClick(action: MenuClickContext.() -> Unit): InventoryMenu =
        onClick(Consumer { action(it) })

    @JvmSynthetic
    fun onOpen(action: MenuOpenContext.() -> Unit): InventoryMenu =
        onOpen(Consumer { action(it) })

    @JvmSynthetic
    fun onClose(action: MenuCloseContext.() -> Unit): InventoryMenu =
        onClose(Consumer { action(it) })

    @JvmSynthetic
    fun onDrag(action: MenuDragContext.() -> Unit): InventoryMenu =
        onDrag(Consumer { action(it) })

    // ================================================================
    // イベントハンドラー登録（複数イベントをグループ化して管理）
    // ================================================================

    /**
     * 複数のイベントをまとめた [InventoryMenuHandler] を登録します。
     *
     * 複雑なロジックを名前付きクラスで整理したい場合や、
     * 再利用可能なハンドラーを定義したい場合に使用します。
     * 単体イベントの追加には [onClick] / [onOpen] / [onClose] / [onDrag] を使用してください。
     */
    fun addHandler(handler: InventoryMenuHandler): InventoryMenu

    // ================================================================
    // LTaskPool（標準搭載・ライフサイクル自動管理）
    // ================================================================

    /**
     * このメニューに紐づいた [LTaskPool] です。
     *
     * ここに登録したタスクは、メニューが閉じられると自動でキャンセルされます。
     * 手動でのライフサイクル管理は不要です。
     *
     * ```kotlin
     * Timer.on(0L, 20L, menu.taskPool) {
     *     menu.refresh()  // メニューが閉じると自動停止
     * }
     * ```
     */
    val taskPool: LTaskPool

    // ================================================================
    // モジュール操作
    // ================================================================

    /**
     * 拡張モジュールをメニューに注入します。
     *
     * @param module 注入するモジュール
     */
    fun addModule(module: InventoryMenuModule): InventoryMenu

    /**
     * 指定した型のモジュールを取得します。
     *
     * @param type モジュールのクラス
     * @return 見つかった場合はそのモジュール、なければnull
     */
    fun <T : InventoryMenuModule> getModule(type: Class<T>): T?

    // ================================================================
    // メニュー操作
    // ================================================================

    /**
     * 指定プレイヤーに対してメニューを開きます。
     *
     * @param player 開く対象のプレイヤー
     */
    fun open(player: Player): InventoryMenu

    /**
     * 履行内容が変化したときに登録した再描画コールバックを実行して表示を更新します。
     *
     * [onRefresh] で登録したコールバックと、各モジュールの
     * [net.logiench.logienchlib.api.menu.module.InventoryMenuModule.onMenuRefresh] を呢び出します。
     *
     * ```kotlin
     * // 御典内容を登録しておき、refresh()で再実行
     * menu.onRefresh {
     *     setItem(13, getNewStockItem())
     * }
     * Timer.on(0L, 20L, menu.taskPool) { menu.refresh() }
     * ```
     */
    fun refresh(): InventoryMenu

    // ================================================================
    // 再描画リスナー登録
    // ================================================================

    /**
     * [refresh] が呼び出されたときに実行する再描画コールバックを登録します。
     *
     * 登録したコールバック内で [setItem] などを呼び出すことで、動的なアイテム更新が可能です。
     *
     * ### Java
     * ```java
     * menu.onRefresh(m -> m.setItem(13, getNewStockItem()));
     * ```
     *
     * ### Kotlin（DSLは [onRefresh] 拡張関数を使用）
     */
    fun onRefresh(action: Consumer<InventoryMenu>): InventoryMenu

    fun onRefresh(action: Runnable): InventoryMenu =
        onRefresh(Consumer { action.run() })

    @JvmSynthetic
    fun onRefresh(action: InventoryMenu.() -> Unit): InventoryMenu =
        onRefresh(Consumer { it.action() })

    // ================================================================
    // ファクトリ（companion object）
    // ================================================================

    companion object {

        /**
         * チェスト型インベントリのBuilderを作成します（Componentタイトル）。
         *
         * @param title タイトル
         * @param rows  行数（1〜6）
         */
        @JvmStatic
        fun chest(title: Component, rows: Int): InventoryMenuBuilder =
            InstanceHolder.inventoryMenuService.chest(title, rows)

        /**
         * チェスト型インベントリのBuilderを作成します（Stringタイトル）。
         *
         * @param title タイトル文字列
         * @param rows  行数（1〜6）
         */
        @JvmStatic
        fun chest(title: String, rows: Int): InventoryMenuBuilder =
            InstanceHolder.inventoryMenuService.chest(title, rows)

        /**
         * 任意の [InventoryType] を指定してBuilderを作成します。
         *
         * @param type  インベントリタイプ
         * @param title タイトル
         */
        @JvmStatic
        fun of(type: InventoryType, title: Component): InventoryMenuBuilder =
            InstanceHolder.inventoryMenuService.of(type, title)

        // ---- Kotlin DSL専用ファクトリ（@JvmSynthetic でJavaから隠蔽） ----

        /**
         * Kotlin DSL: チェスト型Builderを生成し、[block] でBuilderをセットアップして返します。
         *
         * ```kotlin
         * InventoryMenu.chest("ショップ", 3) {
         *     cancelPolicy(CancelPolicy.DEFAULT)
         * }.open(player) {
         *     setItem(1, 1, item) { cancel() }
         * }
         * ```
         */
        @JvmSynthetic
        fun chest(
            title: Component,
            rows: Int,
            block: InventoryMenuBuilder.() -> Unit,
        ): InventoryMenuBuilder = chest(title, rows).apply(block)

        /**
         * Kotlin DSL: チェスト型Builderを生成し、[block] でBuilderをセットアップして返します（Stringタイトル）。
         */
        @JvmSynthetic
        fun chest(
            title: String,
            rows: Int,
            block: InventoryMenuBuilder.() -> Unit,
        ): InventoryMenuBuilder = chest(title, rows).apply(block)

        /**
         * Kotlin DSL: [InventoryType] を指定したBuilderを生成し、[block] でセットアップして返します。
         */
        @JvmSynthetic
        fun of(
            type: InventoryType,
            title: Component,
            block: InventoryMenuBuilder.() -> Unit,
        ): InventoryMenuBuilder = of(type, title).apply(block)
    }
}

/**
 * Kotlin inline: 指定型のモジュールをreified型で取得します。
 *
 * ```kotlin
 * val page = menu.getModule<PageModule>()
 * ```
 */
@JvmSynthetic
inline fun <reified T : InventoryMenuModule> InventoryMenu.getModule(): T? =
    getModule(T::class.java)
