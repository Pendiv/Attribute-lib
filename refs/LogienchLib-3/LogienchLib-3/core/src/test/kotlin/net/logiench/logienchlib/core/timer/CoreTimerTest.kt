package net.logiench.logienchlib.core.timer

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.logiench.logienchlib.api.timer.LTask
import net.logiench.logienchlib.api.timer.TimerOption
import net.logiench.logienchlib.core.CoreTestBase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CoreTimerTest : CoreTestBase() {

	@Test
	fun `LTaskPoolImplにタスクを追加および削除できること`() {
		val pool = LTaskPoolImpl()
		val task1 = mockk<LTask>(relaxed = true)
		val task2 = mockk<LTask>(relaxed = true)

		pool.add(task1)
		pool.add(task2)
		assertEquals(2, pool.getTasks().size, "2つのタスクがプールにあること")

		pool.remove(task1)
		assertEquals(1, pool.getTasks().size, "task1が削除されて残り1つであること")
		assertSame(task2, pool.getTasks()[0], "残っているのはtask2であること")
	}

	@Test
	fun LTaskPoolImplは完了またはキャンセルされたタスクを自動クリーンアップすること() {
		val pool = LTaskPoolImpl()
		val activeTask = mockk<LTask>()
		val finishedTask = mockk<LTask>()

		every { activeTask.isFinished() } returns false
		every { finishedTask.isFinished() } returns true

		pool.add(activeTask)
		pool.add(finishedTask)

		// getTasks() を呼ぶと内部で cleanup() が走る
		val tasks = pool.getTasks()
		assertEquals(1, tasks.size, "完了したタスクがクリーンアップされていること")
		assertSame(activeTask, tasks[0], "アクティブなタスクのみ残ること")
	}

	@Test
	fun `LTaskPoolImplのcancelメソッドですべてのタスクが一括キャンセルされ削除されること`() {
		val pool = LTaskPoolImpl()
		val task1 = mockk<LTask>(relaxed = true)
		val task2 = mockk<LTask>(relaxed = true)

		pool.add(task1)
		pool.add(task2)

		pool.cancel()

		// 各タスクの cancel() が呼び出されたことを検証
		verify(exactly = 1) { task1.cancel() }
		verify(exactly = 1) { task2.cancel() }
		assertEquals(0, pool.getTasks().size, "すべてのタスクがプールから削除されていること")
	}

	@Test
	fun `TimerOptionが正しく値を保持し条件判定できること`() {
		val option = TimerOption(
			delay = 0L,
			period = 1L,
			start = 10L,
			step = -2L,
			test = { it > 5 }
		)

		assertEquals(10L, option.start)
		assertEquals(-2L, option.step)
		assertEquals(0L, option.delay)
		assertEquals(1L, option.period)

		assertTrue(option.test.test(10), "10は5より大きいのでtrue")
		assertTrue(option.test.test(6), "6は5より大きいのでtrue")
		assertFalse(option.test.test(5), "5は5より大きくないのでfalse")
		assertFalse(option.test.test(4), "4は5より大きくないのでfalse")
	}
}
