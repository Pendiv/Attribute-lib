package net.logiench.logienchlibv2.api.minecraft.menu.inventory;

import net.kyori.adventure.text.Component;
import net.logiench.logienchlibv2.LogienchLib;
import net.logiench.logienchlibv2.api.cache.master.player.ListData;
import net.logiench.logienchlibv2.api.cache.master.player.SingleData;
import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import net.logiench.logienchlibv2.api.minecraft.text.ComponentUtil;
import net.logiench.logienchlibv2.api.minecraft.time.Timer;
import net.logiench.logienchlibv2.cacheKeys.InvMenuDataKey;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

import static net.logiench.logienchlibv2.api.minecraft.menu.inventory.InvMenuConsumers.*;

@SuppressWarnings("unused")
public class InventoryMenu {
	private final Inventory inventory;
	private final List<InventoryOpenConsumer> initListeners = new ArrayList<>();
	private final List<InventoryClickConsumer> clickListeners = new ArrayList<>();
	private final List<InventoryDragConsumer> dragListeners = new ArrayList<>();
	private final List<InventoryCloseConsumer> closeListeners = new ArrayList<>();
	private final Map<NamespacedKey, TimerData> timers = new HashMap<>();

	public InventoryMenu(@NotNull String title, @NotNull InventoryType type) {
		this(createInventory(title, type));
	}

	public InventoryMenu(@NotNull Component title, @NotNull InventoryType type) {
		this(createInventory(title, type));
	}

	public InventoryMenu(@NotNull String title, int size) {
		this(createInventory(title, size));
	}

	public InventoryMenu(@NotNull Component title, int size) {
		this(createInventory(title, size));
	}

	public InventoryMenu(@NotNull Inventory inventory) {
		this.inventory = inventory;
	}

	@NotNull
	public Inventory getInventory() {
		return inventory;
	}

	public InventoryMenu addAllListener(@NotNull InventoryConsumer consumers) {
		InventoryOpenConsumer init = consumers.getOpenConsumer();
		InventoryClickConsumer click = consumers.getClickConsumer();
		InventoryDragConsumer drag = consumers.getDragConsumer();
		InventoryCloseConsumer close = consumers.getCloseConsumer();
		if (init != null) {
			addOpenListener(init);
		}
		if (click != null) {
			addClickListener(click);
		}
		if (drag != null) {
			addDragListener(drag);
		}
		if (close != null) {
			addCloseListener(close);
		}
		return this;
	}

	public InventoryMenu addOpenListener(@NotNull InventoryOpenConsumer listener) {
		initListeners.add(listener);
		return this;
	}

	public InventoryMenu addClickListener(@NotNull InventoryClickConsumer listener) {
		clickListeners.add(listener);
		return this;
	}

	public InventoryMenu addDragListener(@NotNull InventoryDragConsumer listener) {
		dragListeners.add(listener);
		return this;
	}

	public InventoryMenu addCloseListener(@NotNull InventoryCloseConsumer listener) {
		closeListeners.add(listener);
		return this;
	}

	public InventoryMenu addTimerTask(@NotNull Consumer<Inventory> consumer, long delay, long period) {
		timers.put(new NamespacedKey(LogienchLib.getInstance(), UUID.randomUUID().toString()), new TimerData(() -> consumer.accept(inventory), delay, period));
		return this;
	}

	public InventoryMenu setItem(int slot, @Nullable ItemStack item) {
		inventory.setItem(slot, item);
		return this;
	}

	public InventoryMenu setItem(int slot, @Nullable SuperItemStack item) {
		if (item != null) {
			setItem(slot, item.build());
		}
		return this;
	}

	public InventoryMenu addItem(@NotNull ItemStack... items) throws IllegalArgumentException {
		inventory.addItem(items);
		return this;
	}

	public InventoryMenu addItem(@NotNull SuperItemStack... items) throws IllegalArgumentException {
		if (items == null || items.length == 0) {
			throw new IllegalArgumentException();
		}
		inventory.addItem(Arrays.stream(items).map(SuperItemStack::build).toArray(ItemStack[]::new));
		return this;
	}

	public InventoryMenu remove(@NotNull Material material) {
		inventory.remove(material);
		return this;
	}

	public InventoryMenu remove(@NotNull ItemStack item) {
		inventory.remove(item);
		return this;
	}

	public InventoryMenu remove(@NotNull SuperItemStack item) {
		return remove(item.build());
	}

	public InventoryMenu onAllItems(@NotNull Consumer<ItemStack> consumer) {
		for (ItemStack item : inventory.getContents()) {
			if (item != null) {
				consumer.accept(item);
			}
		}
		return this;
	}

	public InventoryMenu onAllSuperItems(@NotNull Consumer<SuperItemStack> consumer) {
		return onAllItems(i -> {
			SuperItemStack item = SuperItemStack.safeInit(i);
			if (item != null) {
				consumer.accept(item);
			}
		});
	}

	public InventoryMenu send(@NotNull Player p) {
		UUID uuid = p.getUniqueId();
		if (p.getOpenInventory().getTopInventory() != inventory) {
			SingleData.put(uuid, InvMenuDataKey.TO_OPEN, () -> updateListener(uuid));
			p.openInventory(inventory);
		} else {
			updateListener(uuid);
		}
		for (InventoryOpenConsumer consumer : initListeners) {
			consumer.accept(p, this);
		}
		return this;
	}

	private void updateListener(@NotNull UUID uuid) {
		if (!clickListeners.isEmpty()) {
			ListData.setList(uuid, InvMenuDataKey.CLICK, clickListeners);
		}
		if (!dragListeners.isEmpty()) {
			ListData.setList(uuid, InvMenuDataKey.DRAG, dragListeners);
		}
		ListData.setList(uuid, InvMenuDataKey.CLOSE, closeListeners);
		List<NamespacedKey> timerKeys;
		if (!timers.isEmpty()) {
			timerKeys = ListData.setList(uuid, InvMenuDataKey.TIMER, new ArrayList<>(timers.keySet()));
			for (Map.Entry<NamespacedKey, TimerData> entry : timers.entrySet()) {
				TimerData data = entry.getValue();
				Timer.on(entry.getKey(), data.runnable(), data.delay(), data.period());
			}
		} else {
			timerKeys = ListData.remove(uuid, InvMenuDataKey.TIMER);
		}
		if (timerKeys != null) {
			timerKeys.forEach(Timer::stop);
		}
	}

	private record TimerData(@NotNull Runnable runnable, long delay, long period) {
	}

	@NotNull
	public static Inventory createInventory(@NotNull String title, @NotNull InventoryType type) {
		return createInventory(ComponentUtil.text(title), type);
	}

	@NotNull
	public static Inventory createInventory(@NotNull Component title, @NotNull InventoryType type) {
		return Bukkit.createInventory(null, type, title);
	}

	@NotNull
	public static Inventory createInventory(@NotNull String title, int size) {
		return createInventory(ComponentUtil.text(title), size);
	}

	@NotNull
	public static Inventory createInventory(@NotNull Component title, int size) {
		return Bukkit.createInventory(null, size, title);
	}
}
