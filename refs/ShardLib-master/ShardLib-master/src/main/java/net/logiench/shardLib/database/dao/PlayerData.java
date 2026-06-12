package net.logiench.shardLib.database.dao;

import net.logiench.shardLib.api.attribute.data.AttributeModifier;
import net.logiench.shardLib.api.attribute.data.AttributeOperationModifier;
import net.logiench.shardLib.api.attribute.data.AttributeValueProvider;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PlayerData(
	UUID uuid, Map<String, Double> baseAttributes,
	List<AttributeModifier> modifiers,
	List<AttributeValueProvider> providers,
	Map<AttributeOperationModifier, Long> modifierRemainingTicks,
	Map<AttributeOperationModifier, Long> modifierInstanceIds
) {
}
