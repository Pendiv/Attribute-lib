package net.logiench.shardLib.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import net.logiench.shardLib.api.register.attribute.AttributeDefinitionRegister;
import net.logiench.shardLib.api.register.attribute.AttributeRegister;
import net.logiench.shardLib.api.register.attribute.AttributeValueProviderRegister;
import net.logiench.shardLib.api.register.mob.MobAttributeRegister;
import net.logiench.shardLib.api.register.mob.MobRegister;
import net.logiench.shardLib.api.register.player.PlayerRegister;
import net.logiench.shardLib.core.attribute.AttributeDefinitionRegisterImpl;
import net.logiench.shardLib.core.attribute.AttributeRegisterImpl;
import net.logiench.shardLib.core.attribute.AttributeValueProviderRegisterImpl;
import net.logiench.shardLib.core.mob.MobAttributeRegisterImpl;
import net.logiench.shardLib.core.mob.MobRegisterImpl;
import net.logiench.shardLib.core.player.PlayerRegisterImpl;
import net.logiench.shardLib.di.annotations.CoreAttribute;
import net.logiench.shardLib.di.annotations.MobAttribute;
import net.logiench.shardLib.di.annotations.MobCoreAttribute;
import net.logiench.shardLib.di.annotations.PlayerAttribute;

public class RegisterModule extends AbstractModule {
	@Override
	protected void configure() {
		// registerリンクまとめ
		bind(MobRegister.class)
			.to(MobRegisterImpl.class);

		bind(AttributeRegister.class)
			.to(AttributeRegisterImpl.class);

		bind(PlayerRegister.class)
			.to(PlayerRegisterImpl.class);

		// 各種register
		// item

		// mob
		bind(MobAttributeRegister.class)
			.to(MobAttributeRegisterImpl.class);

		// attribute
		bind(AttributeDefinitionRegister.class)
			.to(AttributeDefinitionRegisterImpl.class);


		bind(AttributeValueProviderRegister.class)
			.to(AttributeValueProviderRegisterImpl.class);
	}

	@Provides
	@Singleton
	@CoreAttribute
	public AttributeDefinitionRegister provideCoreAttributeRegister(
		@CoreAttribute AttributeDefinitionRegisterImpl impl
	) {
		return impl;
	}

	@Provides
	@Singleton
	@CoreAttribute // このメソッドは「@CoreAttribute」ラベルの付いたインスタンスを提供する
	public AttributeDefinitionRegisterImpl provideCoreAttributeRegisterImpl() {
		// 親がいないルートのレジスタ
		return new AttributeDefinitionRegisterImpl(null);
	}

	@Provides
	@Singleton
	@PlayerAttribute
	public AttributeDefinitionRegister providePlayerAttributeRegister(
		@PlayerAttribute AttributeDefinitionRegisterImpl impl
	) {
		return impl;
	}

	@Provides
	@Singleton
	@PlayerAttribute
	public AttributeDefinitionRegisterImpl providePlayerAttributeRegisterImpl(
		@CoreAttribute AttributeDefinitionRegisterImpl coreRegister
	) {
		return new AttributeDefinitionRegisterImpl(coreRegister);
	}

	@Provides
	@Singleton
	@MobCoreAttribute
	public AttributeDefinitionRegisterImpl provideMobCoreAttributeRegister(
		@CoreAttribute AttributeDefinitionRegisterImpl coreRegister
	) {
		return new AttributeDefinitionRegisterImpl(coreRegister);
	}

	@Provides
	@MobAttribute
	public AttributeDefinitionRegisterImpl provideMobAttributeRegister(
		@MobCoreAttribute AttributeDefinitionRegisterImpl mobCoreRegister
	) {
		return new AttributeDefinitionRegisterImpl(mobCoreRegister);
	}
}
