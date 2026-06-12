package net.logiench.shardLib.database.config;

import net.logiench.shardLib.util.ConfigLoadException;
import org.bukkit.configuration.ConfigurationSection;

public class ConfigReader {

	private final ConfigurationSection database;
	private final String type;
	private final String prefix;

	public ConfigReader(ConfigurationSection database) {
		this.database = database;
		this.type = database.getString("type", "sqlite").toLowerCase();
		this.prefix = database.getString("table-prefix", "");
	}

	public DatabaseConfig load() {
		if (type.equals("sqlite")) {
			return loadSQLite(database.getConfigurationSection("sqlite"));
		} else {
			return loadRemote(database.getConfigurationSection("remote"));
		}
	}

	private SQLiteConfig loadSQLite(ConfigurationSection config) {
		if (config == null) {
			throw new ConfigLoadException("'config.yml' database.sqlite section not found!");
		}
		String fileName = config.getString("sqlite.file", "data.db");
		boolean wal = config.getBoolean("sqlite.mode-wal", false);
		return new SQLiteConfig(fileName, wal, prefix);
	}

	private DatabaseConfig loadRemote(ConfigurationSection config) {
		if (config == null) {
			throw new ConfigLoadException("'config.yml' database.remote section not found!");
		}
		String host = config.getString("host", "localhost");
		int port = config.getInt("port", 3306);
		String user = config.getString("user", "root");
		String password = config.getString("password", "");
		String databaseName = config.getString("database");
		if (databaseName == null) {
			throw new ConfigLoadException("'config.yml' database.remote.database is null!");
		}
		return switch (type) {
			case "mysql", "mariadb" -> new MySqlConfig(host, port, databaseName, user, password, prefix);
			default -> throw new ConfigLoadException("'config.yml' database.type is not found type!");
		};
	}
}
