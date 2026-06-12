package net.logiench.logienchlibv2.api.minecraft.menu.inventory.util;

import net.logiench.logienchlibv2.api.minecraft.menu.inventory.InvMenuConsumers;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

public interface BasicListener {
	/// 表示しているインベントリに変化がある可能性のあるイベントをキャンセルします
	InvMenuConsumers.InventoryConsumer INVENTORY_ITEM_CHANGE_CHANCEL = new InvMenuConsumers.InventoryConsumer() {

		@Override
		public InvMenuConsumers.InventoryClickConsumer getClickConsumer() {
			return ev -> {
				if (ev.isCancelled()) {
					return;
				}
				Inventory clicked = ev.getClickedInventory();
				if (clicked == null) {
					return;
				}
				if (clicked.getType() == ev.getView().getTopInventory().getType()) {
					ev.setCancelled(true);
					return;
				}
				ClickType type = ev.getClick();
				if (type.isShiftClick() || type == ClickType.DOUBLE_CLICK) {
					ev.setCancelled(true);
				}
			};
		}

		@Override
		public InvMenuConsumers.InventoryDragConsumer getDragConsumer() {
			return INVENTORY_CHANGE_DRAG_CANCEL;
		}
	};

	/// 全ての変更イベントをキャンセルします
	InvMenuConsumers.InventoryConsumer ANY_CHANGE_CANCEL = new InvMenuConsumers.InventoryConsumer() {

		@Override
		public InvMenuConsumers.InventoryClickConsumer getClickConsumer() {
			return e -> e.setCancelled(true);
		}

		@Override
		public InvMenuConsumers.InventoryDragConsumer getDragConsumer() {
			return e -> e.setCancelled(true);
		}
	};

	/// インベントリからのアイテムドロップをキャンセルします
	InvMenuConsumers.InventoryClickConsumer DROP_CANCEL = ev -> {
		if (ev.isCancelled()) {
			return;
		}
		ClickType type = ev.getClick();
		if (ev.getSlotType().equals(InventoryType.SlotType.OUTSIDE) || type.equals(ClickType.DROP) || type.equals(ClickType.CONTROL_DROP)) {
			ev.setCancelled(true);
		}
	};

	/// インベントリからのドロップをキャンセルします
	InvMenuConsumers.InventoryDragConsumer INVENTORY_CHANGE_DRAG_CANCEL = ev -> {
		if (ev.isCancelled()) {
			return;
		}
		int size = ev.getView().getTopInventory().getSize();
		if (ev.getRawSlots().stream().anyMatch(s -> s < size)) {
			ev.setCancelled(true);
		}
	};
}
