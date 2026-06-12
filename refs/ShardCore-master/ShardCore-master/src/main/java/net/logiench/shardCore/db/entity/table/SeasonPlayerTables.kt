package net.logiench.shardCore.db.entity.table

import net.logiench.shardCore.db.DatabaseTable
import net.logiench.shardCore.db.TargetDatabase
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.datetime

object PlayerMapping : IntIdTable("player_mapping", "season_player_id"), DatabaseTable {
	override val targetDatabase = TargetDatabase.SEASON

	// FK制約つけたいけど別DBなのでJavaで論理制約を作成する
	val playerId = javaUUID("player_id").index()

	// 実際のデータを削除することなくプレイヤーデータを消す
	val deletedAt = datetime("deleted_at").nullable()
}

object JobBase : IntIdTable("job_base", "player_profile_id"), DatabaseTable {
	override val targetDatabase = TargetDatabase.SEASON

	val seasonPlayerId = reference("season_player_id", PlayerMapping)
	val jobId = varchar("job_id", 32)
	val firstLogin = datetime("first_login")
	val lastLogin = datetime("last_login")

	val deletedAt = datetime("deleted_at").nullable()
}

