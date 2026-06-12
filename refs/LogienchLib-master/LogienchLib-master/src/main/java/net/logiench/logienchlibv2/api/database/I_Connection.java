package net.logiench.logienchlibv2.api.database;

public interface I_Connection {
	boolean isClosed();

	default boolean isNotClosed() {
		return !isClosed();
	}

	void close();
}
