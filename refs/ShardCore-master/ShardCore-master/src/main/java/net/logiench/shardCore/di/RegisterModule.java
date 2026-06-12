package net.logiench.shardCore.di;

import com.google.inject.AbstractModule;
import net.logiench.shardCore.core.menu.MenuStateManager;
import net.logiench.shardCore.register.*;

public class RegisterModule extends AbstractModule {

	@Override
	protected void configure() {
		// 強制的にインスタンス化させる
		bind(ItemRegistry.class).asEagerSingleton();
		bind(GemRegistry.class).asEagerSingleton();
		bind(StatsRegistry.class).asEagerSingleton();
		bind(MobRegistry.class).asEagerSingleton();
		bind(MobLootTableRegistry.class).asEagerSingleton();
		bind(PrefixRegistry.class).asEagerSingleton();
		bind(RequirementRegistry.class).asEagerSingleton();
		bind(ModuleRegistry.class).asEagerSingleton();
		bind(SkillRegistry.class).asEagerSingleton();

		// ファイルの読み込みがあるので事前に
		bind(MenuStateManager.class).asEagerSingleton();
	}
}
