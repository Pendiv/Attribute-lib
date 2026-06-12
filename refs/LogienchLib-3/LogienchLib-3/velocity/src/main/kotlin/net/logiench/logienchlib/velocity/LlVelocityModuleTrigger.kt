package net.logiench.logienchlib.velocity

import com.velocitypowered.api.proxy.ProxyServer
import io.avaje.inject.InjectModule

@InjectModule(
	name = "ll-velocity", requires = [
		ProxyServer::class,
		LogienchLibBootstrap::class,
	]
)
class LlVelocityModuleTrigger
