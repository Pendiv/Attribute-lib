package net.logiench.shardCore.db.entity.table

import net.logiench.shardCore.db.DatabaseTable
import net.logiench.shardCore.db.TargetDatabase
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.datetime

object PlayerBase : Table("player_base"), DatabaseTable {
	override val targetDatabase = TargetDatabase.MAIN

	val playerId = javaUUID("player_id")
	val playerName = varchar("player_name", 16).index()
	val firstLogin = datetime("first_login")
	val lastLogin = datetime("last_login")

	override val primaryKey = PrimaryKey(playerId)
}

object PlayerLoginLog : LongIdTable("player_login_log"), DatabaseTable {
	override val targetDatabase = TargetDatabase.MAIN

	// ログにFKつけると高頻度でINSERTされた場合負荷が高くなるので将来的には外すことを検討する。そこまで必要ないと思うけど
//	val playerId = javaUUID("player_id").index()
	val playerId = reference(
		"player_id", PlayerBase.playerId,
		onDelete = ReferenceOption.NO_ACTION, onUpdate = ReferenceOption.NO_ACTION
	).index()

	val loginAt = datetime("login_at").index()
	val logoutAt = datetime("logout_at").nullable()
	val playTime = integer("play_time")

	// IPv4,v6対応のため。
	val ipAddress = varchar("ip_address", 45)
}

