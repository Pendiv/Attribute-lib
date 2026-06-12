package net.logiench.shardCore.core.menu.main.stash;

import com.google.inject.Inject;
import net.kyori.adventure.text.Component;
import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import net.logiench.shardCore.ShardCore;
import net.logiench.shardCore.core.item.system.data.ItemSerializer;
import net.logiench.shardCore.core.item.system.generator.ItemGenerationResult;
import net.logiench.shardCore.core.menu.util.SimpleMenu;
import net.logiench.shardCore.core.player.system.stash.PlayerStashManager;
import net.logiench.shardCore.db.service.PlayerStashContent;
import net.logiench.shardCore.db.service.StashItemData;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StashMenu extends SimpleMenu {
	private static final Component TITLE = Component.text("Stash Menu");
	private static final int STASH_DISPLAY_SLOT_SIZE = 45;

	@Inject
	private PlayerStashManager stashManager;
	@Inject
	private ItemSerializer itemSerializer;
	@Nullable
	private final OfflinePlayer stashPlayer;
	/// このフィールドに直接アクセスするのではなく、{@link #getContent()}を使用してください！
	@Nullable
	private PlayerStashContent content = null;

	private int currentPage = 0;
	/*
	1. メニュー表示中に外部から変更（アイテム拾うなど）があったら更新されるようにする
	2. アイテムの復元処理は重たいので、できるだけキャッシュする
	3. アイテムにはタグ、一定以上のステータス、アイテムの種類でフィルターをかけられるようにする
	4. 取り出したアイテムには専用の識別タグを付与する
	5. ドロップを検知したらスタッシュからアイテムを減らし、識別タグを外す
	 */
	private List<StashItemData> displayedItemDataCache = List.of();
	private final Map<Integer, SuperItemStack> displayedItemsCache = new HashMap<>();

	public StashMenu(@NotNull Player player, @NotNull PlayerStashContent content) {
		super(player);
		this.stashPlayer = null;
		this.content = content;
	}

	public StashMenu(@NotNull Player player, @NotNull OfflinePlayer stashPlayer) {
		super(player);
		this.stashPlayer = stashPlayer;
	}

	public StashMenu(@NotNull Player player) {
		this(player, player);
	}

	@NotNull
	private PlayerStashContent getContent() {
		if (content != null) {
			return content;
		}
		if (stashPlayer == null) {
			// contentがnullならstashPlayerはNotNullが強制される
			throw new RuntimeException("到達しえない例外が発生しました");
		}
		this.content = stashManager.getStash(stashPlayer.getUniqueId());
		if (content == null) {
			//content =
			throw new UnsupportedOperationException("オフラインのプレイヤーのスタッシュロードは作成中です");
		}
		return content;
	}

	@Override
	protected void initMenu() {
		menu.addTimerTask(inv -> {
			// 複数人がメニューを同時に開く可能性があるのでd、変化があったら更新するように
			reflashItems();
		}, 20, 20);

		reflashItems();
	}

	private void resetCache(List<StashItemData> itemDataList) {
		this.displayedItemDataCache = itemDataList;
		this.displayedItemsCache.clear();
	}

	private SuperItemStack getOrCreateItem(int index) {
		SuperItemStack item = displayedItemsCache.get(index);
		if (item != null) {
			return item;
		}
		StashItemData itemData = displayedItemDataCache.get(index);
		ItemGenerationResult result = itemSerializer.deserialize(itemData.getItemData(), itemData.getItemDataJson());
		if (result == null) {
			ShardCore.getPLogger().warning("アイテムの復元に失敗しました");
			return null;
		}
		SuperItemStack generatedItem = result.item();
		if (!result.isSuccess() || generatedItem == null) {
			result.printMessage();
		}
		displayedItemsCache.put(index, generatedItem);
		return generatedItem;
	}

	private void reflashItems() {
		int offset = currentPage * STASH_DISPLAY_SLOT_SIZE;
		for (int slot = 0; slot < STASH_DISPLAY_SLOT_SIZE; slot++) {
			SuperItemStack item = getOrCreateItem(slot + offset);
			if (item == null) {
				// 復元に失敗したらスロットを詰める
				slot--;
				continue;
			}
			menu.setItem(slot, item);
		}
	}

	@Override
	public int getSize() {
		return 54;
	}

	@Override
	public Component getTitle() {
		return null;
	}
}
