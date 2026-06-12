package net.logiench.shardCore.db.repository

import com.google.inject.Singleton
import net.logiench.shardCore.db.Repository
import net.logiench.shardCore.db.entity.table.PlayerBase
import net.logiench.shardCore.db.entity.table.PlayerLoginLog
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert
import java.time.LocalDateTime
import java.util.*

@Singleton
class PlayerBaseRepository : Repository {
	override val table = PlayerBase

	fun upsert(entity: PlayerBaseEntity) =
		// onUpdateExcludeでupsertで更新しない項目を指定する
		table.upsert(onUpdateExclude = listOf(table.firstLogin)) {
			it[playerId] = entity.playerId
			it[playerName] = entity.playerName
			it[lastLogin] = entity.lastLogin
			it[firstLogin] = entity.firstLogin
		}

	fun updateLastLogin(playerId: UUID, lastLogin: LocalDateTime) {
		table.update({ table.playerId eq playerId }) {
			it[table.lastLogin] = lastLogin
		}
	}

	fun selectByUUID(playerId: UUID): Optional<PlayerBaseEntity?> {
		return Optional.ofNullable(
			table.select(table.playerName, table.firstLogin, table.lastLogin)
				.where(table.playerId eq playerId).singleOrNull()
		).map {
			PlayerBaseEntity(
				playerId, it[table.playerName],
				it[table.firstLogin], it[table.lastLogin]
			)
		}
	}
}

@Singleton
class PlayerLoginLogRepository : Repository {
	override val table = PlayerLoginLog

	fun insert(playerId: UUID, loginAt: LocalDateTime, ipAddress: String) =
		table.insertAndGetId {
			it[table.playerId] = playerId
			it[table.loginAt] = loginAt
			it[table.playTime] = 0
			it[table.ipAddress] = ipAddress
		}.value

	fun updateTime(id: Long, playTime: Int, logoutAt: LocalDateTime? = null) {
		table.update({ table.id eq id }) {
			it[table.playTime] = playTime
			if (logoutAt != null) it[table.logoutAt] = logoutAt
		}
	}
}

data class PlayerBaseEntity(
	val playerId: UUID,
	val playerName: String,
	val firstLogin: LocalDateTime,
	val lastLogin: LocalDateTime
)

data class PlayerLoginLogEntity(
	val id: Long, val playerId: UUID, val loginAt: LocalDateTime,
	val logoutAt: LocalDateTime?, val playTime: Int, val ipAddress: String
)