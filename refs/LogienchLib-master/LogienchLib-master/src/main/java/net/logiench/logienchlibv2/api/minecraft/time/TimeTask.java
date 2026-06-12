package net.logiench.logienchlibv2.api.minecraft.time;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.scheduler.BukkitTask;

@SuppressWarnings("unused")
public class TimeTask {
	private final ScheduledTask foliaTask;
	private final BukkitTask bukkitTask;

	protected TimeTask(ScheduledTask foliaTask, BukkitTask bukkitTask) {
		if (foliaTask != null && bukkitTask != null) {
			throw new IllegalArgumentException("Only one or the other can be set");
		}
		this.foliaTask = foliaTask;
		this.bukkitTask = bukkitTask;
	}

	public TimeTask(ScheduledTask foliaTask) {
		this.foliaTask = foliaTask;
		this.bukkitTask = null;
	}

	public TimeTask(BukkitTask bukkitTask) {
		this.foliaTask = null;
		this.bukkitTask = bukkitTask;
	}

	public boolean isCancelled() {
		if (foliaTask != null) {
			return foliaTask.isCancelled();
		}
		if (bukkitTask != null) {
			return bukkitTask.isCancelled();
		}
		return true;
	}

	public void cancel() {
		if (foliaTask != null) {
			foliaTask.cancel();
		} else if (bukkitTask != null) {
			bukkitTask.cancel();
		}
	}

	protected ScheduledTask getFoliaTask() {
		return foliaTask;
	}

	protected BukkitTask getBukkitTask() {
		return bukkitTask;
	}
}
