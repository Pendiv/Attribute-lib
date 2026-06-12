package net.logiench.shardCore.db.service

import com.google.inject.Inject
import com.google.inject.Singleton
import net.logiench.shardCore.db.PlayerDatabaseService
import net.logiench.shardCore.db.repository.PlayerBaseEntity
import net.logiench.shardCore.db.repository.PlayerBaseRepository
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Singleton
class PlayerBaseService @Inject private constructor(
	private val repository: PlayerBaseRepository
) : PlayerDatabaseService {

	private val loadedPlayers: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

	override val loadingPriority = Int.MAX_VALUE
	override val targetDatabase = repository.getTarget()

	override fun loadPlayer(context: PlayerDatabaseService.LoginContext) {
		val now = LocalDateTime.now()
		// upsertではfirstLoginを上書きできないようにしているのでこのままでOK
		repository.upsert(PlayerBaseEntity(context.playerId, context.playerName, now, now))
		loadedPlayers.add(context.playerId)
	}

	override fun unloadPlayer(playerId: UUID) {
		repository.updateLastLogin(playerId, LocalDateTime.now())
	}

	override fun clearCache(playerId: UUID) {
		loadedPlayers.remove(playerId)
	}

	override fun getCacheSnapshot(playerId: UUID) = null

	override fun getCacheSnapshot() = null

	override fun saveAll() {
		val now = LocalDateTime.now()
		loadedPlayers.forEach { repository.updateLastLogin(it, now) }
	}
}