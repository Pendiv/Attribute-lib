package net.logiench.shardCore.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.logiench.shardCore.core._party.PartyProvider;

import java.util.stream.Stream;

public class PartyCommand {
	public PartyCommand(PartyProvider partyProvider) {
		this.partyProvider = partyProvider;
	}

	private final PartyProvider partyProvider;

	public LiteralCommandNode<CommandSourceStack> getCommand() {
		return Commands.literal("party") // 1. コマンド名 (ルート)
			.executes(ctx -> {
				// /mycommand が実行された時の処理
				ctx.getSource().getSender().sendMessage("基本コマンドが実行されました！");
				return 1; // 成功を示す
			})
			.then(Commands.literal("create") // 2. サブコマンドの定義 (/mycommand sub)
				.then(Commands.argument("visibility", StringArgumentType.word()) // 3. 引数の定義 (/mycommand sub <message>)
					.suggests((ctx, builder) -> {
						String remaining = builder.getRemainingLowerCase();
						Stream.of("public", "private").filter(option -> option.startsWith(remaining)).forEach(builder::suggest);
						return builder.buildFuture();
					})
					.executes(ctx -> {
						String visibility = ctx.getArgument("visibility", String.class);
						ctx.getSource().getSender().sendMessage("メッセージ: " + visibility);
						return 1;
					})
				)
			)
			.build();
	}

	/*@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
		if (!(sender instanceof Player p)) {
			sender.sendMessage(ChatColor.RED + "Only players can execute this command.");
			return true;
		}
		UUID uuid = p.getUniqueId();

		// /"give" ["@s", "stone", "1"]
		*//* /party [ //パーティーリーダーのみが行える
		invite, name
		remove, name
		assign, name

		/party [ //全員が行える
		leave, 			引数いらん
		join, name  	引数いる
		]
		] *//*

		if (args.length == 0) {
			return false;
		}
		int length = args.length;
		switch (args[0]) {
			case "leave" -> {

			}
			case "create" -> {
				if (length > 1) {
					Boolean isPrivate = switch (args[1]) {
						case "private" -> true;
						case "public" -> false;
						default -> null;
					};
					if (isPrivate != null) {
						if (!partyProvider.partyExists(uuid)) {
							partyProvider.createParty(uuid, isPrivate);
						} else {
							p.sendMessage(ChatColor.RED + "Already hosting a party.");
						}
						return true;
					}
				}
				p.sendMessage(ChatColor.RED + "Invalid arguments.");
				return false;
			}
			case "disband" -> {//解散

			}
			default -> {
				if (args.length < 2) {
					return false;
				}
				switch (args[0]) {
					case "remove", "assign":
						Player target = Bukkit.getPlayer(args[1]);
						if (target == null) {
							return true;
						}
						partyProvider.addMember(uuid, target.getUniqueId());
						break;
					case "invite":
						//if 招待権限を持つかチェック
					case "join":
						break;
				}
			}
		}


		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {


		return List.of();
	}*/
}
