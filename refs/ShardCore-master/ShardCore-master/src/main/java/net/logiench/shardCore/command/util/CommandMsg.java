package net.logiench.shardCore.command.util;

import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandMsg {
	public static void sendMessage(CommandContext<CommandSourceStack> ctx, Component message) {
		ctx.getSource().getSender().sendMessage(message);
	}

	public static void sendMessage(CommandContext<CommandSourceStack> ctx, String msg, TextColor color) {
		sendMessage(ctx, Component.text(msg, color));
	}

	public static void sendMessageAndLog(CommandContext<CommandSourceStack> ctx, String msg) {
		CommandSender sender = ctx.getSource().getSender();
		sender.sendMessage(Component.text(msg, NamedTextColor.LIGHT_PURPLE));
		Component log = Component.text("[%s]".formatted(sender.getName()), NamedTextColor.GRAY)
			.appendSpace().append(Component.text(msg));

		for (Player player : Bukkit.getOnlinePlayers()) {
			if (!player.isOp() || player.equals(sender)) {
				continue;
			}
			player.sendMessage(log);
		}
	}
}
