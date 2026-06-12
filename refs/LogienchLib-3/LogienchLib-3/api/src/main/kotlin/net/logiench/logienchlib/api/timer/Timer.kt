package net.logiench.logienchlib.api.timer

import net.logiench.logienchlib.api.InstanceHolder
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.time.Duration
import java.util.*
import java.util.function.Consumer

interface LTask {

	/**
	 * 処理が完了したか、キャンセルされるとtrue。それ以外の状態ならfalse
	 */
	fun isFinished(): Boolean

	/**
	 * 処理がキャンセルされたか。これは標準的な [BukkitTask.isCancelled] などのラッパーメソッドです。
	 */
	fun isCancelled(): Boolean

	/**
	 * このタスクをキャンセルする
	 */
	fun cancel()
}

/**
 * 複数のタスクを一括で管理するオブジェクトです。
 * 実行中のタスクのみを保持するように努力するため、 [LTask.isFinished]がtrueのものは自動で削除されます。
 */
interface LTaskPool {

	companion object {
		@JvmStatic
		fun create(vararg tasks: LTask): LTaskPool =
			InstanceHolder.lTaskPoolService.create(*tasks)
	}

	fun add(task: LTask): LTask

	fun remove(task: LTask): Boolean

	/**
	 * 現在保持しているタスク一覧を取得します。
	 * ここで取得できるタスクは全て実行中です。
	 */
	fun getTasks(): List<LTask>

	/**
	 * 指定したタスクをキャンセル後、削除します。
	 * 削除に失敗してもキャンセルは行われます。
	 */
	fun cancel(task: LTask)

	/**
	 * すべてのタスクをキャンセルし、削除します。
	 */
	fun cancel()
}

object PlayerLTaskPool {
	/**
	 * @throws IllegalArgumentException プレイヤーがオンラインでなかった場合
	 */
	fun get(playerId: UUID): LTaskPool = InstanceHolder.timerService.getPlayerPool(playerId)
}

// Bukkitの拡張関数
fun Player.runTask(run: Player.() -> Unit): LTask =
	Task.on({ run(this) }, this.uniqueId)

fun Player.runDelay(tick: Long, run: Player.() -> Unit): LTask =
	Delay.on({ run(this) }, tick, this.uniqueId)

fun Player.runTimer(delay: Long, period: Long, run: Player.() -> Unit): LTask =
	Timer.on({ run(this) }, delay, period, this.uniqueId)

fun Player.runTimer(option: TimerOption, run: Player.(Long) -> Unit): LTask =
	Timer.on({ run(this, it) }, option, this.uniqueId)

// Velocityの拡張関数
fun com.velocitypowered.api.proxy.Player.runTask(run: com.velocitypowered.api.proxy.Player.() -> Unit): LTask =
	Task.on({ run(this) }, this.uniqueId)

fun com.velocitypowered.api.proxy.Player.runDelay(
	duration: Duration,
	run: com.velocitypowered.api.proxy.Player.() -> Unit
): LTask =
	Delay.on({ run(this) }, duration, this.uniqueId)

fun com.velocitypowered.api.proxy.Player.runTimer(
	duration: Duration,
	period: Duration,
	run: com.velocitypowered.api.proxy.Player.() -> Unit
): LTask =
	Timer.on({ run(this) }, duration, period, this.uniqueId)

fun com.velocitypowered.api.proxy.Player.runTimer(
	option: TimerOption,
	run: com.velocitypowered.api.proxy.Player.(Long) -> Unit
): LTask =
	Timer.on({ run(this, it) }, option, this.uniqueId)


object Task {

	@JvmOverloads
	@JvmStatic
	fun on(run: Runnable, pool: LTaskPool? = null): LTask =
		on(pool) { run.run() }

	/**
	 * Kotlinを使用している場合は[runTask]も使用できます
	 */
	@JvmStatic
	fun on(run: Runnable, playerId: UUID): LTask =
		on(PlayerLTaskPool.get(playerId)) { run.run() }

	@JvmSynthetic
	fun on(pool: LTaskPool? = null, run: () -> Unit): LTask =
		InstanceHolder.timerService.onTask(pool, run)

	@JvmSynthetic
	fun on(playerId: UUID, run: () -> Unit): LTask =
		InstanceHolder.timerService.onTask(PlayerLTaskPool.get(playerId), run)
}

object Delay {

	// Tick
	@JvmOverloads
	@JvmStatic
	fun on(run: Runnable, tick: Long, pool: LTaskPool? = null): LTask =
		on(tick, pool) { run.run() }

	/**
	 * Kotlinを使用している場合は[runDelay]も使用できます
	 */
	@JvmStatic
	fun on(run: Runnable, tick: Long, playerId: UUID): LTask =
		on(tick, playerId) { run.run() }

	@JvmSynthetic
	fun on(tick: Long, pool: LTaskPool? = null, run: () -> Unit): LTask =
		InstanceHolder.timerService.onDelay(pool, tick, run)

	@JvmSynthetic
	fun on(tick: Long, playerId: UUID, run: () -> Unit): LTask =
		InstanceHolder.timerService.onDelay(PlayerLTaskPool.get(playerId), tick, run)


	// Duration
	@JvmOverloads
	@JvmStatic
	fun on(run: Runnable, time: Duration, pool: LTaskPool? = null): LTask =
		on(time, pool) { run.run() }

	/**
	 * Kotlinを使用している場合は[runDelay]も使用できます
	 */
	@JvmStatic
	fun on(run: Runnable, time: Duration, playerId: UUID): LTask =
		on(time, playerId) { run.run() }

	@JvmSynthetic
	fun on(time: Duration, pool: LTaskPool? = null, run: () -> Unit): LTask =
		InstanceHolder.timerService.onDelay(pool, time, run)

	@JvmSynthetic
	fun on(time: Duration, playerId: UUID, run: () -> Unit): LTask =
		InstanceHolder.timerService.onDelay(PlayerLTaskPool.get(playerId), time, run)
}

object Timer {

	// Tick
	@JvmOverloads
	@JvmStatic
	fun on(run: Runnable, delay: Long, period: Long, pool: LTaskPool? = null): LTask =
		on(delay, period, pool) { run.run() }

	/**
	 * Kotlinを使用している場合は[runTimer]も使用できます
	 */
	@JvmStatic
	fun on(run: Runnable, delay: Long, period: Long, playerId: UUID): LTask =
		on(delay, period, playerId) { run.run() }

	@JvmSynthetic
	fun on(delay: Long, period: Long, pool: LTaskPool? = null, run: () -> Unit): LTask =
		InstanceHolder.timerService.onTimer(pool, delay, period, run)

	@JvmSynthetic
	fun on(delay: Long, period: Long, playerId: UUID, run: () -> Unit): LTask =
		InstanceHolder.timerService.onTimer(PlayerLTaskPool.get(playerId), delay, period, run)


	// Duration
	@JvmOverloads
	@JvmStatic
	fun on(run: Runnable, delay: Duration, period: Duration, pool: LTaskPool? = null): LTask =
		on(delay, period, pool) { run.run() }

	/**
	 * Kotlinを使用している場合は[runTimer]も使用できます
	 */
	@JvmStatic
	fun on(run: Runnable, delay: Duration, period: Duration, playerId: UUID): LTask =
		on(delay, period, playerId) { run.run() }

	@JvmSynthetic
	fun on(delay: Duration, period: Duration, pool: LTaskPool? = null, run: () -> Unit): LTask =
		InstanceHolder.timerService.onTimer(pool, delay, period, run)

	@JvmSynthetic
	fun on(delay: Duration, period: Duration, playerId: UUID, run: () -> Unit): LTask =
		InstanceHolder.timerService.onTimer(PlayerLTaskPool.get(playerId), delay, period, run)


	@JvmOverloads
	@JvmStatic
	fun on(run: Consumer<Long>, option: TimerOption, pool: LTaskPool? = null): LTask =
		on(option, pool) { run.accept(it) }

	/**
	 * Kotlinを使用している場合は[runTimer]も使用できます
	 */
	@JvmStatic
	fun on(run: Consumer<Long>, option: TimerOption, playerId: UUID): LTask =
		on(option, playerId) { run.accept(it) }

	@JvmSynthetic
	fun on(option: TimerOption, pool: LTaskPool? = null, run: (Long) -> Unit): LTask =
		InstanceHolder.timerService.onTimer(pool, option, run)

	@JvmSynthetic
	fun on(option: TimerOption, playerId: UUID, run: (Long) -> Unit): LTask =
		InstanceHolder.timerService.onTimer(PlayerLTaskPool.get(playerId), option, run)
}
