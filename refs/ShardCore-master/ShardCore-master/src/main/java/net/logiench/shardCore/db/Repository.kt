package net.logiench.shardCore.db

interface Repository {
	val table: DatabaseTable

	fun getTarget() = table.targetDatabase
}