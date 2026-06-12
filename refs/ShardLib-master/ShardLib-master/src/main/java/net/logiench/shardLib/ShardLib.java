package net.logiench.shardLib;

import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
import net.logiench.shardLib.api.ShardLibAPI;
import net.logiench.shardLib.command.CommandShardLib;
import net.logiench.shardLib.di.APIModule;
import net.logiench.shardLib.di.DaoModule;
import net.logiench.shardLib.di.ManagerModule;
import net.logiench.shardLib.di.RegisterModule;
import net.logiench.shardLib.listener.EventListener;
import net.logiench.shardLib.util.ConfigLoader;
import net.logiench.shardLib.util.loader.ReloadRunStrategy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class ShardLib extends JavaPlugin {
	private static ShardLib instance;
	private final static Gson GSON = new Gson();
	private ConfigLoader loader;

	@Override
	public void onLoad() {
		instance = this;
		Injector injector = Guice.createInjector(
			new RegisterModule(),
			new ManagerModule(),
			new APIModule(),
			new DaoModule()
		);
		loader = new ConfigLoader(injector);

		// ServicesManager経由方式のAPIを提供
		Bukkit.getServicesManager().register(
			ShardLibAPI.class,
			injector.getInstance(ShardLibAPI.class),
			ShardLib.getInstance(),
			ServicePriority.Normal
		);
	}

	@Override
	public void onEnable() {
		Objects.requireNonNull(getCommand("shardlib")).setExecutor(new CommandShardLib(loader));

		getServer().getPluginManager().registerEvents(new EventListener(loader), this);
		getServer().getPluginManager().registerEvents(loader.playerCharacterManager(), this);
		getServer().getPluginManager().registerEvents(loader.mobCharacterManager(), this);
	}

	@Override
	public void onDisable() {
		CompletableFuture<Void> completable = loader.playerCharacterManager().saveAllPlayers();
		loader.mobCharacterManager().saveAllCharacters();
		completable.join();
		loader.databaseManager().closeConnection();
	}

	// メインクラスもDI対応させたいが、そこまですると汎用性が下がってしまうのでこれだけ残す
	public static ShardLib getInstance() {
		return instance;
	}

	public static Gson getGson() {
		return GSON;
	}

	public void reload() {
		loader.run(new ReloadRunStrategy());
	}
}
