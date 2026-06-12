package net.logiench.shardLib.database.dao;

import net.logiench.shardLib.api.attribute.data.AttributeOperationModifier;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PlayerDataDAO {
	/**
	 * プレイヤーデータを非同期でロードする
	 *
	 * @param uuid プレイヤーのUUID
	 * @return プレイヤーの全データを返すCompletableFuture
	 */
	CompletableFuture<Optional<PlayerData>> loadPlayerData(UUID uuid);

	/**
	 * 複数のプレイヤーデータを非同期でセーブする
	 *
	 * @param data プレイヤーのUUIDとデータ
	 * @return 新しく保存するModifierのinstanceId
	 */
	CompletableFuture<Map<UUID, Map<AttributeOperationModifier, Long>>> savePlayerData(PlayerData... data);

	/**
	 * 指定されたModifierを全て削除します
	 *
	 * @param deleteModifierInstanceId 削除するModifierのinstanceId
	 * @param deleteProviderInstanceId 削除するProviderのinstanceId
	 */
	CompletableFuture<Void> deletePlayerModifier(List<Long> deleteModifierInstanceId, List<Long> deleteProviderInstanceId);

}