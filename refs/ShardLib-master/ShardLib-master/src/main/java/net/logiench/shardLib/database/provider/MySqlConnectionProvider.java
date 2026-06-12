package net.logiench.shardLib.database.provider;

import net.logiench.logienchlibv2.api.database.mysql.HikariConnection;
import net.logiench.shardLib.database.config.MySqlConfig;

public class MySqlConnectionProvider extends ConnectionProvider {

	public MySqlConnectionProvider(MySqlConfig config) {
		super(new HikariConnection(config.host(), config.port(), config.database(), config.username(), config.password(), false));
	}
}
