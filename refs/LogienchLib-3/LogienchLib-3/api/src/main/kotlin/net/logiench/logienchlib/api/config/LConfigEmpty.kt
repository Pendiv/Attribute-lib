package net.logiench.logienchlib.api.config

import dev.dejvokep.boostedyaml.YamlDocument
import dev.dejvokep.boostedyaml.block.implementation.Section

/**
 * どのファイルにも紐付かない、読み取り専用の空設定オブジェクトです。
 * [LConfig.empty()] から取得します。
 *
 * - 全ての get 系メソッドは null または空コレクションを返します。
 * - set() / createSection() / addDefaults() / rebuildWithDefaults() / save() は
 *   [UnsupportedOperationException] を投げます。
 */
internal object LConfigEmpty : LConfig {

	// =========================================================
	// ナビゲーション（プロパティ）
	// =========================================================

	override val currentPath: String = ""
	override val root: LConfig get() = this
	override val parent: LConfig? = null

	override fun getName(): String = ""

	/** LConfigEmpty はどのツリーにも属さないため false を返します */
	override fun isConfigSection(): Boolean = false

	override fun getSection(path: String): LConfig? = null

	// =========================================================
	// キー・値の列挙
	// =========================================================

	override fun getKeys(deep: Boolean): Set<String> = emptySet()
	override fun getValues(deep: Boolean): Map<String, Any?> = emptyMap()

	// =========================================================
	// 存在確認
	// =========================================================

	override fun contains(path: String): Boolean = false
	override fun isSet(path: String): Boolean = false

	// =========================================================
	// 汎用 get
	// =========================================================

	override fun get(path: String): Any? = null

	// =========================================================
	// 型付き get（全て null を返す）
	// =========================================================

	override fun getString(path: String): String? = null
	override fun getInt(path: String): Int? = null
	override fun getLong(path: String): Long? = null
	override fun getDouble(path: String): Double? = null
	override fun getFloat(path: String): Float? = null
	override fun getBoolean(path: String): Boolean? = null

	// =========================================================
	// リスト型 get（全て空コレクションを返す）
	// =========================================================

	override fun getList(path: String): List<*>? = null
	override fun getStringList(path: String): List<String> = emptyList()
	override fun getIntList(path: String): List<Int> = emptyList()
	override fun getLongList(path: String): List<Long> = emptyList()
	override fun getDoubleList(path: String): List<Double> = emptyList()
	override fun getFloatList(path: String): List<Float> = emptyList()
	override fun getMapList(path: String): List<Map<*, *>> = emptyList()

	// =========================================================
	// 書き込み系（全て例外）
	// =========================================================

	override fun createSection(path: String): LConfig = unsupported()
	override fun createSection(path: String, values: Map<*, *>): LConfig = unsupported()
	override fun asSection(): Section = unsupported()
	override fun asDocument(): YamlDocument = unsupported()
	override fun set(path: String, value: Any?): Unit = unsupported()
	override fun remove(path: String) = unsupported()

	override fun save(): Unit = unsupported()
	override fun saveTo(path: LConfigPath): Unit = unsupported()

	// =========================================================
	// 内部ユーティリティ
	// =========================================================

	private fun unsupported(): Nothing = throw UnsupportedOperationException(
		"LConfig.empty() は読み取り専用です。" +
				"set() / createSection() / addDefaults() / rebuildWithDefaults() / save() は呼び出せません。"
	)

	override fun toString(): String = "LConfig.empty()"
}
