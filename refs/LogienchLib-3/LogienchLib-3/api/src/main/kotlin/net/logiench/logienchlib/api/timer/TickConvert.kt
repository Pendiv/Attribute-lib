package net.logiench.logienchlib.api.timer

import java.time.Duration

object TickConvert {

	const val SINGLE_TICK_DURATION_MS = 50

	fun toTick(duration: Duration): Long = Math.floorDiv(duration.toMillis(), SINGLE_TICK_DURATION_MS)

	fun toDuration(tick: Long): Duration = Duration.ofMillis(tick * SINGLE_TICK_DURATION_MS)
}