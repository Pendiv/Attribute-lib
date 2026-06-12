package net.logiench.shardLib.database;

import com.google.inject.Singleton;
import net.logiench.logienchlibv2.api.config.ConfigUtil;
import net.logiench.shardLib.ShardLib;
import net.logiench.shardLib.database.autoIncrement.AutoIncrementBatchStrategy;
import net.logiench.shardLib.database.autoIncrement.MySqlBatchStrategy;
import net.logiench.shardLib.database.autoIncrement.SQLiteBatchStrategy;
import net.logiench.shardLib.database.config.ConfigReader;
import net.logiench.shardLib.database.config.DatabaseConfig;
import net.logiench.shardLib.database.config.MySqlConfig;
import net.logiench.shardLib.database.config.SQLiteConfig;
import net.logiench.shardLib.database.dialect.MySqlDialect;
import net.logiench.shardLib.database.dialect.SQLDialect;
import net.logiench.shardLib.database.dialect.SQLiteDialect;
import net.logiench.shardLib.database.provider.ConnectionProvider;
import net.logiench.shardLib.database.provider.MySqlConnectionProvider;
import net.logiench.shardLib.database.provider.SQLiteConnectionProvider;
import net.logiench.shardLib.util.ConfigLoadException;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

@Singleton
public class DatabaseManager {
	private ConnectionProvider provider;
	private AutoIncrementBatchStrategy batchStrategy = null;
	private DatabaseConfig config = null;
	private SQLDialect sqlDialect = null;

	/**
	 * プラグイン起動時に呼び出され、最初のDB接続を確立する
	 */
	public void initialize() {
		// config.ymlから設定を読み込む
		this.config = loadConfig();

		this.provider = getProvider(config);
		setSQLSettings(config);

		// ここでテーブル作成/更新処理 (ALTER TABLEなど) を呼び出す
		try (Connection connection = getConnection();
			 Statement statement = connection.createStatement()
		) {
			statement.addBatch(sqlDialect.createTablePlayerStats());
			statement.addBatch(sqlDialect.createTablePlayerModifier());
			statement.addBatch(sqlDialect.createTablePlayerProvider());
			statement.executeBatch();
		} catch (SQLException e) {
			ShardLib.getInstance().getLogger().log(Level.SEVERE, "Failed to create or connect table.", e);
		}
	}

	private ConnectionProvider getProvider(DatabaseConfig config) {
		return switch (config) {
			case SQLiteConfig c -> new SQLiteConnectionProvider(c);
			case MySqlConfig c -> new MySqlConnectionProvider(c);

			default -> throw new IllegalStateException("Unexpected value: " + config);
		};
	}

	private void setSQLSettings(DatabaseConfig config) {
		switch (config) {
			case SQLiteConfig c -> {
				this.sqlDialect = new SQLiteDialect(c);
				this.batchStrategy = new SQLiteBatchStrategy();
			}
			case MySqlConfig c -> {
				this.sqlDialect = new MySqlDialect(c);
				this.batchStrategy = new MySqlBatchStrategy();
			}

			default -> throw new IllegalStateException("Unexpected value: " + config);
		}
	}

	/**
	 * 設定リロード時に呼び出される
	 */
	public void reload() {
		closeConnection();
		initialize();
	}

	public ConnectionTestResult testConnection() {
		try (ConnectionProvider connectionProvider = getProvider(loadConfig())) {
			try (Connection connection = connectionProvider.getConnection()) {
				if (connection.isValid(2)) {
					return new ConnectionTestResult(true, "Connection successful.");
				} else {
					return new ConnectionTestResult(false, "Connection was established but is not valid.");
				}
			}
		} catch (Exception e) {
			return new ConnectionTestResult(false, e.getMessage());
		}
	}

	public Connection getConnection() {
		return provider.getConnection();
	}

	public void closeConnection() {
		if (provider != null && !provider.isClosed()) {
			provider.close();
		}
	}

	public DatabaseConfig getConfig() {
		return config;
	}

	public SQLDialect getSQLDialect() {
		return sqlDialect;
	}

	public AutoIncrementBatchStrategy getBatchStrategy() {
		return batchStrategy;
	}

	private DatabaseConfig loadConfig() {
		ConfigUtil configUtil = new ConfigUtil(ShardLib.getInstance());
		configUtil.saveResource(Path.of("example", "config.yml"), false);

		YamlConfiguration config = configUtil.getYmlConfig("config.yml");
		if (config == null) {
			throw new ConfigLoadException("'config.yml' not found!");
		}
		ConfigurationSection database = config.getConfigurationSection("database");
		if (database == null) {
			throw new ConfigLoadException("'config.yml' database section not found!");
		}

		return new ConfigReader(database).load();
	}

	public record ConnectionTestResult(boolean isSuccess, String message) {}
}
