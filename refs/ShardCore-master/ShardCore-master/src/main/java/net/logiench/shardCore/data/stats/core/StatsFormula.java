package net.logiench.shardCore.data.stats.core;

import net.logiench.shardLib.api.ShardLibProvider;
import net.logiench.shardLib.api.attribute.AttributeDefinition;
import net.logiench.shardLib.api.register.attribute.AttributeDefinitionRegister;

import java.util.List;

public class StatsFormula {
	public void register() {
		AttributeDefinitionRegister attributeDefinitionRegister = ShardLibProvider.get().getRegister().attribute().coreAttribute();

		attributeDefinitionRegister.register(new AttributeDefinition(
			"natural_damage", List.of("test1_value", "test2_value"), m -> {
			return m.get("test1_value") * m.get("test2_value");
		}));
	}
}
