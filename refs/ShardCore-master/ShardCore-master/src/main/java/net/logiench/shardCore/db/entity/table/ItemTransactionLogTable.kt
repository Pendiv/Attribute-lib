package net.logiench.shardCore.db.entity.table

import net.logiench.shardCore.db.DatabaseTable
import net.logiench.shardCore.db.TargetDatabase
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.datetime

/**
 * プレイヤーがあるプレイヤーに対してアイテムをドロップして渡した場合は以下のようなログになります。
 * この場合、amountの意味が18消費したと18取得したで異なることに注意してください。
 *
 * 1. actionType: Drop, amount: 18
 * 2. actionType: Pickup, amount: 18
 */
object ItemTransactionLog : LongIdTable("item_transaction_log"), DatabaseTable {
	override val targetDatabase = TargetDatabase.SEASON

	val seasonPlayerId = reference("season_player_id", PlayerMapping)
	val actionType = varchar("action_type", 24)

	val itemId = varchar("item_id", 48)
	val itemData = text("item_data").nullable()
	val amount = integer("change_amount")
	val itemChecksum = integer("item_checksum")

	val timestamp = datetime("timestamp").index()

	// トランザクションの追加情報。たとえばトレードの場合そのトレードIDを付与など
	val context = varchar("context", 255).nullable()
}