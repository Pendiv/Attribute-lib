package net.logiench.logienchlibv2.api.minecraft.player;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;

@SuppressWarnings("unused")
public class EditionCheck {

	/// オフラインのプレイヤーでも判定できますが、確実ではありません
	public static boolean isBedrockId(UUID uuid) {
		return FloodgateApi.getInstance().isFloodgateId(uuid);
	}

	/// オフラインのプレイヤーでも判定できますが、確実ではありません
	public static boolean isBedrockId(OfflinePlayer player) {
		return isBedrockId(player.getUniqueId());
	}

	/// 現在オンラインのプレイヤーのみ判定できます
	public static boolean isBedrockPlayer(UUID uuid) {
		return FloodgateApi.getInstance().isFloodgatePlayer(uuid);
	}

	/// 現在オンラインのプレイヤーのみ判定できます
	public static boolean isBedrockPlayer(Player player) {
		return isBedrockPlayer(player.getUniqueId());
	}

	/// オフラインのプレイヤーでも判定できますが、確実ではありません
	public static boolean isJavaId(UUID uuid) {
		return !isBedrockId(uuid);
	}

	/// オフラインのプレイヤーでも判定できますが、確実ではありません
	public static boolean isJavaId(OfflinePlayer player) {
		return !isBedrockId(player);
	}

	/// 現在オンラインのプレイヤーのみ判定できます
	public static boolean isJavaPlayer(UUID uuid) {
		return !isBedrockPlayer(uuid);
	}

	/// 現在オンラインのプレイヤーのみ判定できます
	public static boolean isJavaPlayer(Player player) {
		return !isBedrockPlayer(player);
	}
}
