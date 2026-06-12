package net.logiench.logienchlib.bukkit.timer

import jakarta.inject.Singleton
import net.logiench.logienchlib.api.internal.TimerService
import net.logiench.logienchlib.api.timer.LTask
import net.logiench.logienchlib.api.timer.LTaskPool
import net.logiench.logienchlib.api.timer.TickConvert
import net.logiench.logienchlib.api.timer.TimerOption
import net.logiench.logienchlib.core.timer.LTaskPoolImpl
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class BukkitLTaskImpl : LTask {

	internal var task: BukkitTask? = null
	internal var done = false

	override fun isFinished(): Boolean = done || isCancelled()

	override fun isCancelled(): Boolean = task?.isCancelled ?: false

	override fun cancel() {
		task?.cancel()
	}
}

@Singleton
class PlayerLTaskPoolImpl : Listener {

	private val playerLTaskPools: MutableMap<UUID, LTaskPool> = ConcurrentHashMap()

	fun get(playerId: UUID): LTaskPool {
		return playerLTaskPools[playerId] ?: run {
			// プレイヤーがnullまたはisOnlineがtrueではなかったら
			if (!(Bukkit.getPlayer(playerId)?.isOnline ?: false)) {
				throw IllegalArgumentException("LTaskPoolを取得するプレイヤーはオンラインである必要があります")
			}
			LTaskPoolImpl().also { playerLTaskPools[playerId] = it }
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	private fun onPlayerQuit(ev: PlayerQuitEvent) {
		playerLTaskPools.remove(ev.player.uniqueId)?.run { cancel() }
	}
}

@Singleton
class BukkitTimerServiceImpl(
	private val playerLTaskPool: PlayerLTaskPoolImpl,
	private val pluginInstance: JavaPlugin
) : TimerService {

	private val scheduler = Bukkit.getScheduler()

	override fun getPlayerPool(playerId: UUID): LTaskPool =
		playerLTaskPool.get(playerId)

	override fun onTask(pool: LTaskPool?, run: () -> Unit): LTask {
		val task = BukkitLTaskImpl()
		pool?.add(task)
		task.task = scheduler.runTask(pluginInstance, Runnable {
			try {
				run()
			} finally {
				task.done = true
				pool?.remove(task)
			}
		})
		return task
	}

	override fun onDelay(
		pool: LTaskPool?,
		duration: Duration,
		run: () -> Unit
	): LTask {
		return onDelay(pool, TickConvert.toTick(duration), run)
	}

	override fun onDelay(pool: LTaskPool?, tick: Long, run: () -> Unit): LTask {
		val task = BukkitLTaskImpl()
		pool?.add(task)
		task.task = scheduler.runTaskLater(pluginInstance, Runnable {
			try {
				run()
			} finally {
				task.done = true
				pool?.remove(task)
			}
		}, tick)
		return task
	}

	override fun onTimer(
		pool: LTaskPool?,
		delay: Duration,
		period: Duration,
		run: () -> Unit
	): LTask {
		return super.onTimer(
			pool,
			TickConvert.toTick(delay),
			TickConvert.toTick(period),
			run
		)
	}

	override fun onTimer(pool: LTaskPool?, delay: Long, period: Long, run: () -> Unit): LTask {
		val task = BukkitLTaskImpl()
		pool?.add(task)
		task.task = scheduler.runTaskTimer(pluginInstance, Runnable { run() }, delay, period)
		return task
	}

	override fun onTimer(pool: LTaskPool?, option: TimerOption, run: (Long) -> Unit): LTask {
		val task = BukkitLTaskImpl()
		pool?.add(task)
		var value = option.start
		task.task = scheduler.runTaskTimer(pluginInstance, Runnable {
			if (!option.test.test(value)) {
				task.done = true
				pool?.cancel(task) ?: task.cancel()
				return@Runnable
			}
			try {
				run(value)
			} finally {
				value += option.step
			}
		}, option.delay, option.period)
		return task
	}

}