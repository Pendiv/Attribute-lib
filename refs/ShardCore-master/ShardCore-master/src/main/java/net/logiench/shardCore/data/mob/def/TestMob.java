package net.logiench.shardCore.data.mob.def;

import com.destroystokyo.paper.entity.ai.MobGoals;
import net.kyori.adventure.text.Component;
import net.logiench.shardCore.core.mob.base.ShardMob;
import net.logiench.shardCore.core.stats.base.AttributeEnum;
import net.logiench.shardCore.data.mob.ai.ChargedSmashSkill;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class TestMob implements ShardMob {

	@Override
	public String getId() {
		return "test_mob";
	}

	@Override
	public String getAttributeProfileId() {
		return "test";
	}

	@Override
	public @NotNull EntityType getEntityType() {
		return EntityType.HUSK;
	}

	@Override
	public @NotNull Component getName() {
		return Component.text("TEST_MOB");
	}

	@Override
	public double getMaxHp() {
		return 10;
	}

	@Override
	public @NotNull Map<AttributeEnum, Double> getStats() {
		return Map.of();
	}

	@Override
	public @Nullable String getLootTableId() {
		return "test_loot";
	}

	@Override
	public void applyGoals(@NotNull MobGoals goals, @NotNull Mob mob) {
		//		goals.addGoal(mob, -9999, new TestGoal(mob));
		goals.addGoal(mob, -99999999, new ChargedSmashSkill(mob));
	}
}
