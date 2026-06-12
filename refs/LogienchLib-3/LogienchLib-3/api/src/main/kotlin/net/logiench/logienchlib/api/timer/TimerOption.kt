package net.logiench.logienchlib.api.timer

import java.time.Duration
import java.util.function.Predicate

/**
 * タイマーを詳細に制御するオプションです。
 * Ticks(Bukkit)とDuration(Velocity/モダン)の両方の環境に対応しています。
 *
 * @param delay タイマーの実行が開始されるまでの遅延tick数
 * @param period タイマーの実行間隔
 * @param start タイマーの内部カウンターの初期値
 * @param step タイマーの内部カウンターが1回で進む数
 * @param test このタイマーの実行を継続するか。現在の内部カウンターの値が与えられます。trueで継続します。
 */
data class TimerOption constructor(
	// Bukkit用のTicksベース
	val delay: Long,
	val period: Long,
	val start: Long = 0,
	val step: Long = 1,
	val test: Predicate<Long>,

	// VelocityおよびDurationベース用
	val delayDuration: Duration = TickConvert.toDuration(delay),
	val periodDuration: Duration = TickConvert.toDuration(period)
) {
	// 既存互換・Ticksベース

	/**
	 * 元のコンストラクタ：最小構成のTicksベースタイマーを生成します。
	 */
	constructor(delay: Long, period: Long, test: Predicate<Long>) :
			this(
				delay = delay,
				period = period,
				start = 0,
				step = 1,
				test = test,
				delayDuration = TickConvert.toDuration(delay), // Ticksから自動でDurationを逆算
				periodDuration = TickConvert.toDuration(period)
			)

	/**
	 * 元のコンストラクタ：指定した回数だけ実行するTicksベースタイマーを生成します。
	 * @param repeatCount タイマーを実行する回数
	 */
	constructor(delay: Long, period: Long, repeatCount: Long) :
			this(
				delay = delay,
				period = period,
				start = 0,
				step = 1,
				test = Predicate<Long> { it < repeatCount },
				delayDuration = TickConvert.toDuration(delay),
				periodDuration = TickConvert.toDuration(period)
			)

	/**
	 * 元のコンストラクタ：範囲指定（カウントアップ/ダウン）を行うTicksベースタイマーを生成します。
	 * @param start タイマーの内部カウンターの初期値
	 * @param step タイマーの内部カウンターが1回で進む数
	 * @param endInclude タイマーの内部カウンターの範囲制限。この値を含む間は繰り返し続けます
	 */
	@JvmOverloads
	constructor(delay: Long, period: Long, start: Long, endInclude: Long, step: Long = 1) : this(
		delay = delay,
		period = period,
		start = start,
		step = step,
		test = Predicate<Long>(
			if (start <= endInclude) {
				{ it in start..endInclude }
			} else {
				{ it in endInclude..start }
			}
		),
		delayDuration = TickConvert.toDuration(delay),
		periodDuration = TickConvert.toDuration(period)
	)


	// Velocity / Durationベース

	/**
	 * 新設コンストラクタ：Durationベースのタイマーを詳細設定で生成します。
	 */
	@JvmOverloads
	constructor(
		delayDuration: Duration,
		periodDuration: Duration,
		start: Long = 0,
		step: Long,
		test: Predicate<Long>
	) : this(
		delay = TickConvert.toTick(delayDuration), // Bukkitの裏方実装用として、ミリ秒からTicksへ自動逆算
		period = TickConvert.toTick(periodDuration),
		start = start,
		step = step,
		test = test,
		delayDuration = delayDuration,         // 指定された正確なDuration（ミリ秒単位）をそのまま100%保持
		periodDuration = periodDuration
	)

	/**
	 * 新設コンストラクタ：Durationベースの最小構成タイマーを生成します。
	 */
	constructor(delayDuration: Duration, periodDuration: Duration, test: Predicate<Long>) :
			this(delayDuration = delayDuration, periodDuration = periodDuration, start = 0, step = 1, test = test)

	/**
	 * 新設コンストラクタ：指定した回数だけ実行するDurationベースタイマーを生成します。
	 * @param repeatCount タイマーを実行する回数
	 */
	constructor(delayDuration: Duration, periodDuration: Duration, repeatCount: Long) :
			this(
				delayDuration = delayDuration,
				periodDuration = periodDuration,
				start = 0,
				step = 1,
				test = Predicate<Long> { it < repeatCount })

	/**
	 * 新設コンストラクタ：範囲指定（カウントアップ/ダウン）を行うDurationベースタイマーを生成します。
	 * @param start タイマーの内部カウンターの初期値
	 * @param step タイマーの内部カウンターが1回で進む数
	 * @param endInclude タイマーの内部カウンターの範囲制限。この値を含む間は繰り返し続けます
	 */
	@JvmOverloads
	constructor(
		delayDuration: Duration,
		periodDuration: Duration,
		start: Long,
		endInclude: Long,
		step: Long = 1
	) : this(
		delayDuration = delayDuration,
		periodDuration = periodDuration,
		start = start,
		step = step,
		test = Predicate<Long>(
			if (start <= endInclude) {
				{ it in start..endInclude }
			} else {
				{ it in endInclude..start }
			}
		)
	)
}