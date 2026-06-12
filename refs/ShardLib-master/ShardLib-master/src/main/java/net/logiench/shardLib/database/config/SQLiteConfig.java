package net.logiench.shardLib.database.config;

public record SQLiteConfig(
	String filename,
	boolean modeWal,
	String tablePrefix
) implements DatabaseConfig {
}
