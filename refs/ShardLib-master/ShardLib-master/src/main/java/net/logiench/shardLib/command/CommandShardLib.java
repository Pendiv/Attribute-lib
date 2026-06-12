package net.logiench.shardLib.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.logiench.logienchlibv2.api.minecraft.text.ChatColor;
import net.logiench.shardLib.util.ConfigLoader;
import net.logiench.shardLib.util.loader.AbstractLoadStrategy;
import net.logiench.shardLib.util.loader.ReloadRunStrategy;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

public class CommandShardLib implements CommandExecutor, TabCompleter {
	private final ConfigLoader loader;

	public CommandShardLib(ConfigLoader loader) {
		this.loader = loader;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (args.length != 1) {
			return false;
		}

		switch (args[0]) {
			case "config-reload" -> {
				AbstractLoadStrategy strategy = new ReloadRunStrategy();

				if (loader.run(strategy)) {
					strategy.getMessage().ifPresent(message ->
						sender.sendMessage(Component.text(message)
							.color(ChatColor.GREEN.color())
							.hoverEvent(getTimeHover())
						)
					);
				} else {
					sender.sendMessage(Component.text("Config could not be reloaded").color(ChatColor.RED.color()));
					strategy.getMessage().ifPresent(message ->
						sender.sendMessage(Component.text("- " + message)
							.color(ChatColor.GRAY.color())
							.hoverEvent(getTimeHover())
						)
					);
				}
			}
		}
		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (args.length == 1) {
			return Stream.of("config-reload").filter(s -> s.contains(args[0])).toList();
		}
		return List.of();
	}

	private HoverEvent<Component> getTimeHover() {
		return HoverEvent.hoverEvent(
			HoverEvent.Action.SHOW_TEXT,
			Component.text(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd HH:mm:ss")))
		);
	}
}
