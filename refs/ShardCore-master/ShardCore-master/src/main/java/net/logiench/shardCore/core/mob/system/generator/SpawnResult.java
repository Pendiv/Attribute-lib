package net.logiench.shardCore.core.mob.system.generator;

import net.logiench.shardLib.api.mob.MobCharacterAPI;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record SpawnResult(@NotNull State state, @Nullable LivingEntity entity, @Nullable MobCharacterAPI character,
						  @Nullable String message) {

	public SpawnResult {
		if (state == State.SUCCESS) {
			if (entity == null || character == null) {
				throw new IllegalStateException("StateがSUCCESSの場合、EntityやCharacterをnullにすることはできません");
			}
		}
	}

	public boolean isSuccess() {
		return state == State.SUCCESS;
	}

	@Nullable
	public LivingEntity recalcGet() {
		if (character != null) {
			character.getAttributeAPI().recalculateStats();
		}
		return entity;
	}

	public enum State {
		SUCCESS,
		NOT_FOUND,
		ERROR,
	}
}
