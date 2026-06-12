package net.logiench.shardCore.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ShardCommand {
	LiteralCommandNode<CommandSourceStack> builder(LiteralArgumentBuilder<CommandSourceStack> commandBuilder);

	@NotNull
	String getName();

	@Nullable
	String getDescription();

	List<String> getAliases();
}
