package net.logiench.shardLib.database.dialect;

import net.logiench.shardLib.database.config.DatabaseConfig;

public class MySqlDialect extends SQLDialect {
	public MySqlDialect(DatabaseConfig config) {
		super(config);
	}

	// --- Player Stats ---

	@Override
	public String createTablePlayerStats() {
		return "CREATE TABLE IF NOT EXISTS " + config.tablePrefix() + "player_stats (" +
			"owner_uuid CHAR(36) PRIMARY KEY, " +
			"stats JSON NOT NULL" +
			");";
	}

	@Override
	public String savePlayerStats() {
		// owner_uuidが重複していた場合、statsカラムを新しい値で更新する
		return "INSERT INTO " + config.tablePrefix() + "player_stats " +
			"(owner_uuid, stats) VALUES (?, ?) " +
			"ON DUPLICATE KEY UPDATE stats = VALUES(stats);";
	}

	@Override
	public String loadPlayerStats() {
		return "SELECT stats FROM " + config.tablePrefix() + "player_stats " +
			"WHERE owner_uuid = ?;";
	}

	// --- Player Modifier ---

	@Override
	public String createTablePlayerModifier() {
		return "CREATE TABLE IF NOT EXISTS " + config.tablePrefix() + "player_modifier (" +
			"instance_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
			"owner_uuid CHAR(36) NOT NULL, " +
			"source_id VARCHAR(127) NOT NULL, " +
			"target_id VARCHAR(127) NOT NULL, " +
			"operation VARCHAR(32) NOT NULL, " +
			"stacking_rule VARCHAR(32) NOT NULL, " +
			"value DOUBLE NOT NULL, " +
			"remaining_ticks BIGINT NOT NULL, " +
			"INDEX idx_owner_uuid (owner_uuid)" +
			");";
	}

	@Override
	public String insertPlayerModifier() {
		// instance_idが重複していた場合、他の全てのカラムを新しい値で更新する
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
		//		return "DELETE FROM " + config.tablePrefix() + "player_modifier WHERE instance_id IN(" + instanceIds.stream().map(Object::toString).collect(Collectors.joining("', '", "'", "'")) + ");";
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
			"instance_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
			"owner_uuid CHAR(36) NOT NULL, " +
			"source_id VARCHAR(127) NOT NULL, " +
			"target_id VARCHAR(127) NOT NULL, " +
			"operation VARCHAR(32) NOT NULL, " +
			"provider_key VARCHAR(127) NOT NULL, " +
			"remaining_ticks BIGINT NOT NULL, " +
			"INDEX idx_owner_uuid (owner_uuid)" +
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
		//		return "DELETE FROM " + config.tablePrefix() + "player_provider WHERE instance_id IN(" + instanceIds.stream().map(Object::toString).collect(Collectors.joining("', '", "'", "'")) + ");";
		return "DELETE FROM " + config.tablePrefix() + "player_provider WHERE instance_id = ?;";
	}

	@Override
	public String loadPlayerProvider() {
		return "SELECT instance_id, source_id, target_id, operation, provider_key, remaining_ticks " +
			"FROM " + config.tablePrefix() + "player_provider WHERE owner_uuid = ?;";
	}
}