package net.logiench.logienchlib.api.menu

/**
 * インベントリのクリック・ドラッグをキャンセルするポリシーを定義します。
 *
 * ### 使い方
 * ```kotlin
 * // デフォルト（topのみキャンセル）
 * CancelPolicy.DEFAULT
 *
 * // カスタム設定
 * CancelPolicy(
 *     clickCancel = CancelPolicy.ClickCancel.ALL,
 *     dragCancel  = CancelPolicy.DragCancel.NONE
 * )
 * ```
 */
data class CancelPolicy(
	/** クリックイベントのキャンセル対象 */
	val clickCancel: ClickCancel = ClickCancel.TOP_ONLY,
	/** ドラッグイベントのキャンセル対象 */
	val dragCancel: DragCancel = DragCancel.TOP_ONLY,
) {

	/**
	 * クリックイベントのキャンセル対象を表します。
	 */
	enum class ClickCancel {
		/**
		 * メニュー側（topインベントリ）のクリックのみキャンセル（デフォルト）
		 * メニュー側が変更されないためにシフトクリックをすべてキャンセルします
		 */
		TOP_ONLY,

		/**
		 * プレイヤーインベントリ側（bottomインベントリ）のクリックのみキャンセル
		 * メニュー側が変更されないためにシフトクリックをすべてキャンセルします
		 */
		BOTTOM_ONLY,

		/** top/bottom両方のクリックをキャンセル */
		ALL,

		/** クリックをキャンセルしない */
		NONE,
	}

	/**
	 * ドラッグイベントのキャンセル対象を表します。
	 */
	enum class DragCancel {
		/** メニュー側（topインベントリ）スロットが含まれるドラッグのみキャンセル（デフォルト） */
		TOP_ONLY,

		/** プレイヤーインベントリ側（bottomインベントリ）スロットが含まれるドラッグのみキャンセル */
		BOTTOM_ONLY,

		/** すべてのドラッグをキャンセル */
		ALL,

		/** ドラッグをキャンセルしない */
		NONE,
	}

	companion object {
		/** デフォルトポリシー: topのクリックのみキャンセル / topへのドラッグのみキャンセル */
		@JvmField
		val DEFAULT = CancelPolicy()

		/** すべてのクリック・ドラッグをキャンセル */
		@JvmField
		val ALL = CancelPolicy(ClickCancel.ALL, DragCancel.ALL)

		/** キャンセルしない（クリック・ドラッグを自由に通す） */
		@JvmField
		val NONE = CancelPolicy(ClickCancel.NONE, DragCancel.NONE)
	}
}
