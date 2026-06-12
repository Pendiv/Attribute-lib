package net.logiench.logienchlib.bukkit

import io.avaje.inject.InjectModule
import org.bukkit.plugin.java.JavaPlugin

@InjectModule(
	name = "ll-bukkit", requires = [
		JavaPlugin::class
	]
)
class LlBukkitModuleTrigger
