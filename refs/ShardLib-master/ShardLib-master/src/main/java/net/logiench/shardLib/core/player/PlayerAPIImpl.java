package net.logiench.shardLib.core.player;

import com.google.inject.Inject;
import net.logiench.shardLib.api.player.PlayerAPI;
import net.logiench.shardLib.api.player.PlayerCharacterAPI;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public class PlayerAPIImpl implements PlayerAPI {
	private final PlayerCharacterManager playerCharacterManager;

	@Inject
	public PlayerAPIImpl(PlayerCharacterManager playerCharacterManager) {
		this.playerCharacterManager = playerCharacterManager;
	}

	@Override
	@NotNull
	public Optional<PlayerCharacterAPI> getCharacterAPI(@NotNull Player player) {
		return playerCharacterManager.getCharacter(player.getUniqueId());
	}

	@Override
	@NotNull
	public Optional<PlayerCharacterAPI> getCharacterAPI(UUID uuid) {
		return playerCharacterManager.getCharacter(uuid);
	}
}
