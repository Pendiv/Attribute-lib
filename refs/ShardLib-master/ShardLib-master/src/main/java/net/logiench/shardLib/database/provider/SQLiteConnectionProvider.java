package net.logiench.shardLib.database.provider;

import com.zaxxer.hikari.HikariConfig;
import net.logiench.logienchlibv2.api.database.mysql.HikariConnection;
import net.logiench.shardLib.ShardLib;
import net.logiench.shardLib.database.config.SQLiteConfig;

import java.io.File;
import java.util.function.Supplier;

public class SQLiteConnectionProvider extends ConnectionProvider {

	public SQLiteConnectionProvider(SQLiteConfig config) {
		super(((Supplier<HikariConnection>) () -> {
			HikariConfig hikariConfig = new HikariConfig();
			File file = new File(ShardLib.getInstance().getDataFolder(), config.filename());
			hikariConfig.setJdbcUrl("jdbc:sqlite:" + file.getPath() + (config.modeWal() ? "?journal_mode=WAL" : ""));
			hikariConfig.setDriverClassName("org.sqlite.JDBC");

			return new HikariConnection(hikariConfig);
		}).get());
	}
}
