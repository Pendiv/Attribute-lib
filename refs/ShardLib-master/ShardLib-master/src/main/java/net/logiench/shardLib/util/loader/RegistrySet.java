package net.logiench.shardLib.util.loader;

import com.google.inject.Inject;
import net.logiench.shardLib.core.attribute.AttributeDefinitionRegisterImpl;
import net.logiench.shardLib.core.attribute.AttributeValueProviderRegisterImpl;
import net.logiench.shardLib.core.mob.MobAttributeRegisterImpl;
import net.logiench.shardLib.di.annotations.CoreAttribute;
import net.logiench.shardLib.di.annotations.MobCoreAttribute;
import net.logiench.shardLib.di.annotations.PlayerAttribute;


public record RegistrySet(
	// attribute/
	@CoreAttribute AttributeDefinitionRegisterImpl coreAttributeRegister,
	AttributeValueProviderRegisterImpl attributeValueProviderRegister,
	// item/

	// mob/
	@MobCoreAttribute AttributeDefinitionRegisterImpl mobCoreAttributeRegister,
	MobAttributeRegisterImpl mobAttributeRegister,
	// player/
	@PlayerAttribute AttributeDefinitionRegisterImpl playerAttributeRegister
) {
	@Inject
	public RegistrySet {
	}
}
