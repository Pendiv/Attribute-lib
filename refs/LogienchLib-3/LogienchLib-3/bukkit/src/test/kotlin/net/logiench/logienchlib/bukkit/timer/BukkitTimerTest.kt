package net.logiench.logienchlib.bukkit.timer

import net.logiench.logienchlib.api.timer.Delay
import net.logiench.logienchlib.api.timer.PlayerLTaskPool
import net.logiench.logienchlib.bukkit.BukkitTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class BukkitTimerTest : BukkitTestBase() {

	@Test
	fun `タスクが正常に実行完了したときにプールから削除されること`() {
		val player = server.addPlayer("TEST PLAYER")
		val pool = PlayerLTaskPool.get(player.uniqueId)

		// 10tick後に完了するタスクを登録
		val task = Delay.on(10L, pool) {}
		Assertions.assertEquals(1, pool.getTasks().size, "登録直後はプールに存在する")

		// 時間を進めて実行完了させる
		server.scheduler.performTicks(10)

		Assertions.assertTrue(task.isFinished(), "タスクが完了していること")
		Assertions.assertEquals(0, pool.getTasks().size, "実行完了後はプールから削除されていること")
	}

	@Test
	fun `タスクを手動キャンセルしたときにプールから削除されること`() {
		val player = server.addPlayer("TEST PLAYER")
		val pool = PlayerLTaskPool.get(player.uniqueId)

		val task = Delay.on(10L, pool) {}
		Assertions.assertEquals(1, pool.getTasks().size, "登録直後はプールに存在する")

		// 手動キャンセル
		task.cancel()

		Assertions.assertTrue(task.isCancelled(), "タスクがキャンセル状態になっていること")
		Assertions.assertEquals(0, pool.getTasks().size, "キャンセルされたタスクはプールから消えること")
	}

	@Test
	fun `プレイヤー退出時にプールがキャンセルされ空になること`() {
		val player = server.addPlayer("TEST PLAYER")
		val pool = PlayerLTaskPool.get(player.uniqueId)

		val task = Delay.on(10L, pool) {}
		Assertions.assertEquals(1, pool.getTasks().size, "登録直後はプールに存在する")

		// プレイヤーを切断する (PlayerQuitEventを発火させる)
		player.disconnect()

		Assertions.assertTrue(task.isCancelled(), "切断によりタスクがキャンセルされていること")
		Assertions.assertEquals(0, pool.getTasks().size, "プール内のタスクが空になっていること")
	}
}