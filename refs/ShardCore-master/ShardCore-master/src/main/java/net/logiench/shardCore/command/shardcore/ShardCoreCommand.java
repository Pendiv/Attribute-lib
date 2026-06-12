package net.logiench.shardCore.command.shardcore;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.logiench.shardCore.command.ShardCommand;
import net.logiench.shardCore.core.menu.MenuStateManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Singleton
public class ShardCoreCommand implements ShardCommand {
	private final MenuStateManager menuStateManager;

	@Inject
	private ShardCoreCommand(MenuStateManager menuStateManager) {
		this.menuStateManager = menuStateManager;
	}

	@Override
	public LiteralCommandNode<CommandSourceStack> builder(LiteralArgumentBuilder<CommandSourceStack> commandBuilder) {
		return commandBuilder
			.requires(source -> source.getSender().isOp())
			// shardcore menu を作成
			.then(new MenuSubCommand(menuStateManager).build())

			.build();
	}

	@Override
	public @NotNull String getName() {
		return "shardcore";
	}

	@Override
	public @Nullable String getDescription() {
		return "ShardCore管理用コマンド";
	}

	@Override
	public List<String> getAliases() {
		return List.of("sc");
	}
}
