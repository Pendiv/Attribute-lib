package net.logiench.logienchlib.api.config

import dev.dejvokep.boostedyaml.YamlDocument
import dev.dejvokep.boostedyaml.block.implementation.Section

interface LConfig {

	companion object {
		/**
		 * どのファイルにも紐付かない、読み取り専用の空設定オブジェクトを返します。
		 *
		 * - 全ての get 系メソッドは null または空コレクションを返します。
		 * - set() / createSection() / save() を呼ぶと [UnsupportedOperationException] が発生します。
		 *
		 * 主な用途:
		 * - getSection() が null を返した場合の null 安全なデフォルト値
		 * - rebuildWithDefaults 等でユーザー設定が存在しないセクションのプレースホルダー
		 */
		fun empty(): LConfig = LConfigEmpty
	}

	// =========================================================
	// セクションのナビゲーション
	// =========================================================

	/**
	 * このセクションの名前（ドット区切りパスの末尾キー）を返します。
	 * ルートセクションの場合は空文字を返します。
	 */
	fun getName(): String

	/**
	 * ルートから自身までの完全なドット区切りパスを返します。
	 * ルートセクションの場合は空文字を返します。
	 */
	val currentPath: String

	/**
	 * この設定ツリーのルートセクションを返します。
	 * 自身がルートの場合は自分自身を返します。
	 */
	val root: LConfig

	/**
	 * 親セクションを返します。
	 * 自身がルートの場合は null を返します。
	 */
	val parent: LConfig?

	/**
	 * この LConfig インスタンス自体がサブセクション（ルートではない）かどうかを返します。
	 * ルートなら false、サブセクションなら true を返します。
	 */
	fun isConfigSection(): Boolean = parent != null

	/**
	 * 指定したドット区切りのパスでサブセクションを取得します。
	 * 存在しない場合、またはそのパスの値がセクションでない場合は null を返します。
	 */
	fun getSection(path: String): LConfig?

	/**
	 * 指定したパスのサブセクションを取得します。
	 * 存在しない場合はセクションを新規作成して返します。
	 */
	fun getOrCreateSection(path: String): LConfig = getSection(path) ?: createSection(path)

	// =========================================================
	// キー・値の列挙
	// =========================================================

	/**
	 * このセクション直下のキー一覧を返します。
	 * @param deep true の場合は全子孫のキーをドット区切りで再帰的に取得します
	 */
	fun getKeys(deep: Boolean): Set<String>

	/**
	 * このセクションのキーと値のマップを返します。
	 * @param deep true の場合は全子孫を再帰的に含めます。サブセクションは LConfig として格納されます
	 */
	fun getValues(deep: Boolean): Map<String, Any?>

	// =========================================================
	// 存在確認・型確認
	// =========================================================

	/**
	 * 指定したドット区切りのパスに非 null の値が存在するかを返します。
	 */
	fun contains(path: String): Boolean

	/**
	 * 指定したパスに値がセットされているかを返します。
	 * null が明示的にセットされている場合も true を返します（[contains] との違い）。
	 */
	fun isSet(path: String): Boolean

	/** 指定パスの値が文字列かどうかを返します */
	fun isString(path: String): Boolean = get(path) is String

	/** 指定パスの値が Int かどうかを返します */
	fun isInt(path: String): Boolean = get(path) is Int

	/** 指定パスの値が Boolean かどうかを返します */
	fun isBoolean(path: String): Boolean = get(path) is Boolean

	/** 指定パスの値が Double かどうかを返します */
	fun isDouble(path: String): Boolean = get(path) is Double

	/** 指定パスの値が Long かどうかを返します */
	fun isLong(path: String): Boolean = get(path) is Long

	/** 指定パスの値がリストかどうかを返します */
	fun isList(path: String): Boolean = get(path) is List<*>

	/** 指定パスの値がサブセクション（マップ）かどうかを返します */
	fun isSection(path: String): Boolean = get(path) is LConfig

	// =========================================================
	// 汎用 get
	// =========================================================

	/**
	 * 指定パスの値を Any? として取得します。
	 * 存在しない場合は null を返します。
	 */
	fun get(path: String): Any?

	/**
	 * 指定パスの値を取得します。
	 * 存在しない場合または null の場合は def を返します。
	 */
	fun get(path: String, def: Any?): Any? = get(path) ?: def

	// =========================================================
	// String
	// =========================================================

	/** 文字列を取得します。存在しない場合は null を返します */
	fun getString(path: String): String?

	/** 文字列を取得します。存在しない場合はデフォルト値を返します */
	fun getString(path: String, def: String): String = getString(path) ?: def

	// =========================================================
	// Int
	// =========================================================

	/** Int 値を取得します。存在しない場合は null を返します */
	fun getInt(path: String): Int?

	/** Int 値を取得します。存在しない場合はデフォルト値を返します */
	fun getInt(path: String, def: Int): Int = getInt(path) ?: def

	// =========================================================
	// Long
	// =========================================================

	/** Long 値を取得します。存在しない場合は null を返します */
	fun getLong(path: String): Long?

	/** Long 値を取得します。存在しない場合はデフォルト値を返します */
	fun getLong(path: String, def: Long): Long = getLong(path) ?: def

	// =========================================================
	// Double
	// =========================================================

	/** Double 値を取得します。存在しない場合は null を返します */
	fun getDouble(path: String): Double?

	/** Double 値を取得します。存在しない場合はデフォルト値を返します */
	fun getDouble(path: String, def: Double): Double = getDouble(path) ?: def

	// =========================================================
	// Float
	// =========================================================

	/** Float 値を取得します。存在しない場合は null を返します */
	fun getFloat(path: String): Float?

	/** Float 値を取得します。存在しない場合はデフォルト値を返します */
	fun getFloat(path: String, def: Float): Float = getFloat(path) ?: def

	// =========================================================
	// Boolean
	// =========================================================

	/** Boolean 値を取得します。存在しない場合は null を返します */
	fun getBoolean(path: String): Boolean?

	/** Boolean 値を取得します。存在しない場合はデフォルト値を返します */
	fun getBoolean(path: String, def: Boolean): Boolean = getBoolean(path) ?: def

	// =========================================================
	// リスト型
	// =========================================================

	/**
	 * 汎用リストを取得します。存在しない場合は null を返します。
	 * 要素の型は実装依存です。
	 */
	fun getList(path: String): List<*>?

	/** 文字列のリストを取得します。セクションがない場合は空のリストを返します */
	fun getStringList(path: String): List<String>

	/** Int のリストを取得します。セクションがない場合は空のリストを返します */
	fun getIntList(path: String): List<Int>

	/** Long のリストを取得します。セクションがない場合は空のリストを返します */
	fun getLongList(path: String): List<Long>

	/** Double のリストを取得します。セクションがない場合は空のリストを返します */
	fun getDoubleList(path: String): List<Double>

	/** Float のリストを取得します。セクションがない場合は空のリストを返します */
	fun getFloatList(path: String): List<Float>

	/**
	 * Map のリストを取得します。
	 * YAML でいうシーケンス内にマッピングが並ぶ形式（オブジェクト配列）に対応します。
	 */
	fun getMapList(path: String): List<Map<*, *>>

	// =========================================================
	// セクション操作
	// =========================================================

	/**
	 * 指定パスに空のサブセクションを作成します。
	 * すでに値が存在する場合は上書きされます。
	 */
	fun createSection(path: String): LConfig

	/**
	 * 指定パスに Map の内容でサブセクションを作成します。
	 * すでに値が存在する場合は上書きされます。
	 */
	fun createSection(path: String, values: Map<*, *>): LConfig

	// =========================================================
	// BoostedYAML へのアクセス
	// =========================================================

	/**
	 * BoostedYAML の Section オブジェクトを返します。
	 * BoostedYAML 固有のメソッド（コメント操作、高度な値操作等）に
	 * 直接アクセスしたい場合に使用します。
	 */
	fun asSection(): Section

	/**
	 * BoostedYAML の YamlDocument オブジェクトを返します。
	 * ルートセクションの場合のみ非 null を返します。
	 * ドキュメントレベルの操作（reload, save, update 等）に使用します。
	 */
	fun asDocument(): YamlDocument?

	// =========================================================
	// 書き込み・保存
	// =========================================================

	/**
	 * 指定パスに値をセットします。
	 * **ファイルには即座に反映されません**。ファイルへの書き出しは [save] を呼ぶ必要があります。
	 * value に null を渡した場合はnullが挿入されます
	 */
	fun set(path: String, value: Any?)

	/**
	 * 指定のパスを削除します
	 * **ファイルには即座に反映されません**。ファイルへの書き出しは [save] を呼ぶ必要があります。
	 */
	fun remove(path: String)

	/**
	 * 現在の設定内容をファイルに保存します。
	 * サブセクションの場合はルート設定ファイルの保存に委譲します（[root] 経由）。
	 *
	 * @throws UnsupportedOperationException
	 *   ファイルに紐付いていない設定（[LConfig.empty()] 等）に対して呼んだ場合
	 */
	fun save()

	/**
	 * @see [LConfigPath.save]
	 */
	fun saveTo(path: LConfigPath)
}
