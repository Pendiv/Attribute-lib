package net.logiench.shardLib.core.mob;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.logiench.shardLib.api.register.attribute.AttributeDefinitionRegister;
import net.logiench.shardLib.api.register.mob.MobAttributeRegister;
import net.logiench.shardLib.api.register.mob.MobRegister;
import net.logiench.shardLib.di.annotations.CoreAttribute;
import org.jetbrains.annotations.NotNull;

@Singleton
public class MobRegisterImpl implements MobRegister {
	private final MobAttributeRegister attributeRegister;
	private final AttributeDefinitionRegister coreAttribute;

	@Inject
	public MobRegisterImpl(MobAttributeRegister attributeRegister, @CoreAttribute AttributeDefinitionRegister coreAttribute) {
		this.attributeRegister = attributeRegister;
		this.coreAttribute = coreAttribute;
	}

	@Override
	public @NotNull MobAttributeRegister attributes() {
		return attributeRegister;
	}

	@Override
	public @NotNull AttributeDefinitionRegister coreAttributes() {
		return coreAttribute;
	}
}
