package net.logiench.shardLib.core.player;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.logiench.shardLib.api.attribute.data.AttributeModifier;
import net.logiench.shardLib.api.attribute.data.AttributeOperationModifier;
import net.logiench.shardLib.api.attribute.data.AttributeValueProvider;
import net.logiench.shardLib.api.player.PlayerAPI;
import net.logiench.shardLib.api.player.PlayerCharacterAPI;
import net.logiench.shardLib.api.register.attribute.AttributeDefinitionRegister;
import net.logiench.shardLib.core.attribute.AttributeManager;
import net.logiench.shardLib.core.attribute.PlayerAttributeAPIImpl;
import net.logiench.shardLib.core.data.CustomDataContainerAPIImpl;
import net.logiench.shardLib.core.data.CustomDataKey;
import net.logiench.shardLib.database.dao.PlayerData;
import net.logiench.shardLib.database.dao.SqlPlayerDataDAO;
import net.logiench.shardLib.di.annotations.PlayerAttribute;
import net.logiench.shardLib.di.annotations.PlayerAttributeManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Singleton
public class PlayerCharacterManager implements Listener {
	private final Map<UUID, PlayerCharacterAPIImpl> characters = new HashMap<>();
	private final AttributeManager attributeManager;
	private final SqlPlayerDataDAO sqlPlayerDataDAO;
	private final AttributeDefinitionRegister register;
	@Inject
	private PlayerAPI playerAPI;

	@Inject
	public PlayerCharacterManager(@PlayerAttributeManager AttributeManager attributeManager, @PlayerAttribute AttributeDefinitionRegister register, SqlPlayerDataDAO sqlPlayerDataDAO) {
		this.attributeManager = attributeManager;
		this.sqlPlayerDataDAO = sqlPlayerDataDAO;
		this.register = register;
	}

	public Optional<PlayerCharacterAPI> getCharacter(UUID uuid) {
		return Optional.ofNullable(characters.get(uuid));
	}

	@EventHandler(priority = EventPriority.LOWEST)
	private void onPlayerJoin(PlayerJoinEvent ev) {
		Player player = ev.getPlayer();
		UUID uuid = player.getUniqueId();
		PlayerAttributeAPIImpl playerAttributeAPI = new PlayerAttributeAPIImpl(uuid, attributeManager, register, playerAPI);
		characters.put(uuid, new PlayerCharacterAPIImpl(uuid,
			playerAttributeAPI,
			CustomDataContainerAPIImpl.fromGson(
				player.getPersistentDataContainer().get(
					CustomDataKey.CUSTOM_DATA,
					PersistentDataType.STRING
				)
			).orElseGet(CustomDataContainerAPIImpl::new)
		));

		sqlPlayerDataDAO.loadPlayerData(uuid).thenAccept(data ->
			data.ifPresent(attributes -> {
				playerAttributeAPI.setBaseAttributes(attributes.baseAttributes());
				attributes.modifiers().forEach(playerAttributeAPI::addModifier);
				attributes.providers().forEach(playerAttributeAPI::addProvider);
				playerAttributeAPI.getModifierInstanceIds().putAll(attributes.modifierInstanceIds());

				playerAttributeAPI.recalculateStats();
			})
		);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	private void onPlayerQuit(PlayerQuitEvent ev) {
		UUID uuid = ev.getPlayer().getUniqueId();
		PlayerCharacterAPIImpl playerCharacterAPI = characters.remove(uuid);
		if (playerCharacterAPI != null) {
			sqlPlayerDataDAO.savePlayerData(toPlayerData(uuid, playerCharacterAPI));
		}
	}

	public CompletableFuture<Void> saveAllPlayers() {
		return CompletableFuture.runAsync(() -> {
			List<Long> deleteModifierInstanceIds = new ArrayList<>();
			List<Long> deleteProviderInstanceIds = new ArrayList<>();
			for (PlayerCharacterAPIImpl character : characters.values()) {
				PlayerAttributeAPIImpl playerAttributeAPI = character.getAttributeAPI();
				deleteModifierInstanceIds.addAll(playerAttributeAPI.getDeleteModifierInstanceId());
				deleteProviderInstanceIds.addAll(playerAttributeAPI.getDeleteProviderInstanceId());
			}
			CompletableFuture<Void> delete = sqlPlayerDataDAO.deletePlayerModifier(deleteModifierInstanceIds, deleteProviderInstanceIds);
			sqlPlayerDataDAO.savePlayerData(
				characters.entrySet().stream()
					.map(e -> toPlayerData(
						e.getKey(), e.getValue()
					)).toArray(PlayerData[]::new)
			).thenAccept(data -> {
				if (data == null) {
					return;
				}
				for (Map.Entry<UUID, Map<AttributeOperationModifier, Long>> entry : data.entrySet()) {
					PlayerCharacterAPIImpl characterAPI = characters.get(entry.getKey());
					characterAPI.getAttributeAPI().getModifierInstanceIds().putAll(entry.getValue());
				}
			}).join();
			delete.join();
		});
	}

	private PlayerData toPlayerData(UUID uuid, PlayerCharacterAPIImpl playerCharacterAPI) {
		PlayerAttributeAPIImpl playerAttributeAPI = playerCharacterAPI.getAttributeAPI();
		List<AttributeModifier> modifiers = new ArrayList<>();
		List<AttributeValueProvider> providers = new ArrayList<>();
		Map<AttributeOperationModifier, Long> modifierRemainingTicks = new HashMap<>();
		for (AttributeOperationModifier modifier : playerAttributeAPI.getModifiers()) {
			// 永続化しないものを弾く
			if (!modifier.isPersistentToPlayer()) {
				continue;
			}
			switch (modifier) {
				case AttributeModifier m -> modifiers.add(m);
				case AttributeValueProvider p -> providers.add(p);
				default -> {
				}
			}
			OptionalLong remainingTicks = playerAttributeAPI.getRemainingTicks(modifier);
			if (remainingTicks.isEmpty()) {
				continue;
			}
			modifierRemainingTicks.put(modifier, remainingTicks.getAsLong());
		}
		return new PlayerData(uuid, playerAttributeAPI.getBaseAttributes(), modifiers, providers, modifierRemainingTicks, playerAttributeAPI.getModifierInstanceIds());
	}
}
