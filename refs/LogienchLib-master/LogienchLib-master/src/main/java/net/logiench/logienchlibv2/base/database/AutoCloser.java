package net.logiench.logienchlibv2.base.database;

import net.logiench.logienchlibv2.api.database.I_Connection;

import java.util.ArrayList;
import java.util.List;

public class AutoCloser {
	private static final List<I_Connection> connections = new ArrayList<>();

	public static void add(I_Connection c) {
		connections.add(c);
	}

	public static void remove(I_Connection c) {
		connections.remove(c);
	}

	public static void closeAll() {
		for (I_Connection conn : connections) {
			conn.close();
		}
	}
}
