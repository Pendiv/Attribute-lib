package net.logiench.shardCore.db.entity.table

import net.logiench.shardCore.db.DatabaseTable
import net.logiench.shardCore.db.TargetDatabase
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.datetime


object StashCustomTag : LongIdTable("stash_custom_tag", "tag_id"), DatabaseTable {
	override val targetDatabase = TargetDatabase.MAIN

	val tagName = varchar("tag_name", 24).uniqueIndex()
}

object PlayerStashCustomTag : Table("player_stash_custom_tag"), DatabaseTable {
	override val targetDatabase = TargetDatabase.MAIN

	val playerId = reference("player_id", PlayerBase.playerId).index()
	val tagId = reference("tag_id", StashCustomTag)
}


object StashItem : Table("stash_item"), DatabaseTable {
	override val targetDatabase = TargetDatabase.SEASON

	val stashItemId = javaUUID("stash_item_id")
	val seasonPlayerId = reference("season_player_id", PlayerMapping).index()
	val itemId = varchar("item_id", 48)
	val itemData = text("item_data").nullable()
	val amount = integer("amount")
	val itemChecksum = integer("item_checksum")
	val createdAt = datetime("created_at")
	val updatedAt = datetime("updated_at")

	override val primaryKey = PrimaryKey(stashItemId)
}

object StashItemTag : Table("stash_item_tag"), DatabaseTable {
	override val targetDatabase = TargetDatabase.SEASON

	val stashItemId = reference("stash_item_id", StashItem.stashItemId, onDelete = ReferenceOption.CASCADE).index()
	val tagId = long("tag_id")

	override val primaryKey = PrimaryKey(stashItemId, tagId)
}
