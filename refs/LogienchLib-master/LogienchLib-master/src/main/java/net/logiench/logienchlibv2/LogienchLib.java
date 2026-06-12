package net.logiench.logienchlibv2;

import com.comphenix.protocol.ProtocolLibrary;
import net.logiench.logienchlibv2.base.database.AutoCloser;
import net.logiench.logienchlibv2.commands.CommandLogienchLib;
import net.logiench.logienchlibv2.listener.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public final class LogienchLib extends JavaPlugin {

	public static final Logger log = LoggerFactory.getLogger(LogienchLib.class);
	private static LogienchLib plugin = null;

	@Override
	public void onLoad() {
		plugin = this;

	}

	@Override
	public void onEnable() {
		ConfigState.loadConfig();

		getServer().getPluginManager().registerEvents(new InvMenuListener(), this);
		getServer().getPluginManager().registerEvents(new PlayerDropSlotListener(), this);
		getServer().getPluginManager().registerEvents(new PlayerLoginOutListener(), this);
		getServer().getPluginManager().registerEvents(new InventoryItemListener(), this);

		ProtocolLibrary.getProtocolManager().addPacketListener(new AnvilMenuPacket());

		Objects.requireNonNull(getCommand("logienchlib")).setExecutor(new CommandLogienchLib());
	}

	@Override
	public void onDisable() {
		AutoCloser.closeAll();
	}

	public static LogienchLib getInstance() {
		return plugin;
	}
}
