package net.logiench.logienchlibv2.api.database.mysql;

import net.logiench.logienchlibv2.api.database.I_Connection;

import java.sql.Connection;

@SuppressWarnings("unused")
public interface I_DBConnection extends I_Connection {
	Connection getConnection();
}
