package net.logiench.shardLib.core.attribute;

import net.logiench.shardLib.api.attribute.Ticket;
import net.logiench.shardLib.api.attribute.data.AttributeModifier;
import net.logiench.shardLib.api.attribute.data.AttributeOperationModifier;
import net.logiench.shardLib.api.attribute.data.AttributeValueProvider;
import net.logiench.shardLib.api.attribute.data.CalculationContext;
import net.logiench.shardLib.api.player.PlayerAPI;
import net.logiench.shardLib.api.player.PlayerAttributeAPI;
import net.logiench.shardLib.api.player.PlayerCharacterAPI;
import net.logiench.shardLib.api.register.attribute.AttributeDefinitionRegister;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PlayerAttributeAPIImpl extends AttributeAPIImpl implements PlayerAttributeAPI {
	private final UUID playerUUID;
	private final PlayerAPI playerAPI;
	//現在DBに保存されているModifierのinstance_idのマップ。このMapに存在しなくても実際は適応されているModifierが存在します
	private final Map<AttributeOperationModifier, Long> modifierInstanceIds = new HashMap<>();

	private final Map<AttributeOperationModifier, Integer> modifierTimerStartTicks = new HashMap<>();
	private final Set<Long> deleteModifierInstanceId = new HashSet<>();
	private final Set<Long> deleteProviderInstanceId = new HashSet<>();

	public PlayerAttributeAPIImpl(UUID playerUUID, AttributeManager attributeManager, AttributeDefinitionRegister register, PlayerAPI playerAPI) {
		super(attributeManager, register);
		this.playerUUID = playerUUID;
		this.playerAPI = playerAPI;
	}

	@Override
	@NotNull
	public Ticket addProvider(AttributeValueProvider provider) throws IllegalArgumentException {
		return addOperationModifier(provider);
	}

	@Override
	public void removeProvider(AttributeValueProvider provider) {
		removeOperationModifier(provider);
	}

	@Override
	public void removeOperationModifier(AttributeOperationModifier modifier) {
		Long id = modifierInstanceIds.remove(modifier);
		if (id != null) {
			switch (modifier) {
				case AttributeModifier m -> deleteModifierInstanceId.add(id);
				case AttributeValueProvider m -> deleteProviderInstanceId.add(id);
			}
		}
		modifierTimerStartTicks.remove(modifier);
		super.removeOperationModifier(modifier);
	}

	@Override
	Map<String, Double> getRecalculatedStats() {
		// 密結合(Playerに対しての重度の依存状態)を避けるため
		Player player = Bukkit.getPlayer(playerUUID);
		if (player == null) {
			return baseAttributes;
		}
		Optional<PlayerCharacterAPI> characterAPI = playerAPI.getCharacterAPI(player);
		if (characterAPI.isEmpty()) {
			throw new IllegalStateException("PlayerCharacter Not Found");
		}
		return attributeManager.calculateStats(
			new CalculationContext(player, characterAPI.get()),
			baseAttributes,
			finalModifiers
		);
	}

	@Override
	@NotNull
	public Ticket addOperationModifier(AttributeOperationModifier modifier) throws IllegalArgumentException {
		Ticket ticket = super.addOperationModifier(modifier);
		if (modifier.getDurationTicks().isPresent()) {
			// Mapからの削除は、内部的にremoveOperationModifierが呼び出されているので問題なし
			modifierTimerStartTicks.put(modifier, Bukkit.getCurrentTick());
		}
		return ticket;
	}

	public Map<AttributeOperationModifier, Long> getModifierInstanceIds() {
		return modifierInstanceIds;
	}

	public Set<Long> getDeleteModifierInstanceId() {
		return deleteModifierInstanceId;
	}

	public Set<Long> getDeleteProviderInstanceId() {
		return deleteProviderInstanceId;
	}

	public OptionalLong getStartTicks(AttributeOperationModifier modifier) {
		Integer startTicks = modifierTimerStartTicks.get(modifier);
		if (startTicks == null) {
			return OptionalLong.empty();
		}
		return OptionalLong.of(startTicks);
	}

	public OptionalLong getRemainingTicks(AttributeOperationModifier modifier) {
		OptionalLong durationTicks = modifier.getDurationTicks();
		if (durationTicks.isPresent()) {
			Integer startTicks = modifierTimerStartTicks.get(modifier);
			if (startTicks == null) {
				return OptionalLong.empty();
			}
			int timeElapsedTicks = Bukkit.getCurrentTick() - startTicks;
			long remainingTicks = durationTicks.getAsLong() - timeElapsedTicks;
			if (remainingTicks > 0) {
				return OptionalLong.of(remainingTicks);
			}
			return OptionalLong.empty();
		}
		return OptionalLong.empty();
	}
}
