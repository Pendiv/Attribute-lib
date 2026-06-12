package net.logiench.shardCore.core.menu.util;

import lombok.Getter;
import net.logiench.logienchlibv2.api.minecraft.menu.inventory.InvMenuConsumers;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.DragType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Viewのtopがクリックされた時、そのスロットに移動することが許可されているかをチェックし、結果次第でイベントをキャンセルします。
 */
public class ClickInventoryMenuCreator implements InvMenuConsumers.InventoryConsumer {
	private static final Predicate<ClickInventoryMenuEvent> ALWAYS_TRUE = e -> true;
	private final Map<Integer, Predicate<ClickInventoryMenuEvent>> allowedSlots = new HashMap<>();
	private final boolean canTakeOut;

	/**
	 * {@link #addAllowedSlot(int...)}などで指定したスロットからのみアイテムが取り出せます
	 */
	public ClickInventoryMenuCreator() {
		this(true);
	}

	/**
	 * @param canTakeOut アイテムの取り出しを許可するか ({@link #addAllowedSlot(int...)}などで指定したスロットからのみ取り出せます)
	 */
	public ClickInventoryMenuCreator(boolean canTakeOut) {
		this.canTakeOut = canTakeOut;
	}

	public ClickInventoryMenuCreator addAllowedSlot(int... slots) {
		for (int slot : slots) {
			allowedSlots.put(slot, ALWAYS_TRUE);
		}
		return this;
	}

	public ClickInventoryMenuCreator addAllowedSlot(int slot, Predicate<ClickInventoryMenuEvent> predicate) {
		allowedSlots.put(slot, predicate);
		return this;
	}

	public ClickInventoryMenuCreator addAllowedSlot(Map<Integer, Predicate<ClickInventoryMenuEvent>> allowedRawSlots) {
		this.allowedSlots.putAll(allowedRawSlots);
		return this;
	}

	@Override
	public InvMenuConsumers.@Nullable InventoryClickConsumer getClickConsumer() {
		return ev -> {
			if (!(ev.getWhoClicked() instanceof Player player)) {
				return;
			}
			Inventory top = ev.getInventory();
			if (ev.getClickedInventory() != top) {
				if (ev.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
					ev.setCancelled(true);
				}
				return;
			}
			// アイテムを取り出すときの処理
			if (!allowedSlots.containsKey(ev.getRawSlot())) {
				ev.setCancelled(true);
				return;
			}

			ClickType click = ev.getClick();
			if (click == ClickType.SWAP_OFFHAND) {
				if (!checkRawSlot(player, player.getInventory().getItemInOffHand(), ev)) {
					ev.setCancelled(true);
				}
			} else if (click == ClickType.NUMBER_KEY) {
				if (!checkRawSlot(player, player.getInventory().getItem(ev.getHotbarButton()), ev)) {
					ev.setCancelled(true);
				}
				return;
			}

			// カーソル上のアイテムが空か許可されたアイテムなら入れ替え
			if (!checkRawSlot(player, ev.getCursor(), ev)) {
				ev.setCancelled(true);
			}
		};
	}

	private boolean checkRawSlot(Player player, ItemStack item, InventoryClickEvent ev) {
		// 取り出しが拒否なら対象スロットが空でないと許可しない
		if (!canTakeOut) {
			ItemStack current = ev.getCurrentItem();
			if (!(current == null || current.isEmpty())) {
				return false;
			}
		}

		if (item == null || item.isEmpty()) {
			return true;
		}

		return checkRawSlot(ev.getRawSlot(), new ClickInventoryMenuEvent(player, item, ev.getView(), ev.getClickedInventory(), ev.getClick(), ev.getAction()));
	}

	@Override
	public InvMenuConsumers.@Nullable InventoryDragConsumer getDragConsumer() {
		return ev -> {
			if (!(ev.getWhoClicked() instanceof Player player)) {
				return;
			}
			InventoryView view = ev.getView();
			Set<Integer> rawSlots = ev.getRawSlots();
			if (rawSlots.size() != 1) {
				Inventory top = view.getTopInventory();
				if (rawSlots.stream().anyMatch(s -> view.getInventory(s) == top)) {
					ev.setCancelled(true);
				}
				return;
			}

			int rawSlot = rawSlots.iterator().next();
			DragType type = ev.getType();
			InventoryAction action = type == DragType.SINGLE ? InventoryAction.PLACE_ONE : InventoryAction.PLACE_ALL;
			ClickType clickType = type == DragType.SINGLE ? ClickType.RIGHT : ClickType.LEFT;
			boolean isAllow = checkRawSlot(rawSlot, new ClickInventoryMenuEvent(
				player, ev.getNewItems().values().iterator().next(), view, view.getInventory(rawSlot), clickType, action));

			// falseの場合は前までのを引継ぎたい
			if (!isAllow) {
				ev.setCancelled(true);
			}
		};
	}

	private boolean checkRawSlot(int slot, ClickInventoryMenuEvent ev) {
		Predicate<ClickInventoryMenuEvent> predicate = allowedSlots.get(slot);
		if (predicate == null) {
			return false;
		}
		return predicate.test(ev);
	}

	@Getter
	public static class ClickInventoryMenuEvent {
		private final Player player;
		private final ItemStack clickedItem;
		private final InventoryView view;
		private final Inventory clickedInventory;
		private final ClickType click;
		private final InventoryAction action;

		public ClickInventoryMenuEvent(Player player, ItemStack clickedItem, InventoryView view, Inventory clickedInventory, ClickType click, InventoryAction action) {
			this.player = player;
			this.clickedItem = clickedItem;
			this.view = view;
			this.clickedInventory = clickedInventory;
			this.click = click;
			this.action = action;
		}
	}
}
