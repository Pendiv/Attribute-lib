package net.logiench.shardCore.core.item.base.module;

import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import net.logiench.shardCore.core.item.base.module.tools.StructuredLore;
import net.logiench.shardLib.api.player.PlayerCharacterAPI;
import org.bukkit.entity.Player;

public interface DynamicLoreUpdatable {

	void updateDynamicLore(StructuredLore lore, SuperItemStack item, PlayerCharacterAPI characterAPI, Player player);
}
