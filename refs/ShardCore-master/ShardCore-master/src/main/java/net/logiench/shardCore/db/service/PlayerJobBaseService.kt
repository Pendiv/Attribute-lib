package net.logiench.shardCore.db.service

import com.google.inject.Inject
import com.google.inject.Singleton
import net.logiench.shardCore.db.DatabaseManager
import net.logiench.shardCore.db.PlayerDatabaseService
import net.logiench.shardCore.db.ServiceManager
import net.logiench.shardCore.db.repository.Job
import net.logiench.shardCore.db.repository.JobBaseEntity
import net.logiench.shardCore.db.repository.JobBaseRepository
import org.bukkit.entity.Player
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

@Singleton
class PlayerJobBaseService @Inject private constructor(
	private val manager: DatabaseManager,
	private val serviceManager: ServiceManager,
	private val mapping: PlayerMappingService,
	private val repository: JobBaseRepository
) : PlayerDatabaseService {

	override val loadingPriority = Int.MAX_VALUE - 1
	override val targetDatabase = repository.getTarget()

	private val jobMapping: MutableMap<UUID, JobBaseEntity> = ConcurrentHashMap()

	override fun loadPlayer(context: PlayerDatabaseService.LoginContext) {
		// ロードはプレイヤー個人のタイミング
	}

	override fun unloadPlayer(playerId: UUID) {
		val entity = jobMapping[playerId] ?: return
		// 退出と同時にプロファイルのアンロードがあったら時間を更新
		repository.updateLastLogin(entity.profileId, LocalDateTime.now())
	}

	/**
	 * プレイヤーのプロファイルをロードします。
	 * データがなければ新規作成します。
	 * @return ロードしたEntity
	 */
	fun loadProfile(player: Player, job: Job): CompletableFuture<JobBaseEntity?> {
		val playerId = player.uniqueId
		// すでにプロファイルが選択されていたら無視
		if (jobMapping.containsKey(playerId)) return CompletableFuture.completedFuture(null)
		val seasonPlayerId = mapping.getSeasonPlayerId(playerId)

		return manager.supplyAsync(repository.getTarget()) {
			// DBからデータを取得し、データがなければ新規作成する
			val entity = repository.selectByPlayerAndJobId(seasonPlayerId, job).orElse(null)
			val now = LocalDateTime.now()
			if (entity != null) {
				// ロード時に最終ログインを更新
				repository.updateLastLogin(entity.profileId, now)
				entity
			} else {
				JobBaseEntity(
					repository.insertAndGetId(seasonPlayerId, job, now, now),
					seasonPlayerId, job, now, now, null
				)
			}
		}.thenCompose { entity ->
			jobMapping[playerId] = entity
			serviceManager.loadProfileAsync(player, entity.profileId)
				.handle { isSuccess, ex ->
					if (isSuccess && ex == null) {
						entity
					} else {
						// loadProfileAsyncで失敗した時のキャッシュ削除
						jobMapping.remove(playerId)
						null
					}
				}
		}.exceptionally {
			jobMapping.remove(playerId)
			it.printStackTrace()
			null
		}
	}

	/**
	 * プレイヤーのプロファイルをアンロードします。
	 * プレイヤーが退出した際は自動で呼び出されます。
	 * @return アンロードの結果
	 */
	fun unloadProfile(player: Player): CompletableFuture<Boolean> {
		val playerId = player.uniqueId
		val entity = jobMapping.remove(playerId) ?: return CompletableFuture.completedFuture(false)
		return manager.supplyAsync(repository.getTarget()) {
			repository.updateLastLogin(entity.profileId, LocalDateTime.now())
		}.thenCompose {
			serviceManager.unloadProfileAsync(player, entity.profileId)
		}.exceptionally {
			it.printStackTrace()
			false
		}
	}

	override fun clearCache(playerId: UUID) {
		jobMapping.remove(playerId)
	}

	override fun saveAll() {
		jobMapping.forEach { (playerId, entity) ->
			val now = LocalDateTime.now()
			val newTable = entity.copy(lastLogin = now)
			jobMapping[playerId] = newTable
			repository.updateLastLogin(newTable.profileId, now)
		}
	}

	fun getEntity(playerId: UUID): JobBaseEntity? {
		return jobMapping[playerId]
	}

	fun getProfileId(playerId: UUID): Int? {
		return getEntity(playerId)?.profileId
	}

	fun getPlayerId(profileId: Int): UUID? {
		return jobMapping.entries.firstOrNull { it.value.profileId == profileId }?.key
	}

	override fun getCacheSnapshot(playerId: UUID): Any? {
		return jobMapping[playerId]
	}

	override fun getCacheSnapshot(): Map<UUID, Any> {
		return jobMapping.toMap()
	}
}
