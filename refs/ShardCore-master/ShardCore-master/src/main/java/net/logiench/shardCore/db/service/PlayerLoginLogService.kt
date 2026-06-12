package net.logiench.shardCore.db.service

import com.google.inject.Inject
import com.google.inject.Singleton
import net.logiench.shardCore.db.PlayerDatabaseService
import net.logiench.shardCore.db.repository.PlayerLoginLogRepository
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Singleton
class PlayerLoginLogService @Inject private constructor(
	private val repository: PlayerLoginLogRepository
) : PlayerDatabaseService {

	private val playerLogMapping: MutableMap<UUID, Pair<Long, LocalDateTime>> = ConcurrentHashMap()

	override val loadingPriority = Integer.MIN_VALUE
	override val targetDatabase = repository.getTarget()

	override fun loadPlayer(context: PlayerDatabaseService.LoginContext) {
		val playerId = context.playerId
		val now = LocalDateTime.now()
		playerLogMapping[playerId] = repository.insert(playerId, now, context.ipAddress) to now
	}

	override fun unloadPlayer(playerId: UUID) {
		val (id, loginAt) = playerLogMapping.remove(playerId) ?: return
		val now = LocalDateTime.now()

		repository.updateTime(
			id,
			getPlayTimeSecond(now, loginAt),
			LocalDateTime.now()
		)
	}

	override fun clearCache(playerId: UUID) {
		playerLogMapping.remove(playerId)
	}

	// ログはsaveAllのときにしか更新しないからnullで問題なし
	override fun getCacheSnapshot(playerId: UUID) = null

	override fun getCacheSnapshot() = null

	override fun saveAll() {
		val now = LocalDateTime.now()
		playerLogMapping.forEach { _, (id, loginAt) ->
			repository.updateTime(
				id,
				getPlayTimeSecond(now, loginAt)
			)
		}
	}

	private fun getPlayTimeSecond(now: LocalDateTime, loginAt: LocalDateTime) =
		Math.toIntExact(ChronoUnit.SECONDS.between(loginAt, now))
}