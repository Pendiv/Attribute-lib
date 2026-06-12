package net.logiench.shardLib.database.autoIncrement;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class SQLiteBatchStrategy implements AutoIncrementBatchStrategy {
	@Override
	public List<Long> executeAndMapKeys(PreparedStatement insertStmt, Statement stmt, Function<Integer, Boolean> checkIndex, BiConsumer<PreparedStatement, Integer> setPrepared) throws SQLException {
		List<Long> autoIncrements = new ArrayList<>();
		int i = 0;
		while (checkIndex.apply(i)) {
			setPrepared.accept(insertStmt, i++);
			insertStmt.executeUpdate(); // 一件ずつ実行し、autoIncrementの値を取得する
			try (ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
				if (rs.next()) {
					long newId = rs.getLong(1);
					autoIncrements.add(newId);
				}
			}
		}
		return autoIncrements;
	}
}
