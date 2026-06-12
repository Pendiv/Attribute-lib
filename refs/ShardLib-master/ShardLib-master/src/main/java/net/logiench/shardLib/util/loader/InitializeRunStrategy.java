package net.logiench.shardLib.util.loader;

import net.logiench.shardLib.ShardLib;
import net.logiench.shardLib.api.event.ShardLibReadyEvent;
import net.logiench.shardLib.util.ConfigLoader;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class InitializeRunStrategy implements LoadStrategy {
	@Override
	public boolean execute(@NotNull ConfigLoader context) {
		try {
			new RegistrySetBuilder().setRegistry(context.registrySet()).build();

			context.databaseManager().initialize();

			Bukkit.getPluginManager().callEvent(new ShardLibReadyEvent());
			return true;

		} catch (Exception e) {
			// サーバー起動時の(YMLの解析)エラーは致命的
			Logger logger = ShardLib.getInstance().getLogger();
			logger.severe("");
			logger.severe("========================================");
			logger.severe("!!! FATAL ERROR DURING INITIALIZATION !!!");
			logger.severe("Failed to load definitions");
			logger.severe("Error: " + e.getMessage());
			logger.severe("The server will now shut down to prevent data corruption.");
			logger.severe("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
			logger.severe(e.getMessage());
			for (StackTraceElement element : e.getStackTrace()) {
				logger.severe("\tat " + element.toString());
			}
			logger.severe("========================================");
			logger.severe("");
			Bukkit.getServer().shutdown();
		}
		return false;
	}
}