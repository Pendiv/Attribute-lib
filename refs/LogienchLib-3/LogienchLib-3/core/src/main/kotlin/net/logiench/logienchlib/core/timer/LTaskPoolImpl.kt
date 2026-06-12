package net.logiench.logienchlib.core.timer

import jakarta.inject.Singleton
import net.logiench.logienchlib.api.internal.LTaskPoolService
import net.logiench.logienchlib.api.timer.LTask
import net.logiench.logienchlib.api.timer.LTaskPool
import java.util.*

class LTaskPoolImpl(
	vararg tasks: LTask
) : LTaskPool {

	init {
		tasks.forEach { add(it) }
	}

	private val tasks: MutableList<LTask> = Collections.synchronizedList(ArrayList())

	override fun add(task: LTask): LTask {
		cleanup()
		tasks.add(task)
		return task
	}

	override fun remove(task: LTask): Boolean =
		tasks.remove(task)

	override fun getTasks(): List<LTask> {
		cleanup()
		return tasks.toList()
	}

	override fun cancel(task: LTask) {
		task.cancel()
		tasks.remove(task)
		cleanup()
	}

	override fun cancel() {
		// 正常にキャンセルできたもののみ削除する
		tasks.removeIf {
			it.cancel()
			true
		}
	}

	private fun cleanup() {
		tasks.removeIf { it.isFinished() }
	}
}

@Singleton
class LTaskPoolServiceImpl : LTaskPoolService {

	override fun create(vararg tasks: LTask): LTaskPool =
		LTaskPoolImpl(*tasks)
}