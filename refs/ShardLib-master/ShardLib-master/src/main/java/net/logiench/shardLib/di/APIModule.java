package net.logiench.shardLib.di;

import com.google.inject.AbstractModule;
import net.logiench.shardLib.ShardLibAPIImpl;
import net.logiench.shardLib.api.ShardLibAPI;
import net.logiench.shardLib.api.ShardLibProvider;
import net.logiench.shardLib.api.item.ItemAPI;
import net.logiench.shardLib.api.mob.MobAPI;
import net.logiench.shardLib.api.player.PlayerAPI;
import net.logiench.shardLib.api.register.ShardLibRegister;
import net.logiench.shardLib.core.ShardLibRegisterImpl;
import net.logiench.shardLib.core.item.ItemAPIImpl;
import net.logiench.shardLib.core.mob.MobAPIImpl;
import net.logiench.shardLib.core.player.PlayerAPIImpl;

public class APIModule extends AbstractModule {
	@Override
	protected void configure() {
		// APIの窓口の初期化を強制
		// これによりinjectorが作成された時点でAPIの提供が開始される
		bind(ShardLibProvider.class)
			.asEagerSingleton();

		// API本体
		bind(ShardLibAPI.class)
			.to(ShardLibAPIImpl.class);

		// APIの各種機能
		bind(PlayerAPI.class)
			.to(PlayerAPIImpl.class);

		bind(MobAPI.class)
			.to(MobAPIImpl.class);

		bind(ItemAPI.class)
			.to(ItemAPIImpl.class);

		bind(ShardLibRegister.class)
			.to(ShardLibRegisterImpl.class);
	}
}
