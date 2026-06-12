package net.logiench.shardCore.db

import com.google.gson.GsonBuilder
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import com.google.inject.Inject
import com.google.inject.Injector
import com.google.inject.Singleton
import net.kyori.adventure.text.Component
import net.logiench.shardCore.ShardCore
import net.logiench.shardCore.core.item.base.def.ShardItem
import net.logiench.shardCore.db.service.PlayerJobBaseService
import net.logiench.shardCore.util.ClassUtils
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

@Singleton
class ServiceManager @Inject private constructor(
	injector: Injector,
	private val manager: DatabaseManager
) : Listener {

	// servicesAllに関係なくこれだけはコードで使用するのでinjectorで取得できるように
	private val profileMapping: PlayerJobBaseService by lazy { injector.getInstance(PlayerJobBaseService::class.java) }

	// ServiceからManagerを呼び出すものがあったので、循環防止
	private val services: Map<TargetDatabase, List<DatabaseService>> by lazy { servicesAll.first }
	private val playerServices: Map<TargetDatabase, List<PlayerDatabaseService>> by lazy { servicesAll.second }
	private val profileServices: Map<TargetDatabase, List<ProfileDatabaseService>> by lazy { servicesAll.third }

	private val servicesAll: Triple<
			Map<TargetDatabase, List<DatabaseService>>,
			Map<TargetDatabase, List<PlayerDatabaseService>>,
			Map<TargetDatabase, List<ProfileDatabaseService>>
			> by lazy {
		// 呼び出されたタイミングで初めてインスタンスを検索・生成
		val instances = ClassUtils.findSubClasses(
			DatabaseService::class.java,
			"net.logiench.shardCore.db.service"
		).map { injector.getInstance(it) }.sorted().toList()

		Triple(
			instances.groupBy { it.targetDatabase },
			instances.filterIsInstance<PlayerDatabaseService>().groupBy { it.targetDatabase },
			instances.filterIsInstance<ProfileDatabaseService>().groupBy { it.targetDatabase }
		)
	}

	private val playerStates = ConcurrentHashMap<UUID, SessionState>()
	private val activeTasks = ConcurrentHashMap<UUID, CompletableFuture<*>>()

	fun loadServiceClasses() {
		// Reflectionは時間がかかるので特定のタイミングで読み込ませる
		servicesAll
	}

	/*
	アンロード中にロード処理が呼ばれたら無視する
	ロード中にアンロードが呼ばれたらhandleでつなげる
	ロード処理に失敗したらclearCacheを呼び、保存させないようにする
	アンロード処理に失敗したらログを表示してデータを残す
	 */

	@EventHandler(priority = EventPriority.LOWEST)
	private fun onPlayerJoin(ev: AsyncPlayerPreLoginEvent) {
		val playerId = ev.uniqueId
		// データの保存中ならkickする
		if (playerStates[playerId] == SessionState.UNLOADING) {
			ev.disallow(
				AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
				Component.text("データを保存中です。しばらく経ってから参加しなおしてください")
			)
			return
		}
		// データのロード処理
		playerStates[playerId] = SessionState.LOADING
		val context = PlayerDatabaseService.LoginContext(playerId, ev.name, ev.address.hostAddress)
		val future = manager.executeSequentialAsync(reversedValue(playerServices), true) {
			it.loadPlayer(context)
		}
		activeTasks[playerId] = future
		try {
			// ここでスレッドを止めることでデータのロードが完了するまでプレイヤーがサーバーに参加できないようにする
			future.join()
			playerStates[playerId] = SessionState.IDLE
		} catch (ex: Exception) {
			ev.disallow(
				AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
				Component.text("データのロード中にエラーが発生しました")
			)
			playerStates.remove(playerId)
			playerServices.values.forEach { services ->
				services.forEach { it.clearCache(playerId) }
			}
			ex.printStackTrace()
			// ここにログやDiscordに通知を表示
		} finally {
			activeTasks.remove(playerId)
		}

	}

	@EventHandler(priority = EventPriority.HIGHEST)
	private fun onPlayerQuit(ev: PlayerQuitEvent) {
		val playerId = ev.player.uniqueId
		// Quitされたらprofileもunloadしないといけない けどService作成していないので仮
		val profileId = profileMapping.getProfileId(playerId)

		val currentTask = activeTasks[playerId] ?: getEmptyFuture()
		// もし他のタスクを実行中の場合、その完了を待ってから保存を開始する
		val unloadTask = currentTask.thenCompose {
			playerStates[playerId] = SessionState.UNLOADING

			// プロファイルが存在すれば保存処理（非同期）
			(if (profileId != null) {
				manager.executeSequentialAsync(profileServices) { it.unloadProfile(playerId, profileId) }
			} else {
				getEmptyFuture()
			}).thenCompose {
				// プロファイルの保存が終わったら、プレイヤーの保存を始める
				manager.executeAsyncAll(playerServices) { it.unloadPlayer(playerId) }
			}.thenAccept {
				// すべての保存が終わったらキャッシュの削除を行う
				if (profileId != null) {
					profileServices.values.flatten().forEach { service -> service.clearCache(playerId, profileId) }
				}
				playerServices.values.flatten().forEach { service -> service.clearCache(playerId) }
			}.exceptionally {
				if (profileId == null) {
					dumpPlayer(DumpReason.UNLOAD_PLAYER, playerId)
				} else {
					profileServices.values.forEach { services ->
						services.forEach {
							it.clearCache(
								playerId,
								profileId
							)
						}
					}
					dumpPlayerAndProfile(DumpReason.UNLOAD_PLAYER_AND_PROFILE, playerId, profileId)
				}
				playerServices.values.forEach { services -> services.forEach { it.clearCache(playerId) } }
				null
			}
		}.exceptionally {
			ShardCore.getPLogger().warning("プレイヤー ${ev.player.name} はエラー状態のため保存をスキップします。")

			if (profileId == null) {
				dumpPlayer(DumpReason.BEFORE_UNLOAD_PLAYER, playerId)
			} else {
				dumpPlayerAndProfile(DumpReason.BEFORE_UNLOAD_PLAYER, playerId, profileId)
				profileServices.values.forEach { services ->
					services.forEach {
						it.clearCache(
							playerId,
							profileId
						)
					}
				}
			}
			playerServices.values.forEach { services -> services.forEach { it.clearCache(playerId) } }
			null
		}

		// 削除処理を書くより前に登録しないと消えない場合がある
		activeTasks[playerId] = unloadTask

		// 最後に絶対にクリーンアップする (finallyに相当)
		unloadTask.whenComplete { _, ex ->
			if (ex != null) ex.printStackTrace() // セーブ失敗時のログ
			playerStates.remove(playerId)
			activeTasks.remove(playerId)
		}
	}

	/**
	 * Profileをロードする場合、このメソッドを呼び出して紐づくデータを取り出す必要があります
	 */
	fun loadProfileAsync(player: Player, profileId: Int): CompletableFuture<Boolean> {
		val playerId = player.uniqueId

		// IDLE状態じゃない（すでにロード中・セーブ中）なら弾く
		if (playerStates[playerId] != SessionState.IDLE) {
			return CompletableFuture.completedFuture(false)
		}

		playerStates[playerId] = SessionState.LOADING

		// 優先度順にロードを実行
		val future = manager.executeAsyncAll(reversedValue(profileServices)) {
			it.loadProfile(playerId, profileId)
		}.handle { _, ex ->
			if (ex != null) {
				// ここにログやDiscordに通知を表示
				ShardCore.getPLogger()
					.severe("プレイヤー ${player.name}(${playerId}) のプロファイル(${profileId})のロードに失敗しました！")
				ex.printStackTrace()

				// DBには保存せず、メモリ上の残骸だけを破棄する
				profileServices.values.flatten().forEach { it.clearCache(playerId, profileId) }
			}

			// 成功なら true, 失敗なら false を返す
			ex == null
		}

		activeTasks[playerId] = future

		future.whenComplete { _, _ ->
			// 状態をリセットしてロック解除
			activeTasks.remove(playerId)
			playerStates[playerId] = SessionState.IDLE
		}

		return future
	}

	/**
	 * Profileをアンロードする場合、このメソッドを呼び出して紐づくデータを保存する必要があります。
	 * @return この値を用いて正常にアンロードされたことを確認後にデータをロードできるようにしてください
	 */
	fun unloadProfileAsync(player: Player, profileId: Int): CompletableFuture<Boolean> {
		val playerId = player.uniqueId

		// IDLE状態じゃないなら弾く
		if (playerStates[playerId] != SessionState.IDLE) {
			return CompletableFuture.completedFuture(false)
		}

		playerStates[playerId] = SessionState.UNLOADING

		// 優先度順（ロードの逆順）にアンロード（保存）を実行
		val future = manager.executeAsyncAll(profileServices) {
			it.unloadProfile(playerId, profileId)
		}.handle { _, ex ->
			if (ex != null) {
				// セーブ失敗時にはログを表示して破損したデータのキャッシュを破棄する
				ShardCore.getPLogger()
					.severe("プレイヤー ${player.name}(${playerId}) のプロファイル(${profileId})のセーブに失敗しました！")
				ex.printStackTrace()

				dumpProfile(DumpReason.UNLOAD_PROFILE, playerId, profileId)
				profileServices.values.flatten().forEach { it.clearCache(playerId, profileId) }
			}


			ex == null
		}

		activeTasks[playerId] = future

		return future.whenComplete { _, _ ->
			// 状態をリセットしてロック解除
			activeTasks.remove(playerId)
			playerStates[playerId] = SessionState.IDLE
		}
	}

	fun saveAllAsync(): CompletableFuture<Void?> =
		manager.executeAsyncAll(services) { it.saveAll() }
			// エラーが起きた際のデータダンプ処理
			.exceptionally {
				dumpAllPlayerAndProfile()
				null
			}

	private fun getEmptyFuture(): CompletableFuture<Void?> = CompletableFuture.completedFuture(null)

	private fun <T> reversedValue(services: Map<TargetDatabase, List<T>>): Map<TargetDatabase, List<T>> {
		return services.mapValues { (_, list) -> list.reversed() }
	}

	private fun getState(playerId: UUID): SessionState = playerStates.getOrDefault(playerId, SessionState.IDLE)

	private enum class SessionState {
		IDLE,       // 何もしていない（安定状態）
		LOADING,    // ロード中
		UNLOADING,  // 保存（アンロード）中
	}


	private fun dumpPlayer(reason: DumpReason, playerId: UUID) {
		EmergencyDataDumper.dumpData(
			playerId, null
		) {
			val dumpData = mutableMapOf<String, Any>()
			dumpData["dump_reason"] = reason.name
			dumpData["player_data"] = playerServices.values.flatten().mapNotNull {
				it.getCacheSnapshot(playerId)
			}
			dumpData
		}
	}

	private fun dumpProfile(reason: DumpReason, playerId: UUID, profileId: Int) {
		EmergencyDataDumper.dumpData(
			playerId, profileId
		) {
			val dumpData = mutableMapOf<String, Any>()
			dumpData["dump_reason"] = reason.name
			dumpData["profile_data"] = profileServices.values.flatten().mapNotNull {
				it.getCacheSnapshot(playerId, profileId)
			}
			dumpData
		}
	}

	private fun dumpPlayerAndProfile(reason: DumpReason, playerId: UUID, profileId: Int) {
		EmergencyDataDumper.dumpData(
			playerId, profileId
		) {
			val dumpData = mutableMapOf<String, Any>()
			dumpData["dump_reason"] = reason.name
			dumpData["player_data"] = playerServices.values.flatten().mapNotNull {
				it.getCacheSnapshot(playerId)
			}
			dumpData["profile_data"] = profileServices.values.flatten().mapNotNull {
				it.getCacheSnapshot(playerId, profileId)
			}
			dumpData
		}
	}

	private fun dumpAllPlayerAndProfile() {
		EmergencyDataDumper.dumpAllActiveData({
			val dumpData = mutableMapOf<String, Any>()
			dumpData["dump_reason"] = DumpReason.SAVE_ALL.name
			dumpData
		}) { onPlayer ->
			// ProfileService: profileIdのスナップショットを1エントリずつコールバックに渡す
			profileServices.values.flatten().forEach { service ->
				service.getCacheSnapshot()?.forEach { (profileId, data) ->
					// nullの場合、すでに存在しないプレイやーかJobBaseのキャッシュが破壊されている。dumpのファイル名にplayerIdが必要なのでどうにもできない
					val playerId = profileMapping.getPlayerId(profileId) ?: return@forEach
					onPlayer(
						playerId, profileId,
						mapOf(service.javaClass.simpleName to data, "dump_reason" to DumpReason.SAVE_ALL.name)
					)
				}
			}
			// PlayerService: UUIDのスナップショットを1エントリずつコールバックに渡す
			playerServices.values.flatten().forEach { service ->
				service.getCacheSnapshot()?.forEach { (playerId, data) ->
					val profileId = profileMapping.getProfileId(playerId)
					onPlayer(
						playerId, profileId,
						mapOf(service.javaClass.simpleName to data, "dump_reason" to DumpReason.SAVE_ALL.name)
					)
				}
			}
		}
	}

	private enum class DumpReason {
		BEFORE_UNLOAD_PLAYER,
		UNLOAD_PLAYER,
		UNLOAD_PLAYER_AND_PROFILE,
		UNLOAD_PROFILE,
		SAVE_ALL,
	}
}

private object EmergencyDataDumper {
	private const val DIRECTORY_NAME = "error_cache_dumps"
	private val gson = GsonBuilder()
		.setPrettyPrinting()
		// ShardItemがシリアライズされた場合はItemIdに変換する
		.registerTypeAdapter(ShardItem::class.java, JsonSerializer<ShardItem> { src, _, _ ->
			JsonPrimitive("ShardItem[${src.id}]")
		}).create()
	private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")

	/**
	 * 収集したデータをJSONとしてファイルに保存します。
	 */
	fun dumpData(playerId: UUID, profileId: Int?, dataMap: () -> Map<String, Any>) {
		playerDumpData(playerId, profileId, null, dataMap) {

			// TODO: ここに Discord Webhook 送信処理を入れてエラーに迅速に対応できるようにする。 saveAll(dumpAllActiveData)のほうにも同じく
		}
	}

	fun dumpAllActiveData(
		serverDataMap: () -> Map<String, Any>,
		// UUID, profileId, そのエントリのデータを1件ずつ渡すコールバック。中間リストを作らずOOMリスクを軽減する
		eachPlayerDataProducer: (onPlayer: (UUID, Int?, Map<String, Any>) -> Unit) -> Unit
	) {
		val plugin = ShardCore.getInstance()
		val timestamp = LocalDateTime.now().format(formatter)

		// 今回のクラッシュ専用のディレクトリを作成
		val crashDir = File(plugin.dataFolder, "${DIRECTORY_NAME}/${timestamp}_SAVE_ALL")
		if (!crashDir.exists()) crashDir.mkdirs()

		// 1. グローバルデータのダンプ
		dumpToFile(File(crashDir, "global_services.json"), serverDataMap)

		val logger = ShardCore.getPLogger()
		logger.info("=".repeat(30))
		logger.info("SAVE_ALL の処理中にエラーが発生しました。現時点のデータをjsonファイルとして保存します")
		logger.info("Path: ${crashDir.absolutePath}")
		logger.info("=".repeat(30))
		// 2. コールバックから1エントリずつ受け取り、プレイヤーごとに集約してからファイルに書き出す
		val accumulator = mutableMapOf<Pair<UUID, Int?>, MutableMap<String, Any>>()
		eachPlayerDataProducer { playerId, profileId, data ->
			accumulator.getOrPut(playerId to profileId) { mutableMapOf() }.putAll(data)
		}
		for ((key, dataMap) in accumulator) {
			playerDumpData(key.first, key.second, crashDir, { dataMap })
		}
		plugin.logger.info("全プレイヤーのダンプを完了しました")
		logger.info("=".repeat(30))
	}

	private fun playerDumpData(
		playerId: UUID, profileId: Int?,
		folder: File? = null,
		dataMap: () -> Map<String, Any>,
		onDump: (File) -> Unit = {},
	) {
		val plugin = ShardCore.getInstance()
		val dumpDir = folder ?: File(plugin.dataFolder, DIRECTORY_NAME)
		if (!dumpDir.exists()) dumpDir.mkdirs()

		val timestamp = LocalDateTime.now().format(formatter)
		// 例: 2026-05-10_15-30-00_uuid_profile.json
		val file = File(
			dumpDir, "${timestamp}_${playerId}${
				if (profileId == null) "" else "_$profileId"
			}.json"
		)

		dumpToFile(file, dataMap, onDump)
	}

	private fun dumpToFile(
		file: File, dataMap: () -> Map<String, Any>, onDump: (File) -> Unit = {}
	) {
		val logger = ShardCore.getPLogger()
		// シリアライズ失敗（循環参照・ErrorなどThrowable全般）を先にキャッチし、toString()でフォールバック出力
		val jsonString = try {
			gson.toJson(dataMap())
		} catch (e: Throwable) {
			logger.severe("ダンプデータのシリアライズに失敗しました。生データをコンソールに出力します")
			e.printStackTrace()
			logger.info("-".repeat(30))
			logger.info(dataMap().toString())
			logger.info("-".repeat(30))
			return
		}
		// ファイル書き込み失敗時はJSONをコンソールに出力して最低限のログを残す
		try {
			file.writeText(jsonString)
			logger.info("ダンプファイルを作成しました。 ${file.path}")
			onDump(file)
		} catch (e: Exception) {
			logger.severe("ダンプファイルの作成に失敗しました。JSONデータをコンソールに出力します")
			e.printStackTrace()
			logger.info("-".repeat(30))
			// どうにかしてデータログを残すためにコンソールに流す
			logger.info(jsonString)
			logger.info("-".repeat(30))
		}
	}
}