package net.logiench.shardCore.di;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import net.logiench.shardCore.core.loot.system.LootItemGenerateProvider;

public class FactoryModule extends AbstractModule {

	@Override
	protected void configure() {
		install(new FactoryModuleBuilder()
			.implement(LootItemGenerateProvider.class, LootItemGenerateProvider.class)
			.build(LootItemGenerateProvider.Factory.class));
	}
}
