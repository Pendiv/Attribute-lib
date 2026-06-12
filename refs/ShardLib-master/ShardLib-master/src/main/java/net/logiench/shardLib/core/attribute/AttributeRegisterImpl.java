package net.logiench.shardLib.core.attribute;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.logiench.shardLib.api.register.attribute.AttributeDefinitionRegister;
import net.logiench.shardLib.api.register.attribute.AttributeRegister;
import net.logiench.shardLib.api.register.attribute.AttributeValueProviderRegister;
import net.logiench.shardLib.di.annotations.CoreAttribute;

@Singleton
public class AttributeRegisterImpl implements AttributeRegister {
	private final AttributeDefinitionRegister attributeDefinitionRegister;
	private final AttributeValueProviderRegister attributeValueProviderRegister;

	@Inject
	public AttributeRegisterImpl(@CoreAttribute AttributeDefinitionRegister attributeDefinitionRegister, AttributeValueProviderRegister attributeValueProviderRegister) {
		this.attributeDefinitionRegister = attributeDefinitionRegister;
		this.attributeValueProviderRegister = attributeValueProviderRegister;
	}

	@Override
	public AttributeDefinitionRegister coreAttribute() {
		return attributeDefinitionRegister;
	}

	@Override
	public AttributeValueProviderRegister valueProvider() {
		return attributeValueProviderRegister;
	}
}
