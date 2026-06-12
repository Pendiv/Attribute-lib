package net.logiench.logienchlibv2.api.minecraft.time;


import net.logiench.logienchlibv2.base.minecraft.time.TaskScheduler;

@SuppressWarnings("unused")
public final class Task extends TaskScheduler {
	public static void on(Runnable func) {
		run(func);
	}
}
