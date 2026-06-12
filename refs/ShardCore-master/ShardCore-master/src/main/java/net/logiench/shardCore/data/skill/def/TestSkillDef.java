package net.logiench.shardCore.data.skill.def;

import net.kyori.adventure.text.Component;
import net.logiench.shardCore.core.skill.base.ActiveSkill;
import net.logiench.shardCore.core.skill.base.SkillDefinition;
import net.logiench.shardCore.core.skill.system.SkillContext;
import net.logiench.shardCore.core.skill.system.SkillManager;
import net.logiench.shardCore.util.PlayerUtils;
import org.jetbrains.annotations.NotNull;

public class TestSkillDef extends SkillDefinition {

	@Override
	public boolean canCast(@NotNull SkillContext context, @NotNull SkillManager manager) {
		return super.canCast(context, manager) && PlayerUtils.isOnGround(context.player());
	}

	@Override
	public int getMaxLevel() {
		return 1;
	}

	@Override
	public @NotNull ActiveSkill createInstance(SkillContext context) throws IllegalArgumentException {
		if (!isInSkillLevel(context)) {
			throw new IllegalArgumentException();
		}
		return new ActiveTestSkill(context);
	}

	@Override
	public @NotNull String getId() {
		return "test1";
	}

	@Override
	public @NotNull Component getDisplayName() {
		return Component.text("test");
	}

	@Override
	public double getManaCost() {
		return 55;
	}
}
