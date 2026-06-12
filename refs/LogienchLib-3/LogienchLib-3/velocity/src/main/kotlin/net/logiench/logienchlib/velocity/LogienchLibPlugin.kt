package net.logiench.logienchlib.velocity

import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.proxy.ProxyServer
import io.avaje.inject.BeanScope
import net.logiench.logienchlib.api.InstanceHolder
import net.logiench.logienchlib.api.LlApiModule
import net.logiench.logienchlib.api.config.LConfigPath
import net.logiench.logienchlib.core.LlCoreModule
import net.logiench.logienchlib.velocity.timer.PlayerLTaskPoolImpl
import org.slf4j.Logger
import java.nio.file.Path

class LogienchLibPlugin(
	private val plugin: LogienchLibBootstrap,
	private val server: ProxyServer,
	private val logger: Logger,
	private val dataDirectory: Path,
	private val container: PluginContainer
) {

	private val beanScope: BeanScope = BeanScope.builder()
//		.classLoader(this.javaClass.classLoader)
		.bean(ProxyServer::class.java, server)
		.bean(LogienchLibBootstrap::class.java, plugin)
		.modules(LlVelocityModule(), LlCoreModule(), LlApiModule())
		.build()

	internal fun onProxyInitialize(ev: ProxyInitializeEvent) {
		setState(InstanceHolder.State.BEFORE_LOADED)

		@Suppress("DEPRECATION_ERROR")
		InstanceHolder.setLoad(beanScope.get(InstanceHolder.LoadContext::class.java))


		setState(InstanceHolder.State.BEFORE_ENABLED)

		@Suppress("DEPRECATION_ERROR")
		InstanceHolder.setEnabled(beanScope.get(InstanceHolder.EnableContext::class.java))

		server.eventManager.register(plugin, beanScope.get(PlayerLTaskPoolImpl::class.java))


		//TEST START
		val factory = LConfigPath.createFactory(plugin)
		val path = factory.fromRelative("config.yml")
		path.saveResource()
		val config = path.loadWithUpdate()
		for (key in config.getKeys(true)) {
			println("$key: ${config.get(key)}")
		}
		config.set("key.value", "WORLD!")
		config.save()
		// TEST END
	}

	internal fun onProxyShutdown(ev: ProxyShutdownEvent) {

	}

	private fun setState(state: InstanceHolder.State) {
		InstanceHolder.javaClass.getDeclaredField("state").apply {
			isAccessible = true
			set(InstanceHolder, state)
		}
	}
}