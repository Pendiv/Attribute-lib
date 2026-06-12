package net.logiench.shardCore.db.repository

import com.google.inject.Singleton
import net.logiench.shardCore.db.Repository
import net.logiench.shardCore.db.entity.table.ItemTransactionLog
import org.jetbrains.exposed.v1.jdbc.insert
import java.time.LocalDateTime

@Singleton
class ItemTransactionLogRepository : Repository {
	override val table = ItemTransactionLog

	fun insert(entity: ItemTransactionLogEntity) {
		table.insert {
			it[seasonPlayerId] = entity.seasonPlayerId
			it[actionType] = entity.actionType
			it[itemId] = entity.itemId
			it[itemData] = entity.itemData
			it[amount] = entity.amount
			it[itemChecksum] = entity.itemChecksum
			it[timestamp] = entity.timestamp
			it[context] = entity.context
		}
	}
}

data class ItemTransactionLogEntity(
	val id: Long, val seasonPlayerId: Int, val actionType: String,
	val itemId: String, val itemData: String?, val amount: Int, val itemChecksum: Int,
	val timestamp: LocalDateTime, val context: String?
)