package net.logiench.shardCore.db

import com.zaxxer.hikari.HikariConfig
import net.logiench.shardCore.ShardCore
import net.logiench.shardCore.config.data.DatabaseConfigState
import java.io.File

enum class DatabaseType(
	/** リモートDBかどうか */
	val isRemote: Boolean,
	/** このDBタイプに対応する名前リスト */
	private vararg val names: String
) {

	SQLITE(false, "sqlite") {
		override fun applyHikariConfig(
			hikariConfig: HikariConfig,
			config: DatabaseConfigState.State,
			target: TargetDatabase
		) {
			// URLが未指定の場合はデフォルトのファイルパスを使用
			val url = config.url?.takeIf { it.isNotEmpty() }
				?: ("jdbc:sqlite:" + File(ShardCore.getInstance().dataFolder, "data/${target.key}.db").absolutePath)

			hikariConfig.jdbcUrl = url
			hikariConfig.driverClassName = "org.sqlite.JDBC"
			hikariConfig.connectionInitSql = "PRAGMA foreign_keys = ON;"
		}
	},

	MYSQL_MARIADB(true, "mysql", "mariadb") {
		override fun applyHikariConfig(
			hikariConfig: HikariConfig,
			config: DatabaseConfigState.State,
			target: TargetDatabase
		) {
			hikariConfig.jdbcUrl = config.url

			// コンパイルしたSQL文をキャッシュするか
			hikariConfig.addDataSourceProperty("cachePrepStmts", "true")
			// キャッシュするプリペアドステートメントの数
			hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250")
			// キャッシュするプリペアドステートメントのSQL文の最大長を指定
			hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
			// サーバー側でもSQL文をキャッシュし、ネットワークのリソース削減するか
			hikariConfig.addDataSourceProperty("useServerPrepStmts", "true")

			hikariConfig.username = config.username
			hikariConfig.password = config.password
		}
	},

	;

	/** 名前リストの最初の要素を返す */
	fun getNameFirst(): String = names.first()

	/** HikariConfigにDB接続設定を適用する（各enum定数でオーバーライド必須） */
	open fun applyHikariConfig(
		hikariConfig: HikariConfig,
		config: DatabaseConfigState.State,
		target: TargetDatabase
	) {
		throw UnsupportedOperationException()
	}

	companion object {

		/**
		 * 名前からDatabaseTypeを検索して返します。
		 * 見つからない場合は [defaultType] を返します。
		 *
		 * @param name 検索する名前
		 * @param defaultType 見つからない場合のデフォルト値
		 */
		@JvmStatic
		fun fromName(name: String, defaultType: DatabaseType): DatabaseType {
			return entries.firstOrNull { name in it.names } ?: defaultType
		}
	}
}
