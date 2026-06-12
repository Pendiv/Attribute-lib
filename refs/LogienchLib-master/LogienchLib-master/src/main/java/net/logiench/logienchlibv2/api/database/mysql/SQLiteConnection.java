package net.logiench.logienchlibv2.api.database.mysql;

import net.logiench.logienchlibv2.LogienchLib;
import net.logiench.logienchlibv2.base.database.AutoCloser;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

@SuppressWarnings("unused")
public class SQLiteConnection implements I_DBConnection {
	private static final String db = "jdbc:sqlite:";

	private final Connection connection;

	public SQLiteConnection(@NotNull File file) {
		this(file, true);
	}

	public SQLiteConnection(@NotNull File file, boolean autoClose) {
		this(file.toPath().getRoot(), file.getName());
	}

	public SQLiteConnection(@NotNull File file, @NotNull Properties properties) {
		this(file, properties, true);
	}

	public SQLiteConnection(@NotNull File file, @NotNull Properties properties, boolean autoClose) {
		this(file.toPath().getRoot(), file.getName(), properties);
	}

	public SQLiteConnection(@NotNull Path folder, @NotNull String fileName) {
		this(folder, fileName, true);
	}

	public SQLiteConnection(@NotNull Path folder, @NotNull String fileName, boolean autoClose) {
		this(folder, fileName, new Properties() {{
			put("journal_mode", "MEMORY");
			put("synchronous", "OFF");
		}});
	}

	public SQLiteConnection(@NotNull Path folder, @NotNull String fileName, @NotNull Properties properties) {
		this(folder, fileName, properties, true);
	}

	public SQLiteConnection(@NotNull Path folder, @NotNull String fileName, @NotNull Properties properties, boolean autoClose) {
		try {
			File folderFile = folder.toFile();
			if (!folderFile.exists()) {
				if (!folderFile.mkdirs()) {
					throw new IOException("Could not create folder: " + folderFile.getAbsolutePath());
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		try {
			Class.forName("org.sqlite.JDBC");

			this.connection = DriverManager.getConnection(db + folder + "/" + fileName, properties);
		} catch (Exception e) {
			throw new RuntimeException("Failed to connect to database!", e);
		}

		if (autoClose) {
			AutoCloser.add(this);
		}
	}

	@Override
	public Connection getConnection() {
		return connection;
	}

	@Override
	public boolean isClosed() {
		try {
			return connection.isClosed();
		} catch (SQLException ignored) {
		}
		return false;
	}

	@Override
	public void close() {
		try {
			if (isNotClosed()) {
				connection.close();
			}
		} catch (SQLException e) {
			// closeは重要な終了処理が後ろに控えているかもしれないので、throwはしない
			LogienchLib.log.error(e.getMessage(), e);
		}
	}
}
