package net.logiench.shardCore.db

import java.util.*

interface DatabaseService : Comparable<DatabaseService> {

	/**
	 * ロード時の優先度を指定します。
	 * 優先度が高いほど先にロードされ、優先度が低いほど先に保存されます。
	 * load: high -> low, save: low -> high
	 */
	val loadingPriority: Int

	val targetDatabase: TargetDatabase

	/** transactionの内部から呼び出されます */
	fun saveAll()

	override fun compareTo(other: DatabaseService): Int {
		return loadingPriority.compareTo(other.loadingPriority)
	}

	/**
	 * 保存失敗時に、メモリ上のデータをダンプ（出力）するために呼ばれます。
	 * Gson等でシリアライズ可能なオブジェクト（DataClassやMap）を返してください。
	 */
	fun getCacheSnapshot(): Any?
}

interface PlayerDatabaseService : DatabaseService {

	override val loadingPriority: Int

	override val targetDatabase: TargetDatabase

	/** transactionの内部から呼び出されます */
	fun loadPlayer(context: LoginContext)

	/** transactionの内部から呼び出されます */
	fun unloadPlayer(playerId: UUID)

	/**
	 * サーバーからプレイヤーが退出したか、データのロードに失敗した際のリセットとして使用されます
	 */
	fun clearCache(playerId: UUID)

	/**
	 * 保存失敗時に、メモリ上のデータをダンプ（出力）するために呼ばれます。
	 * Gson等でシリアライズ可能なオブジェクト（DataClassやMap）を返してください。
	 */
	fun getCacheSnapshot(playerId: UUID): Any?

	/**
	 * 全てのプレイヤーのデータのダンプを要求します
	 * MapのValueにはGson等でシリアライズ可能なオブジェクト（DataClassやMap）を返してください。
	 * @see DatabaseService.getCacheSnapshot
	 */
	override fun getCacheSnapshot(): Map<UUID, Any>?

	override fun saveAll()

	data class LoginContext(val playerId: UUID, val playerName: String, val ipAddress: String)
}

interface ProfileDatabaseService : DatabaseService {

	override val loadingPriority: Int

	override val targetDatabase: TargetDatabase

	/** transactionの内部から呼び出されます */
	fun loadProfile(playerId: UUID, profileId: Int)

	/** transactionの内部から呼び出されます */
	fun unloadProfile(playerId: UUID, profileId: Int)

	/** データのロードに失敗した際のリセットとしてのみ使用されます */
	fun clearCache(playerId: UUID, profileId: Int)

	/**
	 * 保存失敗時に、メモリ上のデータをダンプ（出力）するために呼ばれます。
	 * Gson等でシリアライズ可能なオブジェクト（DataClassやMap）を返してください。
	 */
	fun getCacheSnapshot(playerId: UUID, profileId: Int): Any?

	/**
	 * 全てのプレイヤープロファイルのデータのダンプを要求します
	 * @see DatabaseService.getCacheSnapshot
	 */
	override fun getCacheSnapshot(): Map<Int, Any>?

	override fun saveAll()
}
