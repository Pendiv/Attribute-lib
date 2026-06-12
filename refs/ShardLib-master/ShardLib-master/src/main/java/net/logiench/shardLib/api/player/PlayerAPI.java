package net.logiench.shardLib.api.player;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public interface PlayerAPI {
	/**
	 * プレイヤーの詳細を取得します
	 *
	 * @param player 取得する対象のプレイヤー
	 * @return PlayerCharacterAPIを取得します。プレイヤーが見つからないときはempty
	 */
	@NotNull
	Optional<PlayerCharacterAPI> getCharacterAPI(@NotNull Player player);

	/**
	 * プレイヤーの詳細を取得します
	 *
	 * @param uuid 取得する対象のプレイヤーのUUID
	 * @return PlayerCharacterAPIを取得します。プレイヤーが見つからないときはempty
	 */
	@NotNull
	Optional<PlayerCharacterAPI> getCharacterAPI(UUID uuid);

}
