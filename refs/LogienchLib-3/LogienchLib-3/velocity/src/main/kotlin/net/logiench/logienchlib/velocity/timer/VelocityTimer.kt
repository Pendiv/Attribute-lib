package net.logiench.logienchlib.velocity.timer

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.scheduler.ScheduledTask
import com.velocitypowered.api.scheduler.Scheduler
import com.velocitypowered.api.scheduler.TaskStatus
import jakarta.inject.Singleton
import net.logiench.logienchlib.api.internal.TimerService
import net.logiench.logienchlib.api.timer.LTask
import net.logiench.logienchlib.api.timer.LTaskPool
import net.logiench.logienchlib.api.timer.TickConvert
import net.logiench.logienchlib.api.timer.TimerOption
import net.logiench.logienchlib.core.timer.LTaskPoolImpl
import net.logiench.logienchlib.velocity.LogienchLibBootstrap
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class VelocityLTaskImpl : LTask {

	internal var task: ScheduledTask? = null

	override fun isFinished(): Boolean = task?.status() == TaskStatus.FINISHED || isCancelled()

	override fun isCancelled(): Boolean = task?.status() == TaskStatus.CANCELLED

	override fun cancel() {
		task?.cancel()
	}
}

@Singleton
class PlayerLTaskPoolImpl(private val server: ProxyServer) {

	private val playerLTaskPools: MutableMap<UUID, LTaskPool> = ConcurrentHashMap()

	fun get(playerId: UUID): LTaskPool {
		return playerLTaskPools[playerId] ?: run {
			// プレイヤーがnullまたはisOnlineがtrueではなかったら
			if (!(server.getPlayer(playerId).map { it.isActive }.orElse(false)!!)) {
				throw IllegalArgumentException("LTaskPoolを取得するプレイヤーはオンラインである必要があります")
			}
			LTaskPoolImpl().also { playerLTaskPools[playerId] = it }
		}
	}

	@Subscribe(priority = Short.MIN_VALUE)
	private fun onPlayerQuit(ev: DisconnectEvent) {
		playerLTaskPools.remove(ev.player.uniqueId)?.run { cancel() }
	}
}

@Singleton
class VelocityTimerServiceImpl(
	server: ProxyServer,
	private val playerLTaskPool: PlayerLTaskPoolImpl,
	private val pluginInstance: LogienchLibBootstrap
) : TimerService {

	private val scheduler = server.scheduler

	override fun getPlayerPool(playerId: UUID): LTaskPool =
		playerLTaskPool.get(playerId)

	override fun onTask(pool: LTaskPool?, run: () -> Unit): LTask {
		val (task, builder) = buildScheduler(pool, run)
		task.task = builder.schedule()
		return task
	}

	override fun onDelay(
		pool: LTaskPool?,
		tick: Long,
		run: () -> Unit
	): LTask {
		return onDelay(pool, TickConvert.toDuration(tick), run)
	}

	override fun onDelay(pool: LTaskPool?, duration: Duration, run: () -> Unit): LTask {
		val (task, builder) = buildScheduler(pool, run)
		task.task = builder.delay(duration).schedule()
		return task
	}

	override fun onTimer(
		pool: LTaskPool?,
		delay: Long,
		period: Long,
		run: () -> Unit
	): LTask {
		return onTimer(pool, TickConvert.toDuration(delay), TickConvert.toDuration(period), run)
	}

	private fun buildScheduler(pool: LTaskPool?, run: () -> Unit): Pair<VelocityLTaskImpl, Scheduler.TaskBuilder> {
		val task = VelocityLTaskImpl()
		pool?.add(task)
		return task to scheduler.buildTask(pluginInstance, Runnable {
			try {
				run()
			} finally {
				pool?.remove(task)
			}
		})
	}

	override fun onTimer(pool: LTaskPool?, delay: Duration, period: Duration, run: () -> Unit): LTask {
		val task = VelocityLTaskImpl()
		pool?.add(task)
		task.task = scheduler.buildTask(pluginInstance, Runnable { run() })
			.delay(delay).repeat(period).schedule()
		return task
	}

	override fun onTimer(pool: LTaskPool?, option: TimerOption, run: (Long) -> Unit): LTask {
		val task = VelocityLTaskImpl()
		pool?.add(task)
		var value = option.start
		task.task = scheduler.buildTask(pluginInstance, Runnable {
			if (!option.test.test(value)) {
				pool?.cancel(task) ?: task.cancel()
				return@Runnable
			}
			try {
				run(value)
			} finally {
				value += option.step
			}
		}).delay(option.delayDuration).repeat(option.periodDuration).schedule()
		return task
	}
}