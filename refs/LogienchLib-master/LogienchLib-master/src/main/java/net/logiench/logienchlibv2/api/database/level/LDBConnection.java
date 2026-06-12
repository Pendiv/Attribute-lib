package net.logiench.logienchlibv2.api.database.level;

import net.logiench.logienchlibv2.LogienchLib;
import net.logiench.logienchlibv2.api.database.I_Connection;
import net.logiench.logienchlibv2.base.database.AutoCloser;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * LevelDBとの接続を管理します。
 * autoCloseをtrueにすると、LogienchLibのonDisableが動いた際に自動で閉じられます。
 */
@SuppressWarnings("unused")
public class LDBConnection implements I_Connection {
	public static final StringByteAdapter UTF_8 = s -> s.getBytes(StandardCharsets.UTF_8);

	private final StringByteAdapter keyAdapter;
	private final DB db;

	/**
	 * 一部のLevelDB設定が自動で行われます。
	 * Stringのbyte[]への変換がUTF-8で行われます。
	 */
	public LDBConnection(@NotNull File ldbFolder) {
		this(ldbFolder, UTF_8);
	}

	/**
	 * 一部のLevelDB設定が自動で行われます。
	 */
	public LDBConnection(@NotNull File ldbFolder, @NotNull StringByteAdapter keyAdapter) {
		this(ldbFolder, keyAdapter, true);
	}

	/**
	 * 一部のLevelDB設定が自動で行われます。
	 * Stringのbyte[]への変換がUTF-8で行われます。
	 */
	public LDBConnection(@NotNull File ldbFolder, @NotNull Options options) {
		this(ldbFolder, options, UTF_8, true);
	}

	/**
	 * 一部のLevelDB設定が自動で行われます。
	 */
	public LDBConnection(@NotNull File ldbFolder, @NotNull Options options, @NotNull StringByteAdapter keyAdapter) {
		this(ldbFolder, options, keyAdapter, true);
	}

	/**
	 * Stringのbyte[]への変換がUTF-8で行われます。
	 */
	public LDBConnection(@NotNull DB db) {
		this(db, UTF_8, true);
	}

	public LDBConnection(@NotNull DB db, @NotNull StringByteAdapter keyAdapter) {
		this(db, keyAdapter, true);
	}

	/**
	 * 一部のLevelDB設定が自動で行われます。
	 * Stringのbyte[]への変換がUTF-8で行われます。
	 */
	public LDBConnection(@NotNull File ldbFolder, boolean autoClose) {
		this(ldbFolder, UTF_8, autoClose);
	}

	/**
	 * 一部のLevelDB設定が自動で行われます。
	 */
	public LDBConnection(@NotNull File ldbFolder, @NotNull StringByteAdapter keyAdapter, boolean autoClose) {
		this(ldbFolder, new Options()
				.createIfMissing(true)
				.blockSize(1024 * 16)
				.compressionType(CompressionType.SNAPPY),
			keyAdapter, autoClose
		);
	}

	/**
	 * Stringのbyte[]への変換がUTF-8で行われます。
	 */
	public LDBConnection(@NotNull File ldbFolder, @NotNull Options options, boolean autoClose) {
		this(ldbFolder, options, UTF_8, autoClose);
	}

	public LDBConnection(@NotNull File ldbFolder, @NotNull Options options, @NotNull StringByteAdapter keyAdapter, boolean autoClose) {
		try {
			this.db = Iq80DBFactory.factory.open(ldbFolder, options);
			this.keyAdapter = keyAdapter;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if (autoClose) {
			AutoCloser.add(this);
		}
	}

	public LDBConnection(@NotNull DB db, boolean autoClose) {
		this(db, UTF_8, autoClose);
	}

	public LDBConnection(@NotNull DB db, @NotNull StringByteAdapter keyAdapter, boolean autoClose) {
		this.db = db;
		this.keyAdapter = keyAdapter;
		if (autoClose) {
			AutoCloser.add(this);
		}
	}

	public LDBManager getManager() {
		return new LDBManager(this);
	}

	@Override
	@Deprecated
	/// @deprecated 必ずfalseが返るため
	public boolean isClosed() {
		return false;
	}

	@Override
	public void close() {
		try {
			db.close();
		} catch (IOException e) {
			// closeは重要な終了処理が後ろに控えているかもしれないので、throwはしない
			LogienchLib.log.error(e.getMessage(), e);
		}
	}

	public byte[] get(String key) {
		return db.get(keyAdapter.getBytes(key));
	}

	public boolean contains(String key) {
		return get(key) != null;
	}

	public void put(String key, byte[] value) {
		db.put(keyAdapter.getBytes(key), value);
	}

	public void remove(String key) {
		db.delete(keyAdapter.getBytes(key));
	}
}

