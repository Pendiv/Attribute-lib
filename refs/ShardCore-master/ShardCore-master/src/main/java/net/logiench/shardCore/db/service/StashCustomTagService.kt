package net.logiench.shardCore.db.service

import com.google.inject.Inject
import com.google.inject.Singleton
import net.logiench.shardCore.db.DatabaseManager
import net.logiench.shardCore.db.PlayerDatabaseService
import net.logiench.shardCore.db.TargetDatabase
import net.logiench.shardCore.db.repository.PlayerStashCustomTagEntity
import net.logiench.shardCore.db.repository.PlayerStashCustomTagRepository
import net.logiench.shardCore.db.repository.StashCustomTagRepository
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

@Singleton
class StashCustomTagService @Inject private constructor(
	private val manager: DatabaseManager,
	private val tagRepository: StashCustomTagRepository,
	private val playerRepository: PlayerStashCustomTagRepository
) : PlayerDatabaseService {
	/*
	CustomTagMappingは更新時に適応する
	PlayerSelectedTagsも同じく更新時
	 */
	private val customTagMapping: MutableMap<Long, String> = ConcurrentHashMap()
	private val playerSelectedTags: MutableMap<UUID, MutableList<Long>> = ConcurrentHashMap()

	override val loadingPriority = Integer.MAX_VALUE
	override val targetDatabase = TargetDatabase.MAIN

	override fun loadPlayer(context: PlayerDatabaseService.LoginContext) {
		val playerId = context.playerId
		val selectedTagIds = playerRepository.selectAllByPlayerId(playerId)
		val selectedTagData = tagRepository.selectAllByIdsMap(
			selectedTagIds.filter { customTagMapping.containsKey(it) })

		customTagMapping.putAll(selectedTagData)
		playerSelectedTags[playerId] = selectedTagIds.toMutableList()
	}

	override fun unloadPlayer(playerId: UUID) {
		// データは更新時に逐次適応なので保存なし
	}

	/**
	 * プレイヤーに新たなタグを関連付けます。
	 * この更新は即材にDBに反映され、キャッシュに適応されます。
	 */
	fun addSelectedTag(playerId: UUID, tagName: String): CompletableFuture<Boolean> {
		val cachedTagId = customTagMapping.entries.find { it.value == tagName }?.key
		if (playerSelectedTags[playerId]?.contains(cachedTagId)
				?: false
		) return CompletableFuture.completedFuture(false)

		return manager.supplyAsync(targetDatabase) {
			// タグがなければ新しくinsertし、プレイヤーと紐づける
			val tagId = cachedTagId ?: run {
				val id = tagRepository.insertAndSelect(tagName)
				customTagMapping[id] = tagName
				id
			}
			playerRepository.insert(PlayerStashCustomTagEntity(playerId, tagId))
			customTagMapping[tagId] = tagName
			true
		}.exceptionally {
			it.printStackTrace()
			false
		}
	}

	/**
	 * プレイヤーとタグの関連付けを削除します。
	 * この操作でタグは削除されませんが、タグのキャッシュは必要なければ削除されます。
	 */
	fun removeSelectedTag(playerId: UUID, tagId: Long): CompletableFuture<Boolean> {
		val selectedTags = playerSelectedTags[playerId] ?: return CompletableFuture.completedFuture(false)
		if (!selectedTags.remove(tagId)) return CompletableFuture.completedFuture(false)

		return manager.supplyAsync(targetDatabase) {
			playerRepository.deleteById(playerId, tagId)
			if (playerSelectedTags.values.flatten().contains(tagId)) {
				customTagMapping.remove(tagId)
			}
			true
		}
	}

	fun getPlayerTags(playerId: UUID): Map<Long, String> {
		val selectedIds = playerSelectedTags[playerId] ?: return emptyMap()
		// selectedにあってmappingにないことはないので、nullではない!!
		return selectedIds.associateWith { customTagMapping[it]!! }
	}

	fun getTagByCache(tagId: Long): String? {
		return customTagMapping[tagId]
	}

	fun getTagsByCache(tagIds: Collection<Long>): Map<Long, String> {
		return tagIds.mapNotNull {
			val tagName = customTagMapping[it] ?: return@mapNotNull null
			it to tagName
		}.toMap()
	}

	fun getTagsByCacheOrNull(tagIds: Collection<Long>): Map<Long, String?> {
		return tagIds.associateWith { customTagMapping[it] }
	}

	override fun clearCache(playerId: UUID) {
		val selectedTags = playerSelectedTags.remove(playerId) ?: return
		val flattenTags = playerSelectedTags.values.flatten()
		selectedTags.forEach {
			// 他のプレイヤーがそのタグを使用していなければ削除
			if (flattenTags.contains(it)) return@forEach
			customTagMapping.remove(it)
		}
	}

	// 追加や削除をDBに直接反映させるので必要なし
	override fun getCacheSnapshot(playerId: UUID) = null

	override fun getCacheSnapshot() = null

	override fun saveAll() {
	}
}