package net.logiench.logienchlib.velocity

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import org.slf4j.Logger
import java.nio.file.Path

/*
versionを自動で置き換えるためにtemplatesに配置してある
その影響で補完が効かないので、全ての情報をvelocityのLogienchLibPluginに渡す
このクラスは間違っていてもコンパイルでしかエラーが出ないので**絶対に触らない**
*/

@Plugin(
	id = "logienchlib",
	name = "LogienchLib",
	version = "${version}",
	description = "Project-Logiench の便利機能ライブラリです",
	authors = ["masa355"],
	url = "https://github.com/Logiench/LogienchLib",
)
class LogienchLibBootstrap @Inject constructor(
	private val server: ProxyServer,
	private val logger: Logger,
	@DataDirectory private val dataDirectory: Path,
	private val container: PluginContainer
) {

	private val plugin: LogienchLibPlugin

	init {
		plugin = LogienchLibPlugin(this, server, logger, dataDirectory, container)
	}

	@Subscribe
	fun onProxyInitialization(event: ProxyInitializeEvent) {
		plugin.onProxyInitialize(event)
	}

	@Subscribe
	fun onProxyShutdown(event: ProxyShutdownEvent) {
		plugin.onProxyShutdown(event)
	}
}