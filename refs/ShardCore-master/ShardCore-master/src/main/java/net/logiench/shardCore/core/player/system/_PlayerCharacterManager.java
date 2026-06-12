package net.logiench.shardCore.core.player.system;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.logiench.shardCore.core.item.system.data.ItemSerializer;
import net.logiench.shardCore.core.item.system.loader.ItemLoader;
import net.logiench.shardCore.core.player.system.item.PlayerEquipmentGemManager;
import net.logiench.shardCore.core.player.system.stats.PlayerStatsManager;
import net.logiench.shardLib.api.ShardLibProvider;
import net.logiench.shardLib.api.player.PlayerAPI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@Singleton
public class _PlayerCharacterManager implements Listener {
	private static final PlayerAPI PLAYER_API = ShardLibProvider.get().getPlayerAPI();

	private final ItemSerializer serializer;
	private final PlayerStatsManager playerStatsManager;
	private final PlayerEquipmentGemManager playerEquipmentGemManager;

	@Inject
	private _PlayerCharacterManager(ItemSerializer serializer,
	                                PlayerStatsManager playerStatsManager, PlayerEquipmentGemManager playerEquipmentGemManager) {
		this.serializer = serializer;
		this.playerStatsManager = playerStatsManager;
		this.playerEquipmentGemManager = playerEquipmentGemManager;
	}

	public Optional<PlayerCharacter> getCharacter(Player player) {
		if (player == null || !player.isOnline()) {
			return Optional.empty();
		}
		return PLAYER_API.getCharacterAPI(player)
			.map(characterAPI -> new PlayerCharacter(player, characterAPI));
	}

	public void onCharacter(Player player, Consumer<PlayerCharacter> consumer) {
		getCharacter(player).ifPresent(consumer);
	}

	@EventHandler(priority = EventPriority.LOW)
	private void onPlayerJoin(PlayerJoinEvent ev) {
		Player player = ev.getPlayer();
		UUID playerId = player.getUniqueId();
		Optional<PlayerCharacter> characterOptional = getCharacter(player);
		if (characterOptional.isEmpty()) {
			return;
		}

		PlayerCharacter character = characterOptional.get();
		List<ItemLoader> equipmentItems = new ArrayList<>();
		/*for (Map.Entry<PlayerEquipmentEntity.Slot, PlayerEquipmentEntity> entry : equipmentManager.getEquipments(playerId).entrySet()) {
			PlayerEquipmentEntity table = entry.getValue();

			equipmentItems.add(
				ItemLoader.of(serializer.deserializeItem(table.itemId(), table.itemData())));
		}*/
		playerEquipmentGemManager.applyItemLoaders(playerId, equipmentItems);
		playerStatsManager.applyEquipmentStats(character, equipmentItems);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	private void onPlayerQuit(PlayerQuitEvent ev) {
		Player player = ev.getPlayer();
		UUID playerId = player.getUniqueId();
		playerEquipmentGemManager.quitPlayer(playerId);
	}
}
