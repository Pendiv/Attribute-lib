package net.logiench.shardCore.core.item.system.gem.context;

import lombok.Getter;
import net.logiench.shardCore.core.player.system.PlayerCharacter;

@Getter
public class GemContext {

	private final PlayerCharacter character;

	public GemContext(PlayerCharacter character) {
		this.character = character;
	}
}
