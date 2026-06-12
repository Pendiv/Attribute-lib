package net.logiench.shardCore.core.mob.base;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractGoal<T extends Mob> implements Goal<T> {
	protected final T mob;
	private final GoalKey<T> key;
	private int activeTicks = 0;

	@SuppressWarnings("unchecked")
	public AbstractGoal(@NotNull T mob, @NotNull NamespacedKey key) {
		this.mob = mob;
		this.key = GoalKey.of((Class<T>) mob.getClass(), key);
	}

	public int activeTicks() {
		return activeTicks;
	}

	@Override
	@NotNull
	public final GoalKey<T> getKey() {
		return key;
	}

	@Override
	public final void tick() {
		activeTicks++;
		_tick();
	}

	@Override
	public final void stop() {
		try {
			_stop();
		} finally {
			this.activeTicks = 0;
		}
	}

	public abstract void _tick();

	public abstract void _stop();
}
