package net.logiench.logienchlibv2.base.minecraft.time;

import net.logiench.logienchlibv2.LogienchLib;
import net.logiench.logienchlibv2.api.minecraft.time.TimeTask;
import org.bukkit.Bukkit;

public class TaskScheduler {
	protected static void run(Runnable runnable) {
		Bukkit.getScheduler().runTask(LogienchLib.getInstance(), runnable);
	}

	protected static TimeTask runLater(Runnable runnable, long delayTicks) {
		return new TimeTask(Bukkit.getScheduler().runTaskLater(LogienchLib.getInstance(), runnable, delayTicks));
	}

	protected static TimeTask runTimer(Runnable runnable, long delayTicks, long periodTicks) {
		return new TimeTask(Bukkit.getScheduler().runTaskTimer(LogienchLib.getInstance(), runnable, delayTicks, periodTicks));
	}
}
