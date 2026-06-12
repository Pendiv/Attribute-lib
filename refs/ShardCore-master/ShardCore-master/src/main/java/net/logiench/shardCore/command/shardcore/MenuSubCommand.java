package net.logiench.shardCore.command.shardcore;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import net.kyori.adventure.text.Component;
import net.logiench.shardCore.command.util.CommandMsg;
import net.logiench.shardCore.core.menu.MenuStateManager;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

public class MenuSubCommand {
	private static final DynamicCommandExceptionType ERROR_NOT_FIND_CLASS = new DynamicCommandExceptionType(name ->
		MessageComponentSerializer.message().serialize(Component.text(name + " は存在しません")));

	private final MenuStateManager menuStateManager;

	MenuSubCommand(MenuStateManager menuStateManager) {
		this.menuStateManager = menuStateManager;
	}

	ArgumentBuilder<CommandSourceStack, ?> build() {
		return Commands.literal("menu")
			.then(Commands.argument("state", new MenuStateArgumentType())
				.then(Commands.argument("className", new MenuNameArgumentType())
					.executes(ctx -> {
						boolean toLock = ctx.getArgument("state", Boolean.class);
						String name = ctx.getArgument("className", String.class);
						// 全てを同時処理
						if (name.equals("ALL")) {
							if (toLock) {
								menuStateManager.getEnabledMenus().forEach(menuStateManager::disable);
								sendLockMessage(ctx, "全てのメニュー");
							} else {
								menuStateManager.getDisabledMenus().forEach(menuStateManager::enable);
								sendUnlockMessage(ctx, "全てのメニュー");
							}
							return Command.SINGLE_SUCCESS;
						}

						if (toLock && menuStateManager.getEnabledMenus().contains(name)) {
							menuStateManager.disable(name);
							sendLockMessage(ctx, name);
							return Command.SINGLE_SUCCESS;
						}
						if (!toLock && menuStateManager.getDisabledMenus().contains(name)) {
							menuStateManager.enable(name);
							sendUnlockMessage(ctx, name);
							return Command.SINGLE_SUCCESS;
						}
						throw ERROR_NOT_FIND_CLASS.create(name);
					})
				)
			);
	}

	private void sendLockMessage(CommandContext<CommandSourceStack> ctx, String target) {
		CommandMsg.sendMessageAndLog(ctx, target + " の使用を制限しました");
	}

	private void sendUnlockMessage(CommandContext<CommandSourceStack> ctx, String target) {
		CommandMsg.sendMessageAndLog(ctx, target + " の制限を解除しました");
	}

	class MenuStateArgumentType implements CustomArgumentType<Boolean, String> {
		private static final String toLockKey = "lock";
		private static final String toUnlockKey = "unlock";

		private static final DynamicCommandExceptionType ERROR_INVALID_STATE = new DynamicCommandExceptionType(name ->
			MessageComponentSerializer.message().serialize(Component.text(name + " は対象がないため使用できません")));

		@Override
		public Boolean parse(StringReader reader) throws CommandSyntaxException {
			String input = reader.readUnquotedString().toLowerCase();
			if (input.equals(toLockKey) && menuStateManager.hasEnabledMenu()) {
				return true;
			}
			if (input.equals(toUnlockKey) && menuStateManager.hasDisabledMenu()) {
				return false;
			}
			throw ERROR_INVALID_STATE.create(input);
		}

		@Override
		public @NonNull ArgumentType<String> getNativeType() {
			return StringArgumentType.string();
		}

		@Override
		public <S> @NonNull CompletableFuture<Suggestions> listSuggestions(@NonNull CommandContext<S> context, @NonNull SuggestionsBuilder builder) {
			String input = builder.getRemainingLowerCase();
			if (toLockKey.startsWith(input) && menuStateManager.hasEnabledMenu()) {
				builder.suggest(toLockKey, MessageComponentSerializer.message().serialize(Component.text("メニューをロックします")));
			}
			if (toUnlockKey.startsWith(input) && menuStateManager.hasDisabledMenu()) {
				builder.suggest(toUnlockKey, MessageComponentSerializer.message().serialize(Component.text("メニューをアンロックします")));
			}
			return builder.buildFuture();
		}
	}

	class MenuNameArgumentType implements CustomArgumentType<String, String> {
		@Override
		public String parse(@NonNull StringReader reader) {
			return reader.readUnquotedString();
		}

		@Override
		public @NonNull ArgumentType<String> getNativeType() {
			return StringArgumentType.string();
		}

		@Override
		public <S> @NonNull CompletableFuture<Suggestions> listSuggestions(@NonNull CommandContext<S> context, @NonNull SuggestionsBuilder builder) {
			boolean toLock = context.getArgument("state", Boolean.class);
			String remaining = builder.getRemaining().toLowerCase();
			for (String className : toLock ? menuStateManager.getEnabledMenus() : menuStateManager.getDisabledMenus()) {
				if (className.toLowerCase().startsWith(remaining)) {
					builder.suggest(className);
				}
			}
			if ("all".startsWith(remaining)) {
				builder.suggest("ALL", MessageComponentSerializer.message().serialize(Component.text("すべてのメニューに対して設定します")));
			}
			return builder.buildFuture();
		}
	}
}
