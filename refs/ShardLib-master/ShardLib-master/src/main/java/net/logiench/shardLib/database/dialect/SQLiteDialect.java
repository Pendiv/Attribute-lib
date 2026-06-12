package net.logiench.shardLib.database.dialect;

import net.logiench.shardLib.database.config.DatabaseConfig;

public class SQLiteDialect extends SQLDialect {
	public SQLiteDialect(DatabaseConfig config) {
		super(config);
	} //todo AIに書かせただけだから動作チェックまだ
	/*
	注意
	SQLiteは型システムがMySQLとは全く違うので、修正は調べてからやること
	ex:
	MySQL   ->    SQLite
	---------------------
	VARCHAR, CHAR... -> TEXT
	INT, BIGINT... -> INTEGER
	FLOAT, DOUBLE... -> REAL
	---------------------
	 */

	// --- Player Stats ---

	@Override
	public String createTablePlayerStats() {
		return "CREATE TABLE IF NOT EXISTS " + config.tablePrefix() + "player_stats (" +
			"owner_uuid TEXT PRIMARY KEY, " +
			"stats JSON NOT NULL" +
			");";
	}

	@Override
	public String savePlayerStats() {
		// owner_uuidが重複した場合、statsカラムを新しい値で更新する
		return "INSERT INTO " + config.tablePrefix() + "player_stats " +
			"(owner_uuid, stats) VALUES (?, ?) " +
			"ON CONFLICT(owner_uuid) DO UPDATE SET stats = excluded.stats;";
	}

	@Override
	public String loadPlayerStats() {
		return "SELECT stats FROM " + config.tablePrefix() + "player_stats " +
			"WHERE owner_uuid = ?;";
	}

	// --- Player Modifier ---

	@Override
	public String createTablePlayerModifier() {
		// SQLiteの型とAUTOINCREMENT構文に修正
		return "CREATE TABLE IF NOT EXISTS " + config.tablePrefix() + "player_modifier (" +
			"instance_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
			"owner_uuid TEXT NOT NULL, " +
			"source_id TEXT NOT NULL, " +
			"target_id TEXT NOT NULL, " +
			"operation TEXT NOT NULL, " +
			"stacking_rule TEXT NOT NULL, " +
			"value REAL NOT NULL, " +
			"remaining_ticks INTEGER NOT NULL" +
			");";
		// INDEXは別途CREATE INDEX文で作成するのがSQLiteの作法
	}

	@Override
	public String insertPlayerModifier() {
		return "INSERT INTO " + config.tablePrefix() + "player_modifier " +
			"(owner_uuid, source_id, target_id, operation, stacking_rule, value, remaining_ticks) " +
			"VALUES (?, ?, ?, ?, ?, ?, ?);";
	}

	@Override
	public String updatePlayerModifier() {
		return "UPDATE " + config.tablePrefix() + "player_modifier " +
			"SET " +
			"owner_uuid = ?, " +
			"source_id = ?, " +
			"target_id = ?, " +
			"operation = ?, " +
			"stacking_rule = ?, " +
			"value = ?, " +
			"remaining_ticks = ? " +
			"WHERE instance_id = ?;";
	}

	@Override
	public String removePlayerModifiers() {
		return "DELETE FROM " + config.tablePrefix() + "player_modifier WHERE instance_id = ?;";
	}

	@Override
	public String loadPlayerModifier() {
		return "SELECT instance_id, source_id, target_id, operation, stacking_rule, value, remaining_ticks " +
			"FROM " + config.tablePrefix() + "player_modifier WHERE owner_uuid = ?;";
	}

	// --- Player Provider ---

	@Override
	public String createTablePlayerProvider() {
		return "CREATE TABLE IF NOT EXISTS " + config.tablePrefix() + "player_provider (" +
			"instance_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
			"owner_uuid TEXT NOT NULL, " +
			"source_id TEXT NOT NULL, " +
			"target_id TEXT NOT NULL, " +
			"operation TEXT NOT NULL, " +
			"provider_key TEXT NOT NULL, " +
			"remaining_ticks INTEGER NOT NULL" +
			");";
	}

	@Override
	public String insertPlayerProvider() {
		return "INSERT INTO " + config.tablePrefix() + "player_provider " +
			"(owner_uuid, source_id, target_id, operation, provider_key, remaining_ticks) " +
			"VALUES (?, ?, ?, ?, ?, ?)";
	}

	@Override
	public String updatePlayerProvider() {
		return "UPDATE " + config.tablePrefix() + "player_provider " +
			"SET " +
			"owner_uuid = ?, " +
			"source_id = ?, " +
			"target_id = ?, " +
			"operation = ?, " +
			"provider_key = ?, " +
			"remaining_ticks = ? " +
			"WHERE instance_id = ?;";
	}

	@Override
	public String removePlayerProviders() {
		return "DELETE FROM " + config.tablePrefix() + "player_provider WHERE instance_id = ?;";
	}

	@Override
	public String loadPlayerProvider() {
		return "SELECT instance_id, source_id, target_id, operation, provider_key, remaining_ticks " +
			"FROM " + config.tablePrefix() + "player_provider WHERE owner_uuid = ?;";
	}
}