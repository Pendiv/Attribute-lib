package net.logiench.shardCore.core.player.system.stash;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.logiench.shardCore.db.service.PlayerStashContent;
import net.logiench.shardCore.db.service.PlayerStashItemService;
import net.logiench.shardCore.db.service.StashCustomTagService;
import net.logiench.shardCore.db.service.StashItemData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class PlayerStashManager {

	private final PlayerStashItemService stashService;
	private final StashCustomTagService tagService;

	private final Map<UUID, HotbarReference> hotbarReferenceCache = new ConcurrentHashMap<>();

	@Inject
	private PlayerStashManager(PlayerStashItemService stashService, StashCustomTagService tagService) {
		this.stashService = stashService;
		this.tagService = tagService;
	}

	/**
	 * 指定されたタグをすべてのアイテムから削除し、選択中から外します。
	 *
	 * @param playerId 対象となるプレイヤー
	 * @param tagId    削除するタグのID
	 * @return タグの削除に成功したか、
	 */
	@NotNull
	public CompletableFuture<Boolean> removeSelectedTag(@NotNull UUID playerId, long tagId) {
		return tagService.removeSelectedTag(playerId, tagId)
			.thenApply(v -> {
				// 選択中のタグ削除に成功したらアイテムのタグを消す
				PlayerStashContent content = getStash(playerId);
				if (content != null) {
					content.removeTagIds(Set.of(tagId));
				}
				return v;
			});
	}

	@NotNull
	public Map<Long, String> getItemTagNames(@NotNull StashItemData data) {
		return tagService.getTagsByCache(data.getTags());
	}

	@Nullable
	public PlayerStashContent getStash(UUID playerId) {
		return stashService.getStash(playerId);
	}

	// --- HotbarReference のライフサイクル管理 ---

	/**
	 * プレイヤーの HotbarReference をロードします（ログイン時に呼び出す）。
	 * 現在はメモリ上のみで管理し、空の参照から開始します。
	 *
	 * @param playerId 対象プレイヤーのUUID
	 */
	public void loadHotbarReference(@NotNull UUID playerId) {
		hotbarReferenceCache.put(playerId, new HotbarReference());
	}

	/**
	 * プレイヤーの HotbarReference を取得します。
	 * ロードされていない場合は null を返します。
	 *
	 * @param playerId 対象プレイヤーのUUID
	 * @return HotbarReference。未ロードの場合は null
	 */
	@Nullable
	public HotbarReference getHotbarReference(@NotNull UUID playerId) {
		return hotbarReferenceCache.get(playerId);
	}

	/**
	 * プレイヤーの HotbarReference をアンロードします（ログアウト時に呼び出す）。
	 *
	 * @param playerId 対象プレイヤーのUUID
	 */
	public void unloadHotbarReference(@NotNull UUID playerId) {
		hotbarReferenceCache.remove(playerId);
	}

	/*
	todo インベントリ確認のために変換部分をどう共有するかが大変だけど作成する
	public CompletableFuture<PlayerStashContent> getOfflineStash(UUID playerId) {
		return null;
	}
	*/
}
