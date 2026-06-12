package net.logiench.shardCore.di;

import com.google.inject.AbstractModule;
import net.logiench.shardCore.config.data.DatabaseConfigState;
import net.logiench.shardCore.config.data.LimboPlayerConfigState;

/**
 * コンフィグのロードタイミングを合わせるため、ここで全て指定する
 */
public class ConfigModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(DatabaseConfigState.class).asEagerSingleton();
		bind(LimboPlayerConfigState.class).asEagerSingleton();
	}
}
