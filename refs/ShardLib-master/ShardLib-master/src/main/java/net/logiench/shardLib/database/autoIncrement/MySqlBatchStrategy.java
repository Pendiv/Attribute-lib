package net.logiench.shardLib.database.autoIncrement;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class MySqlBatchStrategy implements AutoIncrementBatchStrategy {
	@Override
	public List<Long> executeAndMapKeys(PreparedStatement insertStmt, Statement stmt, Function<Integer, Boolean> checkIndex, BiConsumer<PreparedStatement, Integer> setPrepared) throws SQLException {
		int i = 0;
		while (checkIndex.apply(i)) {
			setPrepared.accept(insertStmt, i++);
			insertStmt.addBatch();
		}
		insertStmt.executeBatch();
		List<Long> autoIncrements = new ArrayList<>();
		try (ResultSet keys = insertStmt.getGeneratedKeys()) {
			while (keys.next()) {
				autoIncrements.add(keys.getLong(1));
			}
		}
		return autoIncrements;
	}
}
