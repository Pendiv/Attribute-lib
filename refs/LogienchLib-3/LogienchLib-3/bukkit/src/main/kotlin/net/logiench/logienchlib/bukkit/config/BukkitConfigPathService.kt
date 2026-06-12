package net.logiench.logienchlib.bukkit.config

import jakarta.inject.Singleton
import net.logiench.logienchlib.api.internal.ConfigPathService
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.InputStream

/**
 * Bukkit プラットフォームにおける [ConfigPathService] の実装クラスです。
 */
@Singleton
class BukkitConfigPathService : ConfigPathService {

	override fun getDataFolder(plugin: Any): File {
		val javaPlugin = plugin as? JavaPlugin
			?: throw IllegalArgumentException("プラグインはJavaPluginである必要があります: ${plugin::class.java.name}")
		return javaPlugin.dataFolder
	}

	override fun getResource(plugin: Any, resourcePath: String): InputStream? {
		val javaPlugin = plugin as? JavaPlugin
			?: throw IllegalArgumentException("プラグインはJavaPluginである必要があります: ${plugin::class.java.name}")
		return javaPlugin.getResource(resourcePath)
	}
}
