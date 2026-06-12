package net.logiench.logienchlib.api.internal

import io.avaje.inject.Secondary
import jakarta.inject.Singleton
import net.logiench.logienchlib.api.timer.LTask
import net.logiench.logienchlib.api.timer.LTaskPool
import net.logiench.logienchlib.api.timer.TimerOption
import org.jetbrains.annotations.ApiStatus
import java.time.Duration
import java.util.*

@ApiStatus.Internal
interface TimerService {

	fun getPlayerPool(playerId: UUID): LTaskPool = throw UnsupportedService("タイマー")

	fun onTask(pool: LTaskPool?, run: () -> Unit): LTask = throw UnsupportedService("タイマー")

	fun onDelay(pool: LTaskPool?, tick: Long, run: () -> Unit): LTask = throw UnsupportedService("タイマー")

	fun onTimer(pool: LTaskPool?, delay: Long, period: Long, run: () -> Unit): LTask =
		throw UnsupportedService("タイマー")

	fun onTimer(pool: LTaskPool?, option: TimerOption, run: (Long) -> Unit): LTask =
		throw UnsupportedService("タイマー")


	fun onDelay(pool: LTaskPool?, duration: Duration, run: () -> Unit): LTask =
		throw UnsupportedService("タイマー")

	fun onTimer(pool: LTaskPool?, delay: Duration, period: Duration, run: () -> Unit): LTask =
		throw UnsupportedService("タイマー")
}

@Secondary
@Singleton
internal class UnsupportedTimerService : TimerService

@ApiStatus.Internal
interface LTaskPoolService {
	fun create(vararg tasks: LTask): LTaskPool = throw UnsupportedService("タスクプールの作成")
}

@Secondary
@Singleton
internal class UnsupportedLTaskPoolService : LTaskPoolService
