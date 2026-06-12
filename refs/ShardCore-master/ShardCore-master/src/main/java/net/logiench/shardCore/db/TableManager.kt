package net.logiench.shardCore.db

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.google.inject.Singleton
import net.logiench.shardCore.util.ClassUtils
import org.jetbrains.exposed.v1.core.Table

@Singleton
class TableManager {
	private val databaseTables: ImmutableMap<TargetDatabase, ImmutableList<Table>>

	init {
		val instances = ClassUtils.findSubClasses(DatabaseTable::class.java, "net.logiench.shardCore.db.entity.table")
			.mapNotNull { clazz ->
				// 1. まず objectInstance を DatabaseTable として取得
				clazz.kotlin.objectInstance
			}
			.filter { it is Table } // 2. かつ、Table を実装しているものに絞る
			.groupBy(
				{ it.targetDatabase },
				{ it as Table })

		this.databaseTables = ImmutableMap.copyOf(
			instances.mapValues { (_, values) ->
				ImmutableList.copyOf(values)
			}
		)
	}

	fun getTargetDatabases(): ImmutableSet<TargetDatabase> = databaseTables.keys

	fun getTables(target: TargetDatabase): ImmutableList<Table> = databaseTables[target] ?: ImmutableList.of()
}

interface DatabaseTable {
	val targetDatabase: TargetDatabase
}