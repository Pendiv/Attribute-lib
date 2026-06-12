package net.logiench.shardCore.db.service

import com.google.inject.Inject
import com.google.inject.Singleton
import net.logiench.shardCore.db.PlayerDatabaseService
import net.logiench.shardCore.db.repository.PlayerMappingRepository
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Singleton
class PlayerMappingService @Inject private constructor(
	private val repository: PlayerMappingRepository
) : PlayerDatabaseService {

	private val playerMapping: MutableMap<UUID, Int> = ConcurrentHashMap()
	private val playerReverseMapping: MutableMap<Int, UUID> = ConcurrentHashMap()

	override val loadingPriority = Int.MAX_VALUE
	override val targetDatabase = repository.getTarget()

	override fun loadPlayer(context: PlayerDatabaseService.LoginContext) {
		val playerId = context.playerId
		val seasonPlayerId = repository.insertOrGetId(context.playerId)

		playerMapping[playerId] = seasonPlayerId
		playerReverseMapping[seasonPlayerId] = playerId
	}

	override fun unloadPlayer(playerId: UUID) {
		// データは更新時に逐次適応なので保存なし
	}

	override fun clearCache(playerId: UUID) {
		playerMapping.remove(playerId)?.let {
			playerReverseMapping.remove(it)
		}
	}

	fun getSeasonPlayerId(playerId: UUID): Int =
		getSeasonPlayerIdOrNull(playerId)
			?: throw NullPointerException("SeasonPlayerId は PlayerId: '$playerId' に存在しないか、オンラインではありません")

	fun getSeasonPlayerIdOrNull(playerId: UUID): Int? =
		playerMapping[playerId]

	fun getPlayerId(seasonPlayerId: Int): UUID =
		getPlayerIdOrNull(seasonPlayerId)
			?: throw NullPointerException("PlayerId は SeasonPlayerId: '$seasonPlayerId' に存在しないか、オンラインではありません")

	fun getPlayerIdOrNull(seasonPlayerId: Int): UUID? =
		playerReverseMapping[seasonPlayerId]

	override fun getCacheSnapshot(playerId: UUID): Any? = playerMapping[playerId]

	override fun getCacheSnapshot(): Map<UUID, Any> = playerMapping.toMap()

	override fun saveAll() {
	}
}
