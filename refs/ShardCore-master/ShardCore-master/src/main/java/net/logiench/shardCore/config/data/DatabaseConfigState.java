package net.logiench.shardCore.config.data;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import net.logiench.shardCore.config.system.ConfigKey;
import net.logiench.shardCore.config.system.ConfigManager;
import net.logiench.shardCore.config.system.ConfigSection;
import net.logiench.shardCore.config.system.DefaultConfigKey;
import net.logiench.shardCore.db.DatabaseType;
import net.logiench.shardCore.db.TargetDatabase;

import java.util.EnumMap;
import java.util.Map;

/**
 * この内容はreloadに非対応。
 * データベースへ接続するための情報を取得、管理する
 */
@Getter
@Singleton
public class DatabaseConfigState {

	private static final String CONFIG_PATH = "config.yml|database";

	private static final DefaultConfigKey<Integer> AUTO_SAVE_INTERVAL = ConfigKey.of("auto_save_interval", Integer.class, 300);

	private static final DefaultConfigKey<String> DATABASE_TYPE = ConfigKey.of("type", String.class, DatabaseType.SQLITE.getNameFirst());
	private static final DefaultConfigKey<String> TABLE_PREFIX = ConfigKey.of("table_prefix", String.class, "");

	private static final DefaultConfigKey<String> URL = ConfigKey.of("url", String.class, "");
	private static final DefaultConfigKey<String> USERNAME = ConfigKey.of("username", String.class, "root");
	private static final DefaultConfigKey<String> PASSWORD = ConfigKey.of("password", String.class, "");

	@Getter
	public static class State {
		private final DatabaseType databaseType;
		private final String tablePrefix;
		private final String url;
		private final String username;
		private final String password;

		public State(ConfigSection section) {
			String typeName = section.get(DATABASE_TYPE).toLowerCase();
			this.databaseType = DatabaseType.fromName(typeName, DatabaseType.SQLITE);
			this.tablePrefix = section.get(TABLE_PREFIX);
			this.url = section.get(URL);
			this.username = section.get(USERNAME);
			this.password = section.get(PASSWORD);
		}
	}

	private final Map<TargetDatabase, State> states = new EnumMap<>(TargetDatabase.class);
	private final int autoSaveIntervalTick;

	@Inject
	private DatabaseConfigState(ConfigManager configManager) {
		ConfigSection rootConfig = configManager.getConfig(CONFIG_PATH);
		ConfigSection globalConfig = configManager.getConfig("config.yml");

		int autoSaveInterval = globalConfig.get(AUTO_SAVE_INTERVAL);
		this.autoSaveIntervalTick = autoSaveInterval * 20;

		for (TargetDatabase target : TargetDatabase.values()) {
			ConfigSection targetSection = rootConfig.getSection(target.getKey());
			if (targetSection == null) {
				// ここでエラー吐かないと危ない

				continue;
			}
			states.put(target, new State(targetSection));
		}
	}

	public State getState(TargetDatabase target) {
		return states.get(target);
	}
}
