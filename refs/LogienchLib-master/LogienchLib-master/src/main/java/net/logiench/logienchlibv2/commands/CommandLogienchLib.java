package net.logiench.logienchlibv2.commands;

import net.logiench.logienchlibv2.ConfigState;
import net.logiench.logienchlibv2.api.minecraft.text.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CommandLogienchLib implements CommandExecutor, TabCompleter {
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (args.length == 0) {
			return false;
		}
		switch (args[0].toLowerCase()) {
			case "config-reload" -> {
				ConfigState.loadConfig();
				sender.sendMessage(ChatColor.GREEN + "Config reloaded!");
			}
			default -> {
				sender.sendMessage(ChatColor.RED + "Unknown command!");
				return true;
			}
		}
		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (args.length == 1) {
			return Filter.filter(List.of("config-reload"), args[0]);
		}
		return null;
	}
}
