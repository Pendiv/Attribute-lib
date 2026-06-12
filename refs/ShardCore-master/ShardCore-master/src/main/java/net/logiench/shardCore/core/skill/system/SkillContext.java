package net.logiench.shardCore.core.skill.system;

import net.logiench.shardCore.core.skill.base.SkillDefinition;
import net.logiench.shardLib.api.player.PlayerAttributeAPI;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record SkillContext(
	@NotNull Player player,
	@NotNull PlayerAttributeAPI attribute,
	int level
) {
	public SkillContext(@NotNull Player player, @NotNull PlayerAttributeAPI attribute) {
		this(player, attribute, SkillDefinition.MIN_SKILL_LEVEL);
	}

	@NotNull
	public UUID getUniqueId() {
		return player.getUniqueId();
	}
}
