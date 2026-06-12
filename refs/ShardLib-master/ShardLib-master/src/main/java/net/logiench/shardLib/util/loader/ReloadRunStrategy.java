package net.logiench.shardLib.util.loader;

import net.logiench.shardLib.ShardLib;
import net.logiench.shardLib.api.event.ShardLibReloadFailedEvent;
import net.logiench.shardLib.api.event.ShardLibReloadStartEvent;
import net.logiench.shardLib.api.event.ShardLibReloadSuccessEvent;
import net.logiench.shardLib.database.DatabaseManager;
import net.logiench.shardLib.util.ConfigLoadException;
import net.logiench.shardLib.util.ConfigLoader;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class ReloadRunStrategy extends AbstractLoadStrategy {
	@Override
	public boolean execute(@NotNull ConfigLoader context) {
		Logger logger = ShardLib.getInstance().getLogger();
		RegistrySetBuilder registryBuilder = new RegistrySetBuilder();
		registryBuilder.setRegistry(context.registrySet());
		try {
			// 2. もしドライランが失敗したら、何もせず処理を中断
			logger.info("============= Reload check =============");
			try {
				registryBuilder.dryRun();

				logger.info("Registry: Success");
			} catch (ConfigLoadException e) {
				logger.warning("Registry: Failed");
				throw e;
			}

			DatabaseManager.ConnectionTestResult result = context.databaseManager().testConnection();
			if (result.isSuccess()) {
				logger.info("Database: Success");
			} else {
				logger.warning("Database: Failed");
				throw new ConfigLoadException("Database " + result.message());
			}
		} catch (ConfigLoadException e) {
			setFailure(e.getMessage());

			logger.severe("========================================");
			logger.severe("--- Reload Validation Failed ---");
			logger.severe("Error: " + e.getMessage());
			logger.severe("Please check your config files and API registrations for inconsistencies");
			logger.severe("========================================");

			Bukkit.getPluginManager().callEvent(new ShardLibReloadFailedEvent());
			return false;
		}

		Bukkit.getPluginManager().callEvent(new ShardLibReloadStartEvent());

		// 3. ドライランが成功した場合のみ、本番のRegistryを更新
		try {
			registryBuilder.build();
			context.mobCharacterManager().reloadProfiles();
			context.databaseManager().reload();

			setSuccess("Config successfully reloaded");

			logger.info(" -> Reload: Successful");
			logger.info("============= Reload check =============");

			Bukkit.getPluginManager().callEvent(new ShardLibReloadSuccessEvent());
			return true;
		} catch (ConfigLoadException e) {
			// ドライランは成功したのに本番で失敗した場合 (理論上ほぼ起こり得ないが念のため)
			logger.severe("========================================");
			logger.severe("--- Reload Validation Failed ---");
			logger.severe("Error: " + e.getMessage());
			logger.severe("========================================");

			Bukkit.getPluginManager().callEvent(new ShardLibReloadFailedEvent());
		}
		return false;
	}
}