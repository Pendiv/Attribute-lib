package net.logiench.shardLib.database.config;

public record MySqlConfig(
	String host,
	int port,
	String database,
	String username,
	String password,
	String tablePrefix
) implements DatabaseConfig {
}
