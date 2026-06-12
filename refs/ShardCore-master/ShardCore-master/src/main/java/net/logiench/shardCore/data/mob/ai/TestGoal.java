package net.logiench.shardCore.data.mob.ai;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import net.kyori.adventure.text.Component;
import net.logiench.shardCore.ShardCore;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.Mob;
import org.jspecify.annotations.NonNull;

import java.util.EnumSet;

public class TestGoal implements Goal<Mob> {
	private final NamespacedKey defenseKey = new NamespacedKey(ShardCore.getInstance(), "defense");
	private final Mob mob;
	private int interval = 100;

	public TestGoal(Mob mob) {
		this.mob = mob;
		mob.setCustomNameVisible(true);
	}

	@Override
	public boolean shouldActivate() {
		interval--;
		mob.customName(Component.text(interval + " / 200"));
		if (interval <= 0) {
			interval = 200;
		}
		return interval > 100;
	}

	@Override
	public @NonNull GoalKey<Mob> getKey() {
		return GoalKey.of((Class<Mob>) mob.getClass(), defenseKey);
	}

	@Override
	public @NonNull EnumSet<GoalType> getTypes() {
		return EnumSet.of(GoalType.MOVE);
	}

	@Override
	public void start() {
		//		System.out.println("Starting test goal");
	}

	@Override
	public void stop() {
		//		System.out.println("Stopping test goal");
	}

	@Override
	public void tick() {
		mob.getWorld().spawnParticle(Particle.ASH, mob.getLocation().add(0, 3, 0), 2);
	}
}
