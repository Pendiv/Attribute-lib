package net.logiench.logienchlibv2.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import net.logiench.logienchlibv2.LogienchLib;
import net.logiench.logienchlibv2.api.cache.master.player.SingleData;
import net.logiench.logienchlibv2.cacheKeys.AnvilMenuDataKey;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public final class AnvilMenuPacket implements PacketListener {

	@Override
	public void onPacketSending(PacketEvent ev) {
	}

	@Override
	public void onPacketReceiving(PacketEvent ev) {
		PacketContainer packet = ev.getPacket();
		if (packet == null) {
			return;
		}
		UUID uuid = ev.getPlayer().getUniqueId();
		String value = SingleData.get(uuid, AnvilMenuDataKey.INPUT_TEXT);
		if (value == null) {
			return;
		}
		SingleData.put(uuid, AnvilMenuDataKey.INPUT_TEXT, packet.getStrings().readSafely(0));
	}

	@Override
	public ListeningWhitelist getSendingWhitelist() {
		return ListeningWhitelist.EMPTY_WHITELIST;
	}

	@Override
	public ListeningWhitelist getReceivingWhitelist() {
		return ListeningWhitelist.newBuilder().types(PacketType.Play.Client.ITEM_NAME).build();
	}

	@Override
	public Plugin getPlugin() {
		return LogienchLib.getInstance();
	}
}
