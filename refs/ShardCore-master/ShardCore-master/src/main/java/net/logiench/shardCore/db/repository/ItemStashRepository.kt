package net.logiench.shardCore.db.repository

import com.google.inject.Singleton
import net.logiench.shardCore.db.Repository
import net.logiench.shardCore.db.entity.table.PlayerStashCustomTag
import net.logiench.shardCore.db.entity.table.StashCustomTag
import net.logiench.shardCore.db.entity.table.StashItem
import net.logiench.shardCore.db.entity.table.StashItemTag
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.*
import java.time.LocalDateTime
import java.util.*

@Singleton
class StashCustomTagRepository : Repository {
	override val table = StashCustomTag

	/**
	 * タグは存在しなければその時点で作成し、そのIDを返す
	 */
	fun insertAndSelect(tagName: String): Long {
		val res = table.insertIgnoreAndGetId { it[table.tagName] = tagName }
			?: table.select(table.id).where { table.tagName eq tagName }
				.single()[table.id]
		return res.value
	}

	fun selectAllByIds(tagIds: List<Long>): List<StashCustomTagEntity> {
		return table.select(table.id, table.tagName).where { table.id inList tagIds }
			.map { StashCustomTagEntity(it[table.id].value, it[table.tagName]) }
	}

	fun selectAllByIdsMap(tagIds: List<Long>): Map<Long, String> {
		return table.select(table.id, table.tagName).where { table.id inList tagIds }
			.associate { it[table.id].value to it[table.tagName] }
	}
}

@Singleton
class PlayerStashCustomTagRepository : Repository {
	override val table = PlayerStashCustomTag

	fun insert(entity: PlayerStashCustomTagEntity) {
		table.insert {
			it[playerId] = entity.playerId
			it[tagId] = entity.tagId
		}
	}

	fun selectAllByPlayerId(playerId: UUID): List<Long> =
		table.select(table.tagId).where { table.playerId eq playerId }
			.map { it[table.tagId].value }

	fun deleteById(playerId: UUID, tagId: Long) {
		table.deleteWhere { (table.playerId eq playerId) and (table.tagId eq tagId) }
	}
}

@Singleton
class StashItemRepository : Repository {
	override val table = StashItem

	// StashItemは大量の更新が予測されるのでバッチ処理を構築
	fun batchUpsert(entities: List<StashItemEntity>) {
		table.batchUpsert(
			data = entities,
			// 変化させないカラム
			onUpdateExclude = listOf(
				StashItem.stashItemId,
				StashItem.seasonPlayerId,
				StashItem.itemId,
				StashItem.itemData,
				// amount は変化する
				StashItem.itemChecksum,
				StashItem.createdAt
				// updatedAt は変化する
			)
		) { entity ->
			this[StashItem.stashItemId] = entity.stashItemId
			this[StashItem.seasonPlayerId] = entity.seasonPlayerId
			this[StashItem.itemId] = entity.itemId
			this[StashItem.itemData] = entity.itemData
			this[StashItem.amount] = entity.amount
			this[StashItem.itemChecksum] = entity.itemChecksum
			this[StashItem.createdAt] = entity.createdAt
			this[StashItem.updatedAt] = entity.updatedAt
		}
	}

	fun selectByPlayerId(seasonPlayerId: Int): List<StashItemEntity> =
		table.select(
			table.stashItemId,
			table.itemId, table.itemData, table.amount,
			table.itemChecksum, table.createdAt, table.updatedAt
		).where { table.seasonPlayerId eq seasonPlayerId }
			.map {
				StashItemEntity(
					it[table.stashItemId],
					seasonPlayerId,
					it[table.itemId],
					it[table.itemData],
					it[table.amount],
					it[table.itemChecksum],
					it[table.createdAt],
					it[table.updatedAt]
				)
			}

	fun deleteAll(stashItemIds: Iterable<UUID>) {
		table.deleteWhere { table.stashItemId inList stashItemIds }
	}
}

@Singleton
class StashItemTagRepository : Repository {
	override val table = StashItemTag

	fun batchInsertIgnore(entities: List<StashItemTagEntity>) {
		table.batchInsert(entities, true) { entity ->
			this[StashItemTag.stashItemId] = entity.stashItemId
			this[StashItemTag.tagId] = entity.tagId
		}
	}

	fun selectByStashItemId(stashItemId: UUID): List<StashItemTagEntity> =
		table.select(table.tagId).where { table.stashItemId eq stashItemId }
			.map { StashItemTagEntity(stashItemId, it[table.tagId]) }

	/**
	 * 入力されたIDをもとに、IDをkey、付与されているタグIDのリストをvalueとしたMapを返します。
	 */
	fun selectByStashItemIds(stashItemIds: List<UUID>): Map<UUID, List<Long>> {
		if (stashItemIds.isEmpty()) return emptyMap()

		return table.select(table.stashItemId, table.tagId)
			.where { table.stashItemId inList stashItemIds }
			// toでPairを生成
			.map { it[table.stashItemId] to it[table.tagId] }
			// Pairのリストを、第一要素をキーにしてグループ化する
			.groupBy({ it.first }, { it.second })
	}
}

data class StashCustomTagEntity(val tagId: Long, val tagName: String)

data class PlayerStashCustomTagEntity(val playerId: UUID, val tagId: Long)

/**
 * @param itemChecksum アイテムの個数にかかわらず同じ値である必要があります。
 */
data class StashItemEntity(
	val stashItemId: UUID, val seasonPlayerId: Int,
	val itemId: String, val itemData: String?, val amount: Int, val itemChecksum: Int,
	val createdAt: LocalDateTime, val updatedAt: LocalDateTime
)

data class StashItemTagEntity(val stashItemId: UUID, val tagId: Long)