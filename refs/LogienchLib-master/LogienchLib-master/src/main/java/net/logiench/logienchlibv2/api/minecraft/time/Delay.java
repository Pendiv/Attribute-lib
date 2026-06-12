package net.logiench.logienchlibv2.api.minecraft.time;

import net.logiench.logienchlibv2.base.minecraft.time.TaskScheduler;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public final class Delay extends TaskScheduler {

	private static final Map<UUID, List<TimeTask>> delays = new HashMap<>();

	public static TimeTask on(Runnable r, long delay) {
		return TaskScheduler.runLater(r, delay);
	}

	/**
	 * 対象のプレイヤーが退出した場合、自動で停止されます
	 */
	public static TimeTask on(UUID uuid, Runnable r, long delay) {
		AtomicReference<TimeTask> task = new AtomicReference<>();

		task.set(new DelayTask(uuid, TaskScheduler.runLater(() -> {
			r.run();
			removeTask(uuid, task.get());
		}, delay)));
		delays.computeIfAbsent(uuid, k -> new ArrayList<>())
			.add(task.get());
		return task.get();
	}

	/**
	 * 対象のプレイヤーが退出した場合、自動で停止されます
	 */
	public static TimeTask on(Player p, Runnable r, long delay) {
		return on(p.getUniqueId(), r, delay);
	}


	/**
	 * 対象のプレイヤーが退出した場合、自動で停止されます
	 */
	public static TimeTask on(Player p, Consumer<Player> r, long delay) {
		return on(p, () -> r.accept(p), delay);
	}

	public static void removePlayerTasks(UUID uuid) {
		List<TimeTask> timers = delays.remove(uuid);
		if (timers == null || timers.isEmpty()) {
			return;
		}
		timers.forEach(TimeTask::cancel);
	}

	private static void removeTask(UUID uuid, TimeTask task) {
		if (uuid == null) {
			return;
		}
		List<TimeTask> tasks = delays.get(uuid);
		if (tasks == null || tasks.isEmpty()) {
			return;
		}
		tasks.remove(task);
	}

	public static class DelayTask extends TimeTask {
		private final UUID uuid;

		public DelayTask(@Nullable UUID uuid, TimeTask task) {
			super(task.getFoliaTask(), task.getBukkitTask());
			this.uuid = uuid;
		}

		@Override
		public void cancel() {
			super.cancel();
			removeTask(uuid, this);
		}
	}
}

