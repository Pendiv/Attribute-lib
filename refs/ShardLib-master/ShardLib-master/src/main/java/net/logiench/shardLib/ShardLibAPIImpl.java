package net.logiench.shardLib;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.logiench.shardLib.api.ShardLibAPI;
import net.logiench.shardLib.api.item.ItemAPI;
import net.logiench.shardLib.api.mob.MobAPI;
import net.logiench.shardLib.api.player.PlayerAPI;
import net.logiench.shardLib.api.register.ShardLibRegister;
import org.jetbrains.annotations.NotNull;

@Singleton
public class ShardLibAPIImpl implements ShardLibAPI {
	private final ItemAPI itemAPI;
	private final PlayerAPI playerAPI;
	private final MobAPI mobAPI;
	private final ShardLibRegister shardLibRegister;

	@Inject
	public ShardLibAPIImpl(ItemAPI itemAPI, PlayerAPI playerAPI, MobAPI mobAPI, ShardLibRegister shardLibRegister) {
		this.itemAPI = itemAPI;
		this.playerAPI = playerAPI;
		this.mobAPI = mobAPI;
		this.shardLibRegister = shardLibRegister;
	}

	@Override
	@NotNull
	public PlayerAPI getPlayerAPI() {
		return playerAPI;
	}

	@Override
	@NotNull
	public MobAPI getMobAPI() {
		return mobAPI;
	}

	@Override
	@NotNull
	public ItemAPI getItemAPI() {
		return itemAPI;
	}

	@Override
	@NotNull
	public ShardLibRegister getRegister() {
		return shardLibRegister;
	}
}
