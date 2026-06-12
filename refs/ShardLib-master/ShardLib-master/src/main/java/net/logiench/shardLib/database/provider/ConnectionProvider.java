package net.logiench.shardLib.database.provider;

import net.logiench.logienchlibv2.api.database.mysql.I_DBConnection;

import java.io.Closeable;
import java.sql.Connection;

public abstract class ConnectionProvider implements Closeable {
	protected final I_DBConnection connection;

	protected ConnectionProvider(I_DBConnection connection) {
		this.connection = connection;
	}

	public Connection getConnection() {
		return connection.getConnection();
	}

	public boolean isClosed() {
		return connection.isClosed();
	}

	@Override
	public void close() {
		connection.close();
	}
}
