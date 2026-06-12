package net.logiench.shardCore.core.menu.util;

import com.google.inject.Inject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.logiench.shardCore.core.menu.MenuFactory;
import net.logiench.shardCore.core.menu.MenuNavigationManager;
import net.logiench.shardCore.core.menu.MenuStateManager;
import net.logiench.shardCore.core.menu.NavigationContext;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.function.Function;
import java.util.function.Supplier;

public abstract class AbstractMenu {
	private Supplier<AbstractMenu> constructor;
	protected final Player player;
	@Inject
	protected MenuFactory menuFactory;
	@Inject
	protected MenuStateManager menuStateManager;
	@Inject
	protected MenuNavigationManager navigationManager;

	public AbstractMenu(Player player) {
		this.player = player;
	}

	/**
	 * バイパス処理なしのメニューを開きます。
	 * {@link #open(boolean)}の<code>false</code>と同様です
	 */
	public final void open() {
		open(false);
	}

	/**
	 * メニューがコマンドによって停止されている場合でもバイパスして開くオプションを提供します。
	 *
	 * @param force メニューが停止されている際、trueの場合はバイパス処理をします。falseの場合は開けず、メッセージが表示されます。
	 */
	public final void open(boolean force) {
		if (!force && menuStateManager.isDisabled(this.getClass())) {
			if (player.hasPermission("shardcore.admin.bypass")) {
				// バイパス(確認メニュー経由)させて開く
				// 確認用メニューは確実に安全だとする
				menuFactory.create(player -> new ForceOpenConfirmationMenu(player, this), player).open(true);
				return;
			}
			player.sendMessage(Component.text("このメニューは現在メンテナンス中のため利用できません。", NamedTextColor.RED));
			//			player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.0f);
			return;
		}

		initMenu();
		doOpen();
	}

	/**
	 * メニューが表示される前に1度だけ実行されます。
	 */
	protected abstract void initMenu();

	protected abstract void doOpen();

	public abstract Component getTitle();

	protected void openNext(Function<Player, AbstractMenu> constructor) {
		openNext(() -> constructor.apply(player));
	}

	// 指定されたインスタンスを履歴に登録
	protected void openNext(Supplier<AbstractMenu> newConstructor) {
		// 1. 履歴に「今の自分の呼び出し方」を保存する
		NavigationContext context = navigationManager.getContext(player);
		context.pushNext(this.constructor);

		AbstractMenu newMenu = menuFactory.create(newConstructor);
		// 2. どうやって呼び出されたかの情報を記録する
		newMenu.constructor = newConstructor;
		// 3. 開く
		newMenu.open();
	}

	protected void back() {
		NavigationContext context = navigationManager.getContext(player);
		AbstractMenu prevMenu = context.popBack(this);

		if (prevMenu != null) {
			prevMenu.open();
		} else {
			// 履歴がない場合
			// player.closeInventory();
			player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
		}
	}
}
