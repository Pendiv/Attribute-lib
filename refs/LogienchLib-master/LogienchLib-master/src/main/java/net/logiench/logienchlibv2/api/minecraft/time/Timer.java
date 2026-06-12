package net.logiench.logienchlibv2.api.minecraft.time;

import net.logiench.logienchlibv2.base.minecraft.time.TaskScheduler;
import org.bukkit.NamespacedKey;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public final class Timer extends TaskScheduler {

	private static final Map<UUID, List<TimeTask>> timers = new HashMap<>();
	private static final Map<NamespacedKey, TimeTask> worldTimers = new HashMap<>();

	/**
	 * 一切キャッシュされないタイマーです。
	 * 止めるには返り値である {@link TimeTask} からしかできないので注意してください
	 */
	public static TimeTask on(Runnable runnable, long delay, long period) {
		return TaskScheduler.runTimer(runnable, delay, period);
	}

	/**
	 * 各プレイヤーに紐づけてキャッシュされるタイマーです。
	 * 紐づけ対象のプレイヤーが退出すると停止します
	 */
	public static void on(UUID uuid, Runnable timerFunction, long delay, long period) {
		timers.computeIfAbsent(uuid, k -> new ArrayList<>())
			.add(runTimer(timerFunction, delay, period));
	}

	/**
	 * {@link Timer} にキャッシュさせるタイマーです。
	 * {@link #stop(NamespacedKey)} で止めることができます
	 */
	public static void on(NamespacedKey name, Runnable timerFunction, long delay, long period) {
		worldTimers.put(
			name,
			TaskScheduler.runTimer(timerFunction, delay, period)
		);
	}

	/**
	 * {@link #on(NamespacedKey, Runnable, long, long)} で開始されたタイマーを止めることができます
	 */
	public static void stop(NamespacedKey name) {
		TimeTask task = worldTimers.remove(name);
		if (task == null) {
			return;
		}
		task.cancel();
	}

	public static void removePlayerTasks(UUID uuid) {
		List<TimeTask> task = timers.remove(uuid);
		if (task == null) {
			return;
		}
		for (TimeTask run : task) {
			run.cancel();
		}
	}

	/**
	 * 指定されたUUIDのプレイヤーがオンラインの間、このタイマーは動作します
	 *
	 * @see #startTimer(Consumer, long, long, double, Predicate, double)
	 */
	public static TimeTask startTimer(UUID uuid, Consumer<Double> timerFunction, long delay, long period, double start, Predicate<Double> test, double add) {
		TimeTask task = startTimer(timerFunction, delay, period, start, test, add);
		timers.computeIfAbsent(uuid, k -> new ArrayList<>()).add(task);
		return task;
	}

	/**
	 * for文のように使用できるTimerです
	 * <pre><code>for (double i = start; test.test(i); i += add)</code></pre>
	 * 引数をforに置き換えるとこうなります
	 */
	public static TimeTask startTimer(Consumer<Double> timerFunction, long delay, long period, double start, Predicate<Double> test, double add) {
		AtomicReference<Double> i = new AtomicReference<>(start);
		AtomicReference<TimeTask> task = new AtomicReference<>();
		task.set(on(() -> {
			double number = i.get();
			if (!test.test(number)) {
				task.get().cancel();
				return;
			}
			timerFunction.accept(number);
			i.set(number + add);
		}, delay, period));
		return task.get();
	}

	/**
	 * 指定されたUUIDのプレイヤーがオンラインの間、このタイマーは動作します
	 *
	 * @see #startTimer(Supplier, long, long)
	 */
	public static TimeTask startTimer(UUID uuid, Supplier<Boolean> timerFunction, long delay, long period) {
		TimeTask task = startTimer(timerFunction, delay, period);
		timers.computeIfAbsent(uuid, k -> new ArrayList<>()).add(task);
		return task;
	}

	/**
	 * 次のTimerを実行するか毎回決めることができます
	 *
	 * @param timerFunction return true: 次の処理を停止, false: 続行
	 */
	public static TimeTask startTimer(Supplier<Boolean> timerFunction, long delay, long period) {
		AtomicInteger i = new AtomicInteger();
		AtomicReference<TimeTask> task = new AtomicReference<>();
		task.set(on(() -> {
			if (timerFunction.get()) {
				task.get().cancel();
			}
		}, delay, period));
		return task.get();
	}

	/**
	 * 指定されたUUIDのプレイヤーがオンラインの間、このタイマーは動作します
	 *
	 * @see #startTimer(Consumer, long, long, int)
	 */
	public static TimeTask startTimer(UUID uuid, Consumer<Integer> timerFunction, long delay, long period, int count) {
		TimeTask task = startTimer(timerFunction, delay, period, count);
		timers.computeIfAbsent(uuid, k -> new ArrayList<>()).add(task);
		return task;
	}

	/**
	 * 指定回数タイマーを動かします
	 *
	 * @param timerFunction Integer: 繰り返しの回数(0 ~ count - 1)
	 * @param count         動作する回数
	 */
	public static TimeTask startTimer(Consumer<Integer> timerFunction, long delay, long period, int count) {
		return startTimer(i -> timerFunction.accept((int) Math.round(i)), delay, period, 0, i -> i < count, 1);
	}
}