package net.logiench.logienchlibv2.listener;

import net.logiench.logienchlibv2.api.minecraft.event.PlayerDropItemSlotEvent;
import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public class InventoryItemListener implements Listener {

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	private void onPlayerDropItem(PlayerDropItemSlotEvent ev) {
		Player p = ev.getPlayer();
		SuperItemStack item = SuperItemStack.safeInit(ev.getItemDrop().getItemStack());
		if (item == null) {
			return;
		}
		if (checkItem(p.isInvulnerable(), p.isOp(), item)) {
			ev.setCancelled(true);
			item.destroy();
			ev.getItemDrop().remove();
		} else if (item.isMoveLock()) {
			ev.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	private void onPlayerPickupItem(EntityPickupItemEvent ev) {
		if (!(ev.getEntity() instanceof Player p)) {
			return;
		}
		if (checkItem(p.isInvulnerable(), p.isOp(), SuperItemStack.safeInit(ev.getItem().getItemStack()))) {
			ev.setCancelled(true);
			ev.getItem().remove();
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	private void onPlayerGameModeChange(PlayerGameModeChangeEvent ev) {
		Player p = ev.getPlayer();
		for (ItemStack item : p.getInventory().getContents()) {
			if (checkItem(ev.getNewGameMode().isInvulnerable(), p.isOp(), SuperItemStack.safeInit(item))) {
				item.setAmount(0);
			}
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	private void onPlayerInventoryClick(InventoryClickEvent ev) {
		if (!(ev.getWhoClicked() instanceof Player p)) {
			return;
		}
		Inventory clicked = ev.getClickedInventory();
		if (clicked == null) {
			return;
		}
		if (ev.isShiftClick()) {
			if (clicked.getType() != InventoryType.PLAYER) {
				return;
			}
			if (_checkItem(p, SuperItemStack.safeInit(ev.getCurrentItem()))) {
				return;
			}
			ev.setCancelled(true);
		} else {
			if (clicked.getType() == InventoryType.PLAYER) {
				return;
			}
			if (_checkItem(p, SuperItemStack.safeInit(ev.getCursor()))) {
				return;
			}
			ev.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	private void onPlayerInventoryDrag(InventoryDragEvent ev) {
		if (!(ev.getWhoClicked() instanceof Player p)) {
			return;
		}
		if (_checkItem(p, SuperItemStack.safeInit(ev.getOldCursor()))) {
			return;
		}
		if (ev.getRawSlots().stream().noneMatch(i -> i < ev.getInventory().getSize())) {
			return;
		}
		ev.setCancelled(true);
	}

	// MoveLockかPermission, ShowOnlyの判定 & 破壊
	private boolean _checkItem(Player p, SuperItemStack item) {
		if (item == null || !item.isMoveLock()) {
			if (checkItem(p.isInvulnerable(), p.isOp(), item)) {
				item.destroy();
			}
			return true;
		}
		return false;
	}

	/// @return 削除する必要がある場合は true
	@Contract("_, _, null -> false")
	protected boolean checkItem(boolean isInvulnerable, boolean isOp, @Nullable SuperItemStack item) {
		if (item == null) {
			return false;
		}
		int level = item.getItemPermission();

		if (level == 0) {
			return false;
		}
		return item.isShowOnly() || !(
			(level == 1 && (isInvulnerable || isOp)) ||
				(level == 2 && isOp) ||
				(level == 3 && isOp && isInvulnerable)
		);
	}
}
