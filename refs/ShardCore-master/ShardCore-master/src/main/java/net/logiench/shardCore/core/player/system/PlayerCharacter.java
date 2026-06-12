package net.logiench.shardCore.core.player.system;

import net.logiench.shardLib.api.player.PlayerAttributeAPI;
import net.logiench.shardLib.api.player.PlayerCharacterAPI;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * 一時的なプレイヤーのデータ共有用インスタンス
 */
public record PlayerCharacter(
	Player player, PlayerCharacterAPI characterAPI
) {
	public UUID getUniqueId() {
		return player.getUniqueId();
	}

	public PlayerAttributeAPI attributeAPI() {
		return characterAPI.getAttributeAPI();
	}
}
