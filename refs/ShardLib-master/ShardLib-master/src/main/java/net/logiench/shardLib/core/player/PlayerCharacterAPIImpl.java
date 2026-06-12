package net.logiench.shardLib.core.player;


import com.google.inject.Singleton;
import net.logiench.shardLib.api.data.CustomDataContainerAPI;
import net.logiench.shardLib.api.player.PlayerCharacterAPI;
import net.logiench.shardLib.core.attribute.PlayerAttributeAPIImpl;
import net.logiench.shardLib.core.data.CustomDataContainerAPIImpl;
import net.logiench.shardLib.core.data.CustomDataKey;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Singleton
public class PlayerCharacterAPIImpl implements PlayerCharacterAPI {
	private final UUID uuid;
	private final PlayerAttributeAPIImpl attributeAPI;
	private CustomDataContainerAPI customData;

	public PlayerCharacterAPIImpl(UUID uuid, PlayerAttributeAPIImpl attributeAPI, CustomDataContainerAPI customData) {
		this.uuid = uuid;
		this.attributeAPI = attributeAPI;
		this.customData = customData;
	}

	public UUID getUUID() {
		return uuid;
	}

	@Override
	@NotNull
	public PlayerAttributeAPIImpl getAttributeAPI() {
		return attributeAPI;
	}

	@Override
	@NotNull
	public CustomDataContainerAPI getPersistentData() {
		return customData.clone();
	}

	@Override
	public void setPersistentData(@NotNull CustomDataContainerAPI customData) {
		if (!(customData instanceof CustomDataContainerAPIImpl dataContainerImpl)) {
			throw new IllegalArgumentException("CustomDataContainerAPI is not cannot set PersistentData");
		}
		this.customData = customData;
		Player player = Bukkit.getPlayer(uuid);
		if (player == null) {
			return;
		}
		player.getPersistentDataContainer().set(CustomDataKey.CUSTOM_DATA, PersistentDataType.STRING, dataContainerImpl.toGson());

	}
}
