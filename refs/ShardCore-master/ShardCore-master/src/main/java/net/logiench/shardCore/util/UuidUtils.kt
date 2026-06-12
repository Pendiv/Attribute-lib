package net.logiench.shardCore.util

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@OptIn(ExperimentalUuidApi::class)
object UuidUtils {

	/**
	 * ランダムなUUID v7を生成します
	 */
	@JvmStatic
	fun v7Uuid() = Uuid.generateV7()

	/**
	 * ランダムなUUID v7を生成します
	 */
	@JvmStatic
	fun v7UUID() = v7Uuid().toJavaUuid()
}