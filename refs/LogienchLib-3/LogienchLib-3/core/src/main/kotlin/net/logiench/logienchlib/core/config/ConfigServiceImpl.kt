package net.logiench.logienchlib.core.config

import jakarta.inject.Singleton
import net.logiench.logienchlib.api.config.LConfigPath
import net.logiench.logienchlib.api.config.LConfigPathFactory
import net.logiench.logienchlib.api.internal.ConfigPathService
import net.logiench.logienchlib.api.internal.ConfigService
import java.io.File

/**
 * 共通の [ConfigService] 実装です。
 * プラットフォーム固有の処理は [ConfigPathService] に委譲します。
 */
@Singleton
class ConfigServiceImpl(
	private val pathService: ConfigPathService,
) : ConfigService {

	override fun createPath(file: File): LConfigPath {
		return LConfigPathImpl(file = file, pathService = pathService, plugin = null)
	}

	override fun createFactory(plugin: Any): LConfigPathFactory {
		return ConfigPathFactoryImpl(plugin = plugin, pathService = pathService)
	}
}
