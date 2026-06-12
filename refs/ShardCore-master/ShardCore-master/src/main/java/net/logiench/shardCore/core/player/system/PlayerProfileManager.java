package net.logiench.shardCore.core.player.system;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Singleton
public class PlayerProfileManager {

	private final Map<Integer, PlayerProfile> profiles = new HashMap<>();

	@Inject
	private PlayerProfileManager() {
	}

	public void unloadProfile(UUID playerId) {

	}

	public void loadProfile(UUID playerId) {

	}
}
