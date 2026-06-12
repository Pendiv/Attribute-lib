package net.logiench.logienchlib.api.internal

import io.avaje.inject.Secondary
import jakarta.inject.Singleton
import net.logiench.logienchlib.api.config.LConfigPath
import net.logiench.logienchlib.api.config.LConfigPathFactory
import org.jetbrains.annotations.ApiStatus
import java.io.File
import java.io.InputStream

@ApiStatus.Internal
interface ConfigService {

	fun createPath(file: File): LConfigPath =
		throw UnsupportedService("ConfigService")

	fun createFactory(plugin: Any): LConfigPathFactory =
		throw UnsupportedService("ConfigService")
}

@Secondary
@Singleton
internal class UnsupportedConfigService : ConfigService

/**
 * プラットフォーム固有のコンフィグディレクトリや、Jar内のリソースストリームを取得するためのインターフェースです。
 */
@ApiStatus.Internal
interface ConfigPathService {
	/**
	 * 指定されたプラグインのデータフォルダ（コンフィグディレクトリ）を取得します。
	 *
	 * @param plugin プラグインのインスタンスオブジェクト
	 * @return データフォルダの物理パス
	 */
	fun getDataFolder(plugin: Any): File

	/**
	 * 指定されたプラグインのJar内からリソースファイルを読み込み、InputStream を取得します。
	 *
	 * @param plugin プラグインのインスタンスオブジェクト
	 * @param resourcePath Jar内のリソースパス（例: "config.yml"）
	 * @return リソースの InputStream。見つからない場合は null
	 */
	fun getResource(plugin: Any, resourcePath: String): InputStream?
}
