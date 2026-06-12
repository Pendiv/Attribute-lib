package net.logiench.logienchlib.velocity.config

import com.velocitypowered.api.proxy.ProxyServer
import jakarta.inject.Singleton
import net.logiench.logienchlib.api.internal.ConfigPathService
import java.io.File
import java.io.InputStream

/**
 * Velocity プラットフォームにおける [ConfigPathService] の実装クラスです。
 */
@Singleton
class VelocityConfigPathService(
	private val server: ProxyServer,
) : ConfigPathService {

	override fun getDataFolder(plugin: Any): File {
		val container = server.pluginManager.fromInstance(plugin).orElse(null)
			?: throw IllegalArgumentException(
				"指定されたオブジェクトは登録済みの Velocity プラグインインスタンスではありません: ${plugin::class.java.name}"
			)
		val pluginId = container.description.id
			?: throw IllegalArgumentException("プラグインコンテナの ID が解決できませんでした。")
		return File("plugins", pluginId)
	}

	override fun getResource(plugin: Any, resourcePath: String): InputStream? {
		return plugin.javaClass.classLoader.getResourceAsStream(resourcePath)
	}
}
