package net.logiench.logienchlibv2.api.minecraft.event;

import net.logiench.logienchlibv2.api.cache.master.player.SingleData;
import net.logiench.logienchlibv2.base.minecraft.event.DropSlotData;
import net.logiench.logienchlibv2.cacheKeys.PlayerDropSlotKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class PlayerDropItemSlotEvent extends PlayerEvent implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
	private final Item drop;
	private boolean cancel = false;
	private final int slot;
	private final Inventory dropedInventory;

	public PlayerDropItemSlotEvent(@NotNull Player player, @NotNull Item drop) {
		super(player);
		this.drop = drop;
		DropSlotData dropSlotData = SingleData.remove(player.getUniqueId(), PlayerDropSlotKey.DROP_SLOT);
		Integer slot = null;
		Inventory inventory = null;
		if (dropSlotData != null) {
			slot = dropSlotData.slot();
			inventory = dropSlotData.inventory();
		}
		if (inventory == null) {
			if (slot == null) {
				slot = player.getInventory().getHeldItemSlot();
			}
			inventory = player.getInventory();
		}
		this.slot = slot;
		this.dropedInventory = inventory;
	}

	@NotNull
	public Item getItemDrop() {
		return this.drop;
	}

	public boolean isCancelled() {
		return this.cancel;
	}

	public void setCancelled(boolean cancel) {
		this.cancel = cancel;
	}

	@NotNull
	public HandlerList getHandlers() {
		return handlers;
	}

	@NotNull
	public static HandlerList getHandlerList() {
		return handlers;
	}

	public int getSlot() {
		return slot;
	}

	@NotNull
	public Inventory getDroppedInventory() {
		return dropedInventory;
	}
}
