package net.logiench.shardCore.db.service

import com.google.inject.Inject
import com.google.inject.Singleton
import net.logiench.shardCore.ShardCore
import net.logiench.shardCore.core.item.base.def.ShardItem
import net.logiench.shardCore.db.PlayerDatabaseService
import net.logiench.shardCore.db.TargetDatabase
import net.logiench.shardCore.db.repository.StashItemEntity
import net.logiench.shardCore.db.repository.StashItemRepository
import net.logiench.shardCore.db.repository.StashItemTagEntity
import net.logiench.shardCore.db.repository.StashItemTagRepository
import net.logiench.shardCore.register.ItemRegistry
import net.logiench.shardCore.util.UuidUtils
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Singleton
class PlayerStashItemService @Inject private constructor(
	private val mapping: PlayerMappingService,
	private val stashItemRepository: StashItemRepository,
	private val stashTagRepository: StashItemTagRepository,
	private val itemRegistry: ItemRegistry
) : PlayerDatabaseService {

	private val stashItemCache: MutableMap<UUID, PlayerStashContent> = ConcurrentHashMap()

	override val loadingPriority = 0
	override val targetDatabase = TargetDatabase.SEASON

	override fun loadPlayer(context: PlayerDatabaseService.LoginContext) {
		val playerId = context.playerId
		val seasonPlayerId = mapping.getSeasonPlayerId(playerId)
		val stashItemEntities = stashItemRepository.selectByPlayerId(seasonPlayerId)
		val stashItemTags = stashTagRepository.selectByStashItemIds(stashItemEntities.map { it.stashItemId })

		val playerStashContent = PlayerStashContent(
			stashItemEntities.mapNotNull { itemEntity ->
				val stashItemId = itemEntity.stashItemId
				val itemData = itemRegistry.get(itemEntity.itemId) ?: run {
					ShardCore.getPLogger().warning(
						"[PlayerStashItemService] 存在しないItemId: '${itemEntity.itemId}' がデータベースからロードされました。 StashItemId: '$stashItemId'"
					)
					return@mapNotNull null
				}
				StashItemData.fromDatabase(
					stashItemId,
					itemData,
					itemEntity.amount,
					itemEntity.itemData,
					itemEntity.itemChecksum,
					itemEntity.createdAt,
					itemEntity.updatedAt,
					stashItemTags[stashItemId]?.toSet() ?: emptySet()
				)
			}.toMutableList()
		)

		stashItemCache[playerId] = playerStashContent
	}

	override fun unloadPlayer(playerId: UUID) {
		val content = stashItemCache.remove(playerId) ?: return
		updateDatabaseEntities(playerId, content)
	}

	override fun clearCache(playerId: UUID) {
		stashItemCache.remove(playerId)
	}

	override fun getCacheSnapshot(playerId: UUID) = getStash(playerId)

	override fun getCacheSnapshot() = stashItemCache.toMap()

	override fun saveAll() {
		stashItemCache.forEach { (playerId, content) ->
			updateDatabaseEntities(playerId, content)
		}
	}

	fun getStash(playerId: UUID): PlayerStashContent? = stashItemCache[playerId]


	// -- DBへの保存 --
	private fun updateDatabaseEntities(playerId: UUID, content: PlayerStashContent) {
		getDirtyDatabaseEntity(playerId, content).let { (upsertItems, upsertItemTags, deleteIds) ->
			// delete時にタグはCASCADEなので削除しなくていい
			stashItemRepository.deleteAll(deleteIds)
			stashItemRepository.batchUpsert(upsertItems)
			stashTagRepository.batchInsertIgnore(upsertItemTags)
		}
	}

	/**@return <追加または更新するアイテム, 追加または既存のアイテムタグ, 削除するアイテムのID>*/
	private fun getDirtyDatabaseEntity(
		playerId: UUID,
		content: PlayerStashContent
	): Triple<List<StashItemEntity>, List<StashItemTagEntity>, Set<UUID>> {
		val seasonPlayerId = mapping.getSeasonPlayerId(playerId)
		val snapshot = content.popChangesForSave()
		// 更新するアイテム
		val upsertItems = snapshot.upsertItems.map {
			StashItemEntity(
				it.stashItemId,
				seasonPlayerId,
				it.itemData.id,
				it.itemDataJson,
				it.amount,
				it.itemChecksum,
				it.createdAt,
				it.updatedAt
			)
		}
		// 更新するタグ
		val upsertItemTags = snapshot.upsertTagItems.flatMap { item ->
			val stashItemId = item.stashItemId
			item.tags.map { tagId -> StashItemTagEntity(stashItemId, tagId) }
		}
		// 削除するアイテムID (そのIDに紐づいたタグも)
		val deleteIds = snapshot.deleteIds

		return Triple(upsertItems, upsertItemTags, deleteIds)
	}
}

/**
 * プレイヤーのスタッシュです。
 * プレイヤーがオンラインの間、このオブジェクトに変更を加えると自動でDBに適応されます。
 */
class PlayerStashContent(
	inputItems: MutableCollection<StashItemData>
) {
	private val stashItems: MutableMap<UUID, StashItemData> = ConcurrentHashMap()
	private val tagIndexes: MutableMap<Long, MutableSet<UUID>> = ConcurrentHashMap()

	private val dirtyItems = ConcurrentHashMap.newKeySet<UUID>()
	private val dirtyTags = ConcurrentHashMap.newKeySet<UUID>()
	private val deletedItems = ConcurrentHashMap.newKeySet<UUID>()

	init {
		inputItems.forEach { addItem(it) }
	}

	fun addItem(item: StashItemData) {
		stashItems[item.stashItemId] = item
		item.tags.forEach { tagId ->
			tagIndexes.getOrPut(tagId) { ConcurrentHashMap.newKeySet() }.add(item.stashItemId)
		}

		dirtyItems.add(item.stashItemId)
		dirtyTags.add(item.stashItemId)
		deletedItems.remove(item.stashItemId)
	}

	fun removeItem(stashItemId: UUID): StashItemData? {
		val deletedItem = stashItems.remove(stashItemId) ?: return null
		deletedItem.tags.forEach { tagId ->
			tagIndexes[tagId]?.remove(stashItemId)
		}

		dirtyItems.remove(stashItemId)
		dirtyTags.remove(stashItemId)
		deletedItems.add(stashItemId)
		return deletedItem
	}

	/**
	 * アイテムの数を変更します。アイテムの数が0以下だった場合、[removeItem]が呼び出され、アイテムは削除されます。
	 * @return 変更されたアイテムのデータ。アイテムが削除された場合、または`stashItemId`が存在しない場合はnull
	 */
	fun updateAmount(stashItemId: UUID, newAmount: Int): StashItemData? =
		updateAmount(stashItemId) { newAmount }

	/**
	 * アイテムの数を変更します。アイテムの数が0以下だった場合、[removeItem]が呼び出され、アイテムは削除されます。
	 * @return 変更されたアイテムのデータ。アイテムが削除された場合、または`stashItemId`が存在しない場合はnull
	 */
	fun updateAmount(stashItemId: UUID, newAmount: StashItemData.(Int) -> Int): StashItemData? {
		val oldItem = stashItems[stashItemId] ?: return null
		// 1. 新しいインスタンスを生成 (withAmount内で copy & updatedAt更新)
		val newAmountValue = newAmount(oldItem, oldItem.amount)
		// 0以下の数が設定されたら削除する
		if (newAmountValue <= 0) {
			removeItem(stashItemId)
			return null
		}
		val newItem = oldItem.withAmount(newAmountValue)
		// 2. Mapを差し替え（インデックスに影響しない更新ならこれだけでOK）
		stashItems[stashItemId] = newItem

		dirtyItems.add(stashItemId)
		return newItem
	}

	/**
	 * アイテムのタグを変更します。
	 * @return 変更されたアイテムのデータ。`stashItemId`が存在しない場合はnull
	 */
	fun updateTags(stashItemId: UUID, newTags: Set<Long>): StashItemData? =
		updateTags(stashItemId) { newTags }

	/**
	 * アイテムのタグを変更します。
	 * @return 変更されたアイテムのデータ。`stashItemId`が存在しない場合はnull
	 */
	fun updateTags(stashItemId: UUID, newTags: StashItemData.(Set<Long>) -> Set<Long>): StashItemData? {
		val oldItem = stashItems[stashItemId] ?: return null
		val newItem = oldItem.withTags(newTags(oldItem, oldItem.tags))

		// 1. 古いタグのインデックスからこのアイテム(UUID)を消す
		oldItem.tags.forEach { tagId -> tagIndexes[tagId]?.remove(stashItemId) }

		// 2. 新しいタグのインデックスにこのアイテム(UUID)を追加する
		newItem.tags.forEach { tagId ->
			tagIndexes.getOrPut(tagId) { ConcurrentHashMap.newKeySet() }.add(stashItemId)
		}

		// 3. アイテム実体の差し替え
		stashItems[stashItemId] = newItem

		// 4. タグだけを更新対象としてマーク
		dirtyTags.add(stashItemId)

		return newItem
	}

	/**
	 * 指定されたタグを全てのアイテムから削除します
	 */
	fun removeTagIds(tagIds: Set<Long>) {
		stashItems.values.forEach { item ->
			// どれか一つでも指定されたtagIdを持つならupdateを実行する
			if (item.tags.any { tagIds.contains(it) }) {
				updateTags(item.stashItemId) { it - tagIds }
			}
		}
		// ここに到達すれば指定されたtagIdsを持つ物はないからそのままindex削除
		tagIds.forEach { tagId ->
			tagIndexes.remove(tagId)
		}
	}

	fun getItem(stashItemId: UUID): StashItemData? = stashItems[stashItemId]

	/**
	 * タグに紐づいたアイテムを取得します。
	 */
	fun getTaggedItems(tagId: Long): List<StashItemData> =
		tagIndexes[tagId]?.mapNotNull { getItem(it) } ?: return emptyList()

	/**
	 * タグに紐づいたアイテムを取得します。
	 * [StashItemData]の値を使ってソートできます
	 */
	fun getTaggedItems(tagId: Long, sortOption: SortOption? = null): List<StashItemData> {
		val itemIds = tagIndexes[tagId] ?: return emptyList()
		return sortItems(itemIds.mapNotNull { getItem(it) }, sortOption)
	}

	/**
	 * プレイヤーのアイテムをすべて取得します。
	 */
	fun getItems(): List<StashItemData> = stashItems.values.toList()

	/**
	 * プレイヤーのアイテムをすべて取得します。
	 * [StashItemData]の値を使ってソートできます
	 */
	fun getItems(sortOption: SortOption? = null): List<StashItemData> =
		sortItems(stashItems.values, sortOption)

	fun isDirty(): Boolean =
		!(dirtyItems.isEmpty() && dirtyTags.isEmpty() && deletedItems.isEmpty())

	// --- 保存用スナップショットの抽出 ---
	fun popChangesForSave(): SaveSnapshot {
		val coreToUpsert = dirtyItems.toSet()
		val tagsToUpsert = dirtyTags.toSet()
		val toDelete = deletedItems.toSet()

		dirtyItems.removeAll(coreToUpsert)
		dirtyTags.removeAll(tagsToUpsert)
		deletedItems.removeAll(toDelete)

		return SaveSnapshot(
			coreToUpsert.mapNotNull { stashItems[it] },
			tagsToUpsert.mapNotNull { stashItems[it] },
			toDelete
		)
	}

	private fun sortItems(items: Collection<StashItemData>, sort: SortOption?): List<StashItemData> =
		if (sort == null) {
			items.toList()
		} else {
			if (sort.desc) {
				items.sortedByDescending(sort.comparatorValue)
			} else {
				items.sortedBy(sort.comparatorValue)
			}
		}

	data class SaveSnapshot(
		val upsertItems: List<StashItemData>, // stash_item 用
		val upsertTagItems: List<StashItemData>,  // stash_item_tag 用
		val deleteIds: Set<UUID>                 // DELETE 用
	)

	data class SortOption(val desc: Boolean, val comparatorValue: (StashItemData) -> Comparable<Any>)
}

@ConsistentCopyVisibility
data class StashItemData private constructor(
	val stashItemId: UUID,  // UUID v7 で時間と乱数を使って生成する
	val itemData: ShardItem, // itemIdとベースとなるアイテムのデータを持つ
	val amount: Int,
	val itemDataJson: String?, // dbに保存されているGson変換されたデータ
	val itemChecksum: Int,        // dbから復元したアイテムが元と完全に同じか(バージョンによって生成されるアイテムが変化していないか)
	val createdAt: LocalDateTime, // 外部からは変更不可
	val updatedAt: LocalDateTime, // Witherで自動更新
	val tags: Set<Long> = emptySet()
) {
	// --- Java用 Wither メソッド ---
	// 呼び出すたびに updatedAt を現在時刻で上書きした新しいコピーを返す

	fun withAmount(newAmount: Int): StashItemData =
		this.copy(amount = newAmount, updatedAt = LocalDateTime.now())

	// タグの更新はアイテム自体の更新ではないのでupdatedAtを更新しない
	fun withTags(newTags: Set<Long>): StashItemData =
		this.copy(tags = newTags)

	companion object {
		/*
		 * 新規作成用ファクトリ
		 * ここで一度だけ createdAt を設定する
		 *
		 * Javaからだとデフォルト値を指定しても無駄だからオーバーロードする
		 */
		@JvmStatic
		fun createNew(itemData: ShardItem, amount: Int, itemChecksum: Int): StashItemData =
			createNew(itemData, amount, null, itemChecksum)

		@JvmStatic
		fun createNew(itemData: ShardItem, amount: Int, json: String?, itemCheckSum: Int): StashItemData {
			val now = LocalDateTime.now()
			return StashItemData(UuidUtils.v7UUID(), itemData, amount, json, itemCheckSum, now, now)
		}

		/**
		 * DBロード用ファクトリ
		 * DBに保存されている既存の時間を復元する
		 */
		fun fromDatabase(
			stashItemId: UUID, itemData: ShardItem, amount: Int, json: String?, checksum: Int,
			created: LocalDateTime, updated: LocalDateTime, tags: Set<Long>
		): StashItemData {
			return StashItemData(stashItemId, itemData, amount, json, checksum, created, updated, tags)
		}
	}
}