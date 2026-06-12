package net.logiench.shardLib.database.autoIncrement;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public interface AutoIncrementBatchStrategy {
	/**
	 * 要素を挿入し、AutoIncrementの値を取得します。
	 * SQLiteでは<code>getGeneratedKeys</code>ですが、それに対応します。
	 *
	 * @param insertStmt  実行するSQLのプレースホルダ式SQLコードが指定されたPreparedStatement
	 * @param stmt        SQLiteでAutoIncrementの値を取得するためのStatement
	 * @param checkIndex  返り値がtrueの間設定処理を繰り返します
	 * @param setPrepared プレースホルダに値を設定する処理
	 * @return <code>setPrepared</code>で処理した順番のAutoIncrementの値のリスト
	 *
	 */
	List<Long> executeAndMapKeys(PreparedStatement insertStmt, Statement stmt, Function<Integer, Boolean> checkIndex, BiConsumer<PreparedStatement, Integer> setPrepared) throws SQLException;

}
