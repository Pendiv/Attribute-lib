package net.logiench.shardCore.loader

import com.google.inject.Inject
import com.google.inject.Singleton
import net.logiench.shardCore.ShardCore
import net.logiench.shardCore.loader.ktsEngine.KtsItemEngine
import net.logiench.shardCore.loader.ktsScriptConfiguration.ScriptCache.instance
import net.logiench.shardCore.register.ItemRegistry
import net.logiench.shardCore.util.LoadFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.function.BiConsumer
import kotlin.math.max

@Singleton
class KtsItemLoader @Inject private constructor(private val itemRegistry: ItemRegistry) {
	private val engine = KtsItemEngine

	// 現在進行中のタスクを保持する
	private var currentLoadingTask: CompletableFuture<Void>? = null
	private var currentExecutor: ExecutorService? = null
	private var results: MutableList<LoadResult>? = null
	private var threadPoolSize = 0
	private var startTime: Long = 0
	private var endTime: Long = 0

	/**
	 * 非同期でロードを開始します（onLoadなどで呼び出す）
	 */
	fun startAsyncRegistryAll(rootFile: File, defaultPath: Path?) {
		check(currentLoadingTask == null) { "すでに読み込みは開始されています。完了するには 'waitForCompletion()' を呼び出してください。" }
		val logger = ShardCore.getInstance().logger
		logger.info("KTSアイテムのバックグラウンド・ロードを開始します...")

		if (rootFile.isFile()) {
			logger.warning(rootFile.toPath().toString() + " にディレクトリが必要ですが、既にファイルがあります")
			return
		}
		if (!rootFile.exists()) {
			if (!rootFile.mkdirs()) {
				logger.warning(rootFile.toPath().toString() + " にディレクトリが必要ですが、作成できませんでした")
				return
			}
			createDefault(rootFile, defaultPath)
		}

		this.startTime = System.currentTimeMillis()

		// ファイルへのアクセス履歴をリセット
		instance.resetAccessedFiles()

		// 1. ファイルのスキャン（ここは一瞬なのでメインスレッドで実行）
		val targetFiles = LoadFile.collectAll(rootFile, FILE_EXTENSION)
		if (targetFiles.isEmpty()) {
			logger.info("ファイルが見つかりませんでした。ロードを終了します")
			return
		}

		// 2. スレッドプールの準備
		val threads = max(1, Runtime.getRuntime().availableProcessors() - 1)
		this.currentExecutor = Executors.newFixedThreadPool(threads)
		this.threadPoolSize = threads

		// 3. 非同期タスクのリストを作成
		val futures = mutableListOf<CompletableFuture<Void>>()//ArrayList<CompletableFuture<Void?>?>()
		// ログを一括で出すためにここでまとめる
		this.results = Collections.synchronizedList(mutableListOf())
		for (file in targetFiles) {
			futures.add(CompletableFuture.runAsync({
				var result: LoadResult
				try {
					val item = engine.loadItemScript(file)
					// 登録とID重複チェック（synchronizedされたregisterメソッドを必ず使用すること）
					val registerResult = itemRegistry.registerAndCheck(item)
					result = LoadResult(file.path, registerResult.isSuccess, registerResult.message)
				} catch (t: Throwable) {
					// ExceptionじゃなくてThrowableにすることで、KTSのコンパイルエラーなど、どんなエラーでもキャッチする
					result = LoadResult(file.path, false, t.message)
				}
				results!!.add(result)
			}, currentExecutor))
		}

		// 4. 全体の完了を待つ「予約票」を発行して保持する
		this.currentLoadingTask = CompletableFuture.allOf(*futures.toTypedArray<CompletableFuture<*>?>())
			.whenComplete(BiConsumer { _: Void?, _: Throwable? ->
				this.endTime = System.currentTimeMillis()
			})
	}

	/**
	 * ロードが完了するまで待機します（onEnableなどで呼び出す）
	 */
	fun waitForCompletion() {
		val loadingTask = currentLoadingTask ?: return
		val logger = ShardCore.getInstance().logger
		logger.info("アイテムロードの完了を待機しています...")

		try {
			// ロードが終わるまでここでブロックする
			loadingTask.join()

			// 使用しなかったファイルを削除
			instance.cleanup()
		} finally {
			// スレッドプールの解体
			currentExecutor?.also { it.shutdown() }
			currentExecutor = null
			currentLoadingTask = null
		}

		val duration = endTime - startTime
		printSummary(results!!, duration)
	}

	private fun createDefault(rootFile: File, defaultFilePath: Path?) {
		if (defaultFilePath != null) {
			val `in` = ShardCore.getInstance().getResource(defaultFilePath.toString().replace("\\", "/"))
			if (`in` != null) {
				try {
					val file = rootFile.toPath().resolve(defaultFilePath.fileName).toFile()
					if (file.createNewFile()) {
						val out: OutputStream = FileOutputStream(file)
						val buf = ByteArray(1024)
						var len: Int
						while ((`in`.read(buf).also { len = it }) > 0) {
							out.write(buf, 0, len)
						}
						out.close()
						`in`.close()
					}
				} catch (e: IOException) {
					throw IllegalStateException(e)
				}
			}
		}
	}

	private fun printSummary(results: List<LoadResult>, duration: Long) {
		val logger = ShardCore.getInstance().logger
		val failedCount = results.stream().filter { r: LoadResult? -> !r!!.success }.count()

		logger.info("")
		logger.info("=".repeat(40))
		logger.info("       ShardCore KTS Load Summary       ")
		logger.info("=".repeat(40))

		for (res in results) {
			if (res.success) {
				// 成功は1行でさらっと
				logger.info(String.format("[SUCCESS] %-20s", res.fileName))
			} else {
				// 失敗は目立つように
				logger.warning(String.format("[FAILED ] %-20s", res.fileName))
				var isFirst = true
				for (msg in res.errorMessage!!.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
					if (isFirst) {
						logger.warning(" └─ $msg")
						isFirst = false
					} else {
						logger.warning("    $msg")
					}
				}
			}
		}

		logger.info("=".repeat(40))
		if (failedCount > 0) {
			logger.warning("$failedCount 件のアイテムがロードに失敗しました。詳細は上記を確認してください。")
		} else {
			logger.info("すべてのアイテムが正常にロードされました。")
		}

		logger.info(String.format("読み込みに %,dms かかりました。(スレッドプールサイズ: %d)", duration, threadPoolSize))
		logger.info("=".repeat(40))
		logger.info("")
	}

	@JvmRecord
	private data class LoadResult(val fileName: String?, val success: Boolean, val errorMessage: String?)
	companion object {
		private const val FILE_EXTENSION = "item.kts"
	}
}