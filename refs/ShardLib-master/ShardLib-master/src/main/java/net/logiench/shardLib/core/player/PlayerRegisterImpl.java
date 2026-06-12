package net.logiench.shardLib.core.player;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.logiench.shardLib.api.register.attribute.AttributeDefinitionRegister;
import net.logiench.shardLib.api.register.player.PlayerRegister;
import net.logiench.shardLib.di.annotations.PlayerAttribute;

@Singleton
public class PlayerRegisterImpl implements PlayerRegister {
	private final AttributeDefinitionRegister attributeRegister;

	@Inject
	public PlayerRegisterImpl(@PlayerAttribute AttributeDefinitionRegister attributeRegister) {
		this.attributeRegister = attributeRegister;
	}

	@Override
	public AttributeDefinitionRegister attributes() {
		return attributeRegister;
	}
}
