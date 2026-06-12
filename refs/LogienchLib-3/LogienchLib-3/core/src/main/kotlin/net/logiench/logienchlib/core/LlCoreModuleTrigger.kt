package net.logiench.logienchlib.core

import io.avaje.inject.InjectModule
import net.logiench.logienchlib.api.internal.ConfigPathService

@InjectModule(
	name = "ll-core",
	requires = [ConfigPathService::class]
)
class LlCoreModuleTrigger