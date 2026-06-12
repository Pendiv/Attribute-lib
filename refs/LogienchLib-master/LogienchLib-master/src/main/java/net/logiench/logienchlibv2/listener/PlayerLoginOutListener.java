package net.logiench.logienchlibv2.listener;

import net.logiench.logienchlibv2.ConfigState;
import net.logiench.logienchlibv2.api.cache.master.player.MapData;
import net.logiench.logienchlibv2.api.httpRequest.GET;
import net.logiench.logienchlibv2.cacheKeys.PlayerDataKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerLoginOutListener implements Listener {

	@EventHandler(priority = EventPriority.LOWEST)
	private void onPlayerJoin(PlayerJoinEvent ev) {
		UUID uuid = ev.getPlayer().getUniqueId();

		if (ConfigState.isGetBedrockPlayerSkin()) {
			FloodgatePlayer connection = FloodgateApi.getInstance().getPlayer(uuid);
			if (connection != null) {
				GET get = new GET("https://api.geysermc.org/v2/skin/" + connection.getXuid());
				String texture_id = get.getResponseValue("texture_id");
				String signature = get.getResponseValue("signature");
				String value = get.getResponseValue("value");
				if (value != null && signature != null) {
					Map<PlayerDataKey.SkinKey, String> data = new HashMap<>();
					data.put(PlayerDataKey.SkinKey.TEXTURE_ID, texture_id);
					data.put(PlayerDataKey.SkinKey.SIGNATURE, signature);
					data.put(PlayerDataKey.SkinKey.VALUE, value);
					MapData.setMap(uuid, PlayerDataKey.BE_SKIN_DATA, data);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	private void onPlayerQuit(PlayerQuitEvent ev) {

	}
}
