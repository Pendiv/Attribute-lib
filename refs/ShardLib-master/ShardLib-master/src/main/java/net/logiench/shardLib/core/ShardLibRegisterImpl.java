package net.logiench.shardLib.core;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.logiench.shardLib.api.register.ShardLibRegister;
import net.logiench.shardLib.api.register.attribute.AttributeRegister;
import net.logiench.shardLib.api.register.mob.MobRegister;
import net.logiench.shardLib.api.register.player.PlayerRegister;

@Singleton
public class ShardLibRegisterImpl implements ShardLibRegister {
	private final PlayerRegister player;
	private final MobRegister mob;
	private final AttributeRegister attribute;

	@Inject
	public ShardLibRegisterImpl(PlayerRegister player, MobRegister mob, AttributeRegister attribute) {
		this.player = player;
		this.mob = mob;
		this.attribute = attribute;
	}

	@Override
	public PlayerRegister player() {
		return player;
	}

	@Override
	public MobRegister mob() {
		return mob;
	}

	@Override
	public AttributeRegister attribute() {
		return attribute;
	}
}
