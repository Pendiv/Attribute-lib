package net.logiench.logienchlibv2.api.database.mysql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.logiench.logienchlibv2.base.database.AutoCloser;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

/**
 * HikariDataSourceの接続を管理します。
 * autoCloseをtrueにすると、LogienchLibのonDisableが動いた際に自動で閉じられます。
 */
@SuppressWarnings("unused")
public class HikariConnection implements I_DBConnection {
	/**
	 * MySQL用のDB-urlを取得します。
	 */
	public static String toJdbcUrl(@NotNull String host, int port, @NotNull String database) {
		return "jdbc:mysql://" + host + ":" + port + "/" + database;
	}

	private final HikariDataSource hikari;

	/**
	 * MySQL用の接続を行います。
	 * 一部のhikari設定が自動で行われます。
	 */
	public HikariConnection(@NotNull String host, int port, @NotNull String database, @NotNull String username, @NotNull String password) {
		this(host, port, database, username, password, true);
	}

	/**
	 * MySQL用の接続を行います。
	 * 一部のhikari設定が自動で行われます。
	 */
	public HikariConnection(@NotNull String host, int port, @NotNull String database, @NotNull String username, @NotNull String password, boolean autoClose) {
		this(toJdbcUrl(host, port, database),
			username, password, autoClose);
	}

	/**
	 * 一部のhikari設定が自動で行われます。
	 */
	public HikariConnection(@NotNull String url, @NotNull String username, @NotNull String password) {
		this(url, username, password, true);
	}

	/**
	 * 一部のhikari設定が自動で行われます。
	 */
	public HikariConnection(@NotNull String url, @NotNull String username, @NotNull String password, boolean autoClose) {
		this(new HikariConfig() {{
			//接続先のurlを指定する `jdbc:mysql://ホスト:ポート/DB名`
			setJdbcUrl(url);
			//コンパイルしたSQL文(プリペアドステートメント)をキャッシュするか
			addDataSourceProperty("cachePrepStmts", "true");
			//キャッシュするプリペアドステートメントの数
			addDataSourceProperty("prepStmtCacheSize", "250");
			//キャッシュするプリペアドステートメントのSQL文の最大長を指定
			addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
			//サーバー側でもSQL文をキャッシュし、ネットワークのリソース削減するか
			addDataSourceProperty("useServerPrepStmts", "true");

			setUsername(username);
			setPassword(password);
		}});
	}

	/**
	 * MySQL用の接続を行います
	 */
	public HikariConnection(@NotNull String host, int port, @NotNull String database, @NotNull String username, @NotNull String password, @NotNull HikariConfig config) {
		this(host, port, database, username, password, config, true);
	}

	/**
	 * MySQL用の接続を行います
	 */
	public HikariConnection(@NotNull String host, int port, @NotNull String database, @NotNull String username, @NotNull String password, @NotNull HikariConfig config, boolean autoClose) {
		this(toJdbcUrl(host, port, database), username, password, config, autoClose);
	}

	public HikariConnection(@NotNull String url, @NotNull String username, @NotNull String password, @NotNull HikariConfig config) {
		this(url, username, password, config, true);
	}

	public HikariConnection(@NotNull String url, @NotNull String username, @NotNull String password, @NotNull HikariConfig config, boolean autoClose) {
		this(((Function<HikariConfig, HikariConfig>) (f -> {
			f.setJdbcUrl(url);
			f.setUsername(username);
			f.setPassword(password);
			return f;
		})).apply(config), autoClose);
	}

	public HikariConnection(@NotNull HikariConfig config) {
		this(config, true);
	}

	public HikariConnection(@NotNull HikariConfig config, boolean autoClose) {
		hikari = new HikariDataSource(config);

		if (autoClose) {
			AutoCloser.add(this);
		}
	}

	public HikariDataSource getHikari() {
		return hikari;
	}

	@Override
	public Connection getConnection() {
		try {
			return hikari.getConnection();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean isClosed() {
		return hikari.isClosed();
	}

	@Override
	public void close() {
		if (isNotClosed()) {
			hikari.close();
		}
	}
}
