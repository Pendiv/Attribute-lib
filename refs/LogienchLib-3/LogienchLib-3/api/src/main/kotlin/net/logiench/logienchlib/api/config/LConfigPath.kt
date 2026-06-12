package net.logiench.logienchlib.api.config

import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings
import net.logiench.logienchlib.api.InstanceHolder
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.nio.file.Path

interface LConfigPathFactory {

	/**
	 * このファクトリのデータフォルダ
	 */
	val dataFolder: File

	/**
	 * プラグインのデータフォルダ直下にあるファイルまたはディレクトリのパスを指します。
	 * 例: factory.fromRelative("config.yml") / factory.fromRelative("locales")
	 */
	fun fromRelative(relativePath: String): LConfigPath

	/**
	 * Jar 内から指定されたリソースパスのファイルを直接読み込み、メモリ上に LConfig を生成します。
	 * 物理ファイルへの書き出し（保存）は行いません。
	 *
	 * @param resourcePath Jar 内のリソースパス（例: "config.yml"、"defaults/config.yml"）
	 * @return 読み込まれたデフォルトの LConfig（物理パスを持たないため、save() を呼ぶと例外が発生します）
	 * @throws IllegalStateException Jar 内にリソースが見つからない場合、または読み込みに失敗した場合
	 */
	fun loadResource(resourcePath: String): LConfig
}

interface LConfigPath {

	companion object {

		/**
		 * プラグイン固有のデータフォルダを起点とするファクトリを生成します。
		 * ### このメソッドはBukkit専用です
		 */
		@JvmStatic
		fun createFactory(plugin: JavaPlugin): LConfigPathFactory =
			InstanceHolder.configService.createFactory(plugin)

		/**
		 * プラグイン固有のデータフォルダを起点とするファクトリを生成します。
		 * ### このメソッドはVelocity専用です
		 * @param plugin Velocityの主インスタンスオブジェクト
		 * @throws IllegalArgumentException [plugin]が主インスタンスオブジェクトでなかった場合
		 */
		@JvmStatic
		fun createFactory(plugin: Any): LConfigPathFactory =
			InstanceHolder.configService.createFactory(plugin)

		/**
		 * 任意の Path オブジェクトから直接 LConfigPath を生成します。
		 */
		@JvmStatic
		fun of(path: Path): LConfigPath = of(path.toFile())

		/**
		 * 任意の File オブジェクトから直接 LConfigPath を生成します。
		 */
		@JvmStatic
		fun of(file: File): LConfigPath =
			InstanceHolder.configService.createPath(file)
	}

	/**
	 * プラットフォーム依存の物理的な File オブジェクトを取得します。
	 */
	fun toFile(): File

	/**
	 * Jar 内から、このパスが指すファイルと同名のリソースファイルを直接読み込み、メモリ上に LConfig を生成します。
	 * 物理ファイルへの書き出し（保存）は行いません。
	 *
	 * @return 読み込まれたデフォルトの LConfig（物理パスを持たないため、save() を呼ぶと例外が発生します）
	 * @throws IllegalStateException Jar 内に同名のリソースが見つからない場合、または読み込みに失敗した場合
	 */
	fun loadResource(): LConfig

	/**
	 * このパスが指すファイルまたはディレクトリが存在するかを返します。
	 */
	fun exists(): Boolean

	/**
	 * このパスがディレクトリを指しているかを返します。
	 */
	fun isDirectory(): Boolean

	// =========================================================
	// リソース保存
	// =========================================================

	/**
	 * Jar内から同名の未変形リソースファイルを物理パスへ書き出します（既に存在する場合は上書きしない）。
	 * Bukkitの [org.bukkit.plugin.Plugin.saveResource] の挙動を再現します。
	 * @return 正常に書き出された、または既に存在していた場合は true
	 */
	fun saveResource(): Boolean = saveResource(replace = false)

	/**
	 * Jar内から同名のリソースファイルを物理パスへ書き出します。
	 * @param replace true の場合、既に存在するファイルを上書きします
	 * @return 正常に書き出されたか、replace=false で既に存在した場合は true
	 */
	fun saveResource(replace: Boolean): Boolean

	/**
	 * Jar内の指定したリソースパスのファイルを、このパスへ書き出します。
	 * @param resourcePath Jar内のリソースパス（例: "defaults/config.yml"）
	 * @param replace true の場合、既に存在するファイルを上書きします
	 * @return 正常に書き出されたか、replace=false で既に存在した場合は true
	 */
	fun saveResource(resourcePath: String, replace: Boolean): Boolean

	// =========================================================
	// 読み込み
	// =========================================================

	/**
	 * このパスが指すファイルの内容を読み込み、LConfig オブジェクトを生成します。
	 * ファイルが存在しない場合は空のオブジェクトが生成されます。
	 * @throws IllegalStateException ファイルがディレクトリだった場合、または読み込みに失敗した場合
	 */
	fun load(): LConfig

	/**
	 * ファイルを読み込み、Jar 内の同名リソースをデフォルトとして自動マージした LConfig を返します。
	 * BoostedYAML の Updater を内部で使用し、コメントを保持したままキーの補完を行います。
	 */
	fun loadWithUpdate(): LConfig

	/**
	 * BoostedYAML の UpdaterSettings を指定してロードします。
	 * バージョン管理（BasicVersioning）やカスタムマイグレーションが必要な場合に使用します。
	 */
	fun loadWithUpdate(updaterSettings: UpdaterSettings): LConfig

	/**
	 * 拡張子が ["yml", "yaml"] のファイルだけを対象に、ディレクトリ内の一括読み込みを行います。
	 *
	 * @return ファイル名（拡張子なし）と LConfig のマップ
	 */
	fun loadAllInDirectory(): Map<String, LConfig> = loadAllInDirectory(listOf("yml", "yaml"))

	/**
	 * 指定した拡張子のファイルだけを対象に、ディレクトリ内の一括読み込みを行います。
	 *
	 * @param extensions 対象とする拡張子のコレクション（ドットなし）。
	 * @return ファイル名（拡張子なし）と LConfig のマップ
	 */
	fun loadAllInDirectory(extensions: Collection<String>): Map<String, LConfig>

	// =========================================================
	// 書き込み
	// =========================================================

	/**
	 * メモリ上の [config] の内容をこのパスが指す物理ファイルへ書き込みます。
	 */
	fun save(config: LConfig)
}