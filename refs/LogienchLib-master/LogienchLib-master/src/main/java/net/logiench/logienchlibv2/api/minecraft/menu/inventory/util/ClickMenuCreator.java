package net.logiench.logienchlibv2.api.minecraft.menu.inventory.util;

import net.logiench.logienchlibv2.api.minecraft.menu.inventory.InvMenuConsumers;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ClickMenuCreator implements InvMenuConsumers.InventoryConsumer {
	private final Map<Integer, InvMenuConsumers.InventoryClickConsumer> itemClicks = new HashMap<>();
	private final boolean notCancel;
	private final boolean ignoreCancel;

	public ClickMenuCreator() {
		this(false, false);
	}

	public ClickMenuCreator(boolean notCancel, boolean ignoreCancel) {
		this.notCancel = notCancel;
		this.ignoreCancel = ignoreCancel;
	}

	public InvMenuConsumers.InventoryClickConsumer getClickConsumer() {
		return ev -> {
			if (!ignoreCancel && ev.isCancelled()) {
				return;
			}
			if (!notCancel) {
				Objects.requireNonNull(BasicListener.INVENTORY_ITEM_CHANGE_CHANCEL.getClickConsumer()).accept(ev);
			}
			InvMenuConsumers.InventoryClickConsumer consumer = itemClicks.get(ev.getRawSlot());
			if (consumer != null) {
				consumer.accept(ev);
			}
		};
	}

	public InvMenuConsumers.InventoryDragConsumer getDragConsumer() {
		return ev -> {
			if (notCancel || ev.isCancelled()) {
				return;
			}
			Objects.requireNonNull(BasicListener.INVENTORY_ITEM_CHANGE_CHANCEL.getDragConsumer()).accept(ev);
		};
	}

	@NotNull
	public ClickMenuCreator addEvent(int slot, InvMenuConsumers.InventoryClickConsumer itemClick) {
		itemClicks.put(slot, itemClick);
		return this;
	}
}
