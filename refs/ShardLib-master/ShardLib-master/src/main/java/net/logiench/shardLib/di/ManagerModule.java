package net.logiench.shardLib.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import net.logiench.shardLib.core.attribute.AttributeDefinitionRegisterImpl;
import net.logiench.shardLib.core.attribute.AttributeManager;
import net.logiench.shardLib.di.annotations.MobAttributeManager;
import net.logiench.shardLib.di.annotations.PlayerAttribute;
import net.logiench.shardLib.di.annotations.PlayerAttributeManager;

public class ManagerModule extends AbstractModule {
	@Provides
	@Singleton
	@PlayerAttributeManager
	public AttributeManager providePlayerAttributeManager(
		@PlayerAttribute AttributeDefinitionRegisterImpl playerAttributeRegister
	) {
		return new AttributeManager(playerAttributeRegister);
	}

	@Provides
	@Singleton
	@MobAttributeManager
	public AttributeManager provideMobAttributeManager(
		@PlayerAttribute AttributeDefinitionRegisterImpl mobAttributeRegister
	) {
		return new AttributeManager(mobAttributeRegister);
	}
}
