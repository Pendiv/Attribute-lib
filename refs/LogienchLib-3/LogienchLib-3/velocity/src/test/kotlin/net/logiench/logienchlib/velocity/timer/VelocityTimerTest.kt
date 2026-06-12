package net.logiench.logienchlib.velocity.timer

import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.scheduler.ScheduledTask
import com.velocitypowered.api.scheduler.Scheduler
import com.velocitypowered.api.scheduler.TaskStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import net.logiench.logienchlib.core.timer.LTaskPoolImpl
import net.logiench.logienchlib.velocity.VelocityTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.*

class VelocityTimerTest : VelocityTestBase() {

	private lateinit var timerService: VelocityTimerServiceImpl
	private lateinit var playerLTaskPool: PlayerLTaskPoolImpl
	private val schedulerMock = mockk<Scheduler>()
	private val builderMock = mockk<Scheduler.TaskBuilder>(relaxed = true)

	@BeforeEach
	override fun setUp() {
		super.setUp()
		// proxyServerのschedulerが呼び出されたら `schedulerMock` を返すように機能を作成
		every { proxyServer.scheduler } returns schedulerMock
		every { schedulerMock.buildTask(any(), any<Runnable>()) } returns builderMock
		every { builderMock.delay(any<Duration>()) } returns builderMock

		playerLTaskPool = PlayerLTaskPoolImpl(proxyServer)
		timerService = VelocityTimerServiceImpl(proxyServer, playerLTaskPool, plugin)
	}

	@Test
	fun `タスクが正常に実行完了したときにプールから削除されること`() {
		val taskMock = mockk<ScheduledTask>(relaxed = true)
		val runnableSlot = slot<Runnable>()

		every { schedulerMock.buildTask(any(), capture(runnableSlot)) } returns builderMock
		every { builderMock.schedule() } returns taskMock

		val pool = LTaskPoolImpl()

		timerService.onDelay(pool, Duration.ofSeconds(1)) {}
		assertEquals(1, pool.getTasks().size, "登録直後はプールに存在する")

		// 完了を模倣
		runnableSlot.captured.run()

		assertEquals(0, pool.getTasks().size, "実行完了後はプールから削除されていること")
	}

	@Test
	fun `タスクを手動キャンセルしたときにプールから削除されること`() {
		val taskMock = mockk<ScheduledTask>()
		var status = TaskStatus.SCHEDULED
		every { taskMock.status() } answers { status }
		every { taskMock.cancel() } answers { status = TaskStatus.CANCELLED }
		every { builderMock.schedule() } returns taskMock

		val pool = LTaskPoolImpl()

		val task = timerService.onDelay(pool, Duration.ofSeconds(1)) {}
		assertEquals(1, pool.getTasks().size, "登録直後はプールに存在する")

		task.cancel()

		assertEquals(0, pool.getTasks().size, "キャンセルされたタスクはプールから消えること")
	}

	@Test
	fun `プレイヤー退出時にプールがキャンセルされ空になること`() {
		val playerId = UUID.randomUUID()
		val playerMock = mockk<Player>()
		every { playerMock.uniqueId } returns playerId
		every { playerMock.isActive } returns true
		every { proxyServer.getPlayer(playerId) } returns Optional.of(playerMock)

		val pool = playerLTaskPool.get(playerId)
		val taskMock = mockk<ScheduledTask>()
		var status = TaskStatus.SCHEDULED
		every { taskMock.status() } answers { status }
		every { taskMock.cancel() } answers { status = TaskStatus.CANCELLED }
		every { builderMock.schedule() } returns taskMock

		// プールにタスクを登録
		timerService.onDelay(pool, Duration.ofSeconds(1)) {}
		assertEquals(1, pool.getTasks().size)

		// プレイヤー切断イベントをシミュレート
		val disconnectEvent = DisconnectEvent(playerMock, DisconnectEvent.LoginStatus.SUCCESSFUL_LOGIN)

		// リフレクションで private メソッド onPlayerQuit(DisconnectEvent) を呼び出す
		val method = PlayerLTaskPoolImpl::class.java.getDeclaredMethod("onPlayerQuit", DisconnectEvent::class.java)
		method.isAccessible = true
		method.invoke(playerLTaskPool, disconnectEvent)

		// 切断後にタスクが自動でキャンセルされプールが空になっていること
		assertEquals(0, pool.getTasks().size, "切断によりタスクプールがクリアされていること")
	}
}
