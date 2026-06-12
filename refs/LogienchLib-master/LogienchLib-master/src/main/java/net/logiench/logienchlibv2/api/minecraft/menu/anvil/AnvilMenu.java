package net.logiench.logienchlibv2.api.minecraft.menu.anvil;

import net.kyori.adventure.text.Component;
import net.logiench.logienchlibv2.api.cache.master.player.SingleData;
import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import net.logiench.logienchlibv2.api.minecraft.menu.inventory.InvMenuConsumers;
import net.logiench.logienchlibv2.api.minecraft.menu.inventory.InventoryMenu;
import net.logiench.logienchlibv2.base.minecraft.menu.MenuEventException;
import net.logiench.logienchlibv2.cacheKeys.AnvilMenuDataKey;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static net.logiench.logienchlibv2.api.minecraft.menu.anvil.AnvilMenuConsumers.*;

/**
 * かな床を使用してテキストを入力させますが、統合版の場合は1レベル以上ないと決定できません。<br>
 * 対象のプレイヤーによって自動で入力形式の切り替わる、
 * {@link net.logiench.logienchlibv2.api.minecraft.menu.input.TextInputMenu} を使用してください
 */
@SuppressWarnings("unused")
public class AnvilMenu {
	private final InventoryMenu menu;
	private final List<AnvilEnterConsumer> enterListeners = new ArrayList<>();

	public AnvilMenu(@NotNull String title) {
		this.menu = new InventoryMenu(title, InventoryType.ANVIL);
		init();
	}

	public AnvilMenu(@NotNull Component title) {
		this.menu = new InventoryMenu(title, InventoryType.ANVIL);
		init();
	}

	public AnvilMenu(@NotNull Inventory inventory) {
		if (inventory.getType() != InventoryType.ANVIL) {
			throw new IllegalArgumentException("Inventory must be anvil");
		}
		this.menu = new InventoryMenu(inventory);
		init();
	}

	private void init() {
		// 入力テキスト記録用キャッシュ
		addOpenListener((p, menu) -> SingleData.put(p, AnvilMenuDataKey.INPUT_TEXT, ""));
		addCloseListener(ev ->
			SingleData.remove((Player) ev.getPlayer(), AnvilMenuDataKey.INPUT_TEXT)
		);
		// 決定ボタン(アイテムの作成) が押されたかの判定と処理
		addClickListener(ev -> {
			if (ev.getClickedInventory() != ev.getInventory() || ev.getSlot() != 2) {
				return;
			}
			Player p = (Player) ev.getWhoClicked();
			AnvilEnterEvent e = new AnvilEnterEvent(ev.getInventory(), p, SingleData.get(p, AnvilMenuDataKey.INPUT_TEXT), ev.isCancelled());
			for (AnvilEnterConsumer listener : enterListeners) {
				MenuEventException.accept(p, "AnvilMenu: EnterEvent", listener, e);
			}
			ev.setCancelled(e.isCancelled());
			// @todo 経験値がcancel trueで消費されないか、falseなら消費されるか、消費される場合はそれをクラスに明記
		});
	}

	public AnvilMenu addAllListener(@NotNull InvMenuConsumers.InventoryConsumer consumers) {
		menu.addAllListener(consumers);
		return this;
	}

	public AnvilMenu addAllListener(@NotNull AnvilMenuConsumers.InventoryConsumer consumers) {
		AnvilEnterConsumer enter = consumers.getAnvilEnterConsumer();
		if (enter != null) {
			enterListeners.add(enter);
		}
		return addAllListener(consumers);
	}

	public AnvilMenu addOpenListener(@NotNull InventoryOpenConsumer listener) {
		menu.addOpenListener(listener);
		return this;
	}

	public AnvilMenu addClickListener(@NotNull InventoryClickConsumer listener) {
		menu.addClickListener(listener);
		return this;
	}

	public AnvilMenu addDragListener(@NotNull InventoryDragConsumer listener) {
		menu.addDragListener(listener);
		return this;
	}

	public AnvilMenu addCloseListener(@NotNull InventoryCloseConsumer listener) {
		menu.addCloseListener(listener);
		return this;
	}

	public AnvilMenu addTimerTask(@NotNull Consumer<Inventory> consumer, long delay, long period) {
		menu.addTimerTask(consumer, delay, period);
		return this;
	}

	public AnvilMenu setItem(int slot, @Nullable ItemStack item) {
		menu.setItem(slot, item);
		return this;
	}

	public AnvilMenu setItem(int slot, @Nullable SuperItemStack item) {
		menu.setItem(slot, item);
		return this;
	}

	public AnvilMenu addItem(@NotNull ItemStack... items) throws IllegalArgumentException {
		menu.addItem(items);
		return this;
	}

	public AnvilMenu addItem(@NotNull SuperItemStack... items) throws IllegalArgumentException {
		menu.addItem(items);
		return this;
	}

	public AnvilMenu remove(@NotNull Material material) {
		menu.remove(material);
		return this;
	}

	public AnvilMenu remove(@NotNull ItemStack item) {
		menu.remove(item);
		return this;
	}

	public AnvilMenu remove(@NotNull SuperItemStack item) {
		menu.remove(item);
		return this;
	}

	public AnvilMenu onAllItems(@NotNull Consumer<ItemStack> consumer) {
		menu.onAllItems(consumer);
		return this;
	}

	public AnvilMenu onAllSuperItems(@NotNull Consumer<SuperItemStack> consumer) {
		menu.onAllSuperItems(consumer);
		return this;
	}

	/**
	 * @param bypassCancel {@link #addClickListener(InventoryClickConsumer)} がキャンセルされたのを無視するか
	 */
	public AnvilMenu addEnterListener(boolean bypassCancel, @NotNull AnvilEnterConsumer listener) {
		return addEnterListener(ev -> {
			if (bypassCancel || !ev.isCancelled()) {
				listener.accept(ev);
			}
		});
	}

	/**
	 * {@link #addEnterListener(boolean, AnvilEnterConsumer)}
	 * のbypassCancelがtrueと同じ動作をします
	 */
	public AnvilMenu addEnterListener(@NotNull AnvilEnterConsumer listener) {
		enterListeners.add(listener);
		return this;
	}

	public AnvilMenu send(@NotNull Player p) {
		menu.send(p);
		return this;
	}
}
