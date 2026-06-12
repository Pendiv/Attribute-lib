package net.logiench.logienchlib.core.config

import dev.dejvokep.boostedyaml.YamlDocument
import net.logiench.logienchlib.api.config.LConfig
import net.logiench.logienchlib.api.config.LConfigPath
import net.logiench.logienchlib.api.config.LConfigPathFactory
import net.logiench.logienchlib.api.internal.ConfigPathService
import java.io.File

/**
 * [LConfigPathFactory] の共通実装です。
 */
class ConfigPathFactoryImpl(
	private val plugin: Any,
	private val pathService: ConfigPathService,
) : LConfigPathFactory {

	override val dataFolder: File
		get() = pathService.getDataFolder(plugin)

	override fun fromRelative(relativePath: String): LConfigPath {
		val file = File(dataFolder, relativePath)
		return LConfigPathImpl(file, pathService, plugin)
	}

	override fun loadResource(resourcePath: String): LConfig {
		val stream = pathService.getResource(plugin, resourcePath)
			?: throw IllegalStateException("Jar 内にリソース '$resourcePath' が見つかりませんでした。")
		return stream.use { input ->
			val doc = YamlDocument.create(input)
			LConfigImpl(doc)
		}
	}
}
