package net.logiench.shardCore.db

import com.google.common.collect.ImmutableMap
import com.google.inject.Inject
import com.google.inject.Singleton
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import net.logiench.shardCore.config.data.DatabaseConfigState
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.function.Supplier
import java.util.regex.Pattern

enum class TargetDatabase(val key: String, val loadingPriority: Int) {
	MAIN("main", Integer.MAX_VALUE),
	SEASON("season", 0),
}

@Singleton
class DatabaseManager @Inject private constructor(
	config: DatabaseConfigState, val tableManager: TableManager
) {

	private val executor: ExecutorService = Executors.newFixedThreadPool(10)
	private val targetDatabases: ImmutableMap<TargetDatabase, DatabaseDataSource>

	init {
		val databases = EnumMap<TargetDatabase, DatabaseDataSource>(TargetDatabase::class.java)
		for (target in TargetDatabase.entries) {
			val state = config.getState(target) ?: continue

			val hikariConfig = HikariConfig()
			state.databaseType.applyHikariConfig(hikariConfig, state, target)
			if (!state.databaseType.isRemote) {
				prepareFolderFromUrl(hikariConfig.jdbcUrl)
			}
			val hikariDataSource = HikariDataSource(hikariConfig)
			val database = Database.connect(hikariDataSource)
			databases[target] = DatabaseDataSource(hikariDataSource, database)
		}
		targetDatabases = ImmutableMap.copyOf(databases)
	}

	fun createTables() {
		for (target in tableManager.getTargetDatabases()) {
			transaction(targetDatabases[target]!!.database) {
				// バッチ処理のが効率的に思えるけどSQLiteだとエラー吐くのでfalse。デフォルトfalseだけど重要なので書いてる
				SchemaUtils.create(*tableManager.getTables(target).toTypedArray(), inBatch = false)
			}
		}
	}

	fun executeAsync(target: TargetDatabase, action: Runnable): CompletableFuture<Void> {
		val db = targetDatabases[target]?.database
			?: return CompletableFuture.failedFuture(IllegalArgumentException("Unknown target: $target"))

		return CompletableFuture.runAsync({
			transaction(db) {
				action.run()
			}
		}, executor)
	}

	/**
	 * データベースごとの優先度に従って1つずつ処理を行います。
	 */
	fun <T> executeSequentialAsync(
		targets: Map<TargetDatabase, List<T>>,
		reverseOrder: Boolean = false,
		action: (T) -> Unit
	): CompletableFuture<Void> {
		val sortedEntities = if (reverseOrder) {
			targets.entries.sortedByDescending { it.key.loadingPriority }
		} else {
			targets.entries.sortedBy { it.key.loadingPriority }
		}
		var currentFuture: CompletableFuture<Void> = CompletableFuture.completedFuture(null)

		for ((target, values) in sortedEntities) {
			if (values.isEmpty()) continue
			currentFuture = currentFuture.thenCompose {
				executeAsync(target) {
					values.forEach(action)
				}
			}
		}
		return currentFuture
	}

	/**
	 * すべてのデータベースに対して同時に接続し、一括して処理を行います。
	 */
	fun <T> executeAsyncAll(targets: Map<TargetDatabase, List<T>>, action: (T) -> Unit): CompletableFuture<Void> {
		val threads = ArrayList<CompletableFuture<Void>>(targets.size)
		for ((target, values) in targets) {
			if (values.isEmpty()) continue
			threads.add(executeAsync(target) {
				values.forEach(action)
			})
		}
		return CompletableFuture.allOf(*threads.toTypedArray())
	}

	fun <T> supplyAsync(target: TargetDatabase, action: Supplier<T>): CompletableFuture<T> {
		val db = targetDatabases[target]?.database
			?: return CompletableFuture.failedFuture(IllegalArgumentException("Unknown target: $target"))

		return CompletableFuture.supplyAsync({
			transaction(db) {
				action.get()
			}
		}, executor)
	}

	fun shutdown() {
		for (databaseDataSource in targetDatabases.values) {
			val source = databaseDataSource.dataSource
			if (source.isClosed) continue
			// 必ずすべてcloseを実行させるためのtry catch
			try {
				databaseDataSource.dataSource.close()
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
		executor.shutdown()
	}

	private fun prepareFolderFromUrl(jdbcUrl: String) {
		// 正規表現: 最後のコロン以降で、コロンを含まない末尾までの文字列
		// パラメータ (? ... ) を考慮して少し調整
		val pattern = Pattern.compile("(?<=:)[^:]+?$")
		val matcher = pattern.matcher(jdbcUrl)

		if (matcher.find()) {
			val fullPath = matcher.group()

			// もしURLにクエリパラメータ (? 以降) が含まれている場合は除去
			val cleanPath = fullPath.split("\\?".toRegex())[0]

			val filePath = Paths.get(cleanPath)
			val parentDir = filePath.parent

			if (parentDir != null) {
				try {
					Files.createDirectories(parentDir)
				} catch (e: IOException) {
					e.printStackTrace()
				}
			}
		}
	}

	private data class DatabaseDataSource(
		val dataSource: HikariDataSource,
		val database: Database
	)
}
