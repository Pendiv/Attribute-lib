package net.logiench.logienchlibv2.listener;

import net.logiench.logienchlibv2.api.cache.master.player.SingleData;
import net.logiench.logienchlibv2.api.minecraft.event.PlayerDropItemSlotEvent;
import net.logiench.logienchlibv2.api.minecraft.time.Task;
import net.logiench.logienchlibv2.base.minecraft.event.DropSlotData;
import net.logiench.logienchlibv2.cacheKeys.PlayerDropSlotKey;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

public class PlayerDropSlotListener implements Listener {

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void onClickDropItem(InventoryClickEvent ev) {
		if (ev.getWhoClicked() instanceof Player p) {
			int slot = ev.getSlot();
			if (ev.getCurrentItem() == null)
				slot = -1;
			SingleData.put(p.getUniqueId(), PlayerDropSlotKey.DROP_SLOT, new DropSlotData(ev.getClickedInventory(), slot));
			Task.on(() -> SingleData.remove(p.getUniqueId(), PlayerDropSlotKey.DROP_SLOT));
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onDropItem(PlayerDropItemEvent ev) {
		Player p = ev.getPlayer();
		PlayerDropItemSlotEvent newEv = new PlayerDropItemSlotEvent(p, ev.getItemDrop());
		Bukkit.getServer().getPluginManager().callEvent(newEv);
		if (newEv.isCancelled())
			ev.setCancelled(true);
	}
}
