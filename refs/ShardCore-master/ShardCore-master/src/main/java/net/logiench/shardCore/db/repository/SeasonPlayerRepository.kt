package net.logiench.shardCore.db.repository

import com.google.inject.Singleton
import net.logiench.shardCore.ShardCore
import net.logiench.shardCore.db.Repository
import net.logiench.shardCore.db.entity.table.JobBase
import net.logiench.shardCore.db.entity.table.PlayerMapping
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.update
import java.time.LocalDateTime
import java.util.*

@Singleton
class PlayerMappingRepository : Repository {
	override val table = PlayerMapping

	fun insertOrGetId(playerId: UUID): Int {
		val entityId = table.select(table.id)
			.where { table.playerId eq playerId }
			.singleOrNull()?.get(table.id)
			?: table.insertAndGetId { it[table.playerId] = playerId }
		return entityId.value
	}

	fun selectByPlayerId(playerId: UUID): Optional<PlayerMappingEntity> =
		Optional.ofNullable(
			table.select(table.id)
				// 削除されていない = 削除された日時がないデータを取得する
				.where { table.playerId eq playerId and table.deletedAt.isNull() }
				.singleOrNull()?.let {
					PlayerMappingEntity(it[table.id].value, playerId, null)
				}
		)

	fun setDeletedAt(seasonPlayerId: Int, deleteAt: LocalDateTime) {
		table.update({ (table.id eq seasonPlayerId) and table.deletedAt.isNull() }) {
			it[table.deletedAt] = deleteAt
		}
	}
}

@Singleton
class JobBaseRepository : Repository {
	override val table = JobBase

	fun insertAndGetId(seasonPlayerId: Int, job: Job, firstLogin: LocalDateTime, lastLogin: LocalDateTime): Int =
		table.insertAndGetId {
			it[table.seasonPlayerId] = seasonPlayerId
			it[table.jobId] = job.jobId
			it[table.firstLogin] = firstLogin
			it[table.lastLogin] = lastLogin
		}.value

	fun updateLastLogin(profileId: Int, lastLogin: LocalDateTime) {
		table.update({ (table.id eq profileId) and table.deletedAt.isNull() }) {
			it[table.lastLogin] = lastLogin
		}
	}

	fun selectByPlayerAndJobId(seasonPlayerId: Int, job: Job): Optional<JobBaseEntity> =
		Optional.ofNullable(
			table.select(table.id, table.jobId, table.firstLogin, table.lastLogin)
				.where { (table.seasonPlayerId eq seasonPlayerId) and (table.jobId eq job.jobId) and table.deletedAt.isNull() }
				.singleOrNull()?.let {
					JobBaseEntity(
						it[table.id].value, seasonPlayerId, job, it[table.firstLogin], it[table.lastLogin], null
					)
				}
		)

	fun selectAllByPlayerId(seasonPlayerId: Int): List<JobBaseEntity> =
		table.select(table.id, table.jobId, table.firstLogin, table.lastLogin)
			.where { (table.seasonPlayerId eq seasonPlayerId) and table.deletedAt.isNull() }
			.mapNotNull {
				val jobId = it[table.jobId]
				val job = Job.entries.find { j -> j.jobId == jobId }
				if (job == null) {
					ShardCore.getPLogger().warning("JobBaseRepository: 指定されたjobIdの職業が見つかりません: $jobId")
					null
				} else {
					JobBaseEntity(
						it[table.id].value, seasonPlayerId, job,
						it[table.firstLogin], it[table.lastLogin], null
					)
				}
			}

	fun setDeletedAt(seasonPlayerId: Int, job: Job, deleteAt: LocalDateTime) {
		table.update({ (table.id eq seasonPlayerId) and (table.jobId eq job.jobId) and table.deletedAt.isNull() }) {
			it[table.deletedAt] = deleteAt
		}
	}
}

data class PlayerMappingEntity(val seasonPlayerId: Int, val playerId: UUID, val deleteAt: LocalDateTime?)

data class JobBaseEntity(
	val profileId: Int, val seasonPlayerId: Int, val job: Job,
	val firstLogin: LocalDateTime, val lastLogin: LocalDateTime, val deleteAt: LocalDateTime?
) {
	fun updateLastLogin(lastLogin: LocalDateTime) =
		JobBaseEntity(profileId, seasonPlayerId, job, firstLogin, lastLogin, deleteAt)
}

enum class Job(
	inputJobId: String
) {
	TEST1("TEST1"),
	TEST2("TEST2"),
	TEST3("TEST3"),
	;

	val jobId: String = inputJobId.lowercase()
}

