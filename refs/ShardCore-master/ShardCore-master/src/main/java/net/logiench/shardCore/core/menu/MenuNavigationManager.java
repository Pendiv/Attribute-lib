package net.logiench.shardCore.core.menu;

import com.google.inject.Singleton;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class MenuNavigationManager implements Listener {
	private final Map<UUID, NavigationContext> contexts = new ConcurrentHashMap<>();

	public NavigationContext getContext(Player player) {
		return contexts.computeIfAbsent(player.getUniqueId(), k -> new NavigationContext());
	}

	// プレイヤーがサーバーから抜けたら履歴を削除
	// fixme 職業が変わってからの履歴はバグの元だから修正。場合によっては履歴システムを捨てるべき
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		contexts.remove(event.getPlayer().getUniqueId());
	}

	// ※メニューを完全に閉じた(Escキー)時に履歴を消すかどうかは仕様次第です。
	// 今回は「閉じてまた開いたら履歴が残っている」挙動もありえるため、ここには書きません。
}