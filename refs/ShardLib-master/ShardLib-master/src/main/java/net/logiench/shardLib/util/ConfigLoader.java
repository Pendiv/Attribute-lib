package net.logiench.shardLib.util;

import com.google.inject.Injector;
import net.logiench.shardLib.core.item.ItemFactory;
import net.logiench.shardLib.core.mob.MobCharacterManager;
import net.logiench.shardLib.core.player.PlayerCharacterManager;
import net.logiench.shardLib.database.DatabaseManager;
import net.logiench.shardLib.util.loader.DaoSet;
import net.logiench.shardLib.util.loader.LoadStrategy;
import net.logiench.shardLib.util.loader.RegistrySet;

public class ConfigLoader {
	private final DatabaseManager databaseManager;
	private final RegistrySet registrySet;
	private final DaoSet daoSet;
	private final ItemFactory itemFactory;
	private final MobCharacterManager mobCharacterManager;
	private final PlayerCharacterManager playerCharacterManager;

	public ConfigLoader(Injector injector) {
		this.databaseManager = injector.getInstance(DatabaseManager.class);
		this.registrySet = injector.getInstance(RegistrySet.class);
		this.daoSet = injector.getInstance(DaoSet.class);
		this.itemFactory = injector.getInstance(ItemFactory.class);
		this.mobCharacterManager = injector.getInstance(MobCharacterManager.class);
		this.playerCharacterManager = injector.getInstance(PlayerCharacterManager.class);
	}

	public boolean run(LoadStrategy strategy) {
		return strategy.execute(this);
	}

	public RegistrySet registrySet() {
		return registrySet;
	}

	public DaoSet daoSet() {
		return daoSet;
	}

	public ItemFactory itemFactory() {
		return itemFactory;
	}

	public MobCharacterManager mobCharacterManager() {
		return mobCharacterManager;
	}

	public PlayerCharacterManager playerCharacterManager() {
		return playerCharacterManager;
	}

	public DatabaseManager databaseManager() {
		return databaseManager;
	}
}
