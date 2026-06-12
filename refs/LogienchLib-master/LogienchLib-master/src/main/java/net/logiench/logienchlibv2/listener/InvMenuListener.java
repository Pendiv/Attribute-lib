package net.logiench.logienchlibv2.listener;

import net.logiench.logienchlibv2.api.cache.master.player.ListData;
import net.logiench.logienchlibv2.api.cache.master.player.SingleData;
import net.logiench.logienchlibv2.api.minecraft.time.Timer;
import net.logiench.logienchlibv2.base.minecraft.menu.MenuEventException;
import net.logiench.logienchlibv2.cacheKeys.InvMenuDataKey;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.List;
import java.util.UUID;

import static net.logiench.logienchlibv2.api.minecraft.menu.inventory.InvMenuConsumers.*;

public class InvMenuListener implements Listener {

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onInventoryClick(InventoryClickEvent ev) {
		if (!(ev.getWhoClicked() instanceof Player p)) {
			return;
		}
		List<InventoryClickConsumer> events = ListData.getList(p.getUniqueId(), InvMenuDataKey.CLICK);
		if (events == null) {
			return;
		}
		for (InventoryClickConsumer e : events) {
			MenuEventException.accept(p, "Inventory menu: ClickEvent", e, ev);
		}

	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onInventoryDrag(InventoryDragEvent ev) {
		if (!(ev.getWhoClicked() instanceof Player p)) {
			return;
		}
		List<InventoryDragConsumer> events = ListData.getList(p.getUniqueId(), InvMenuDataKey.DRAG);
		if (events == null) {
			return;
		}
		for (InventoryDragConsumer e : events) {
			MenuEventException.accept(p, "Inventory menu: DragEvent", e, ev);
		}

	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onInventoryOpen(InventoryOpenEvent ev) {
		if (!(ev.getPlayer() instanceof Player p)) {
			return;
		}
		Runnable runnable = SingleData.remove(p.getUniqueId(), InvMenuDataKey.TO_OPEN);
		if (runnable == null) {
			return;
		}

		MenuEventException.run(p, "Inventory menu: OpenEvent", runnable);
		runnable.run();
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onInventoryClose(InventoryCloseEvent ev) {
		if (!(ev.getPlayer() instanceof Player p)) {
			return;
		}
		UUID uuid = p.getUniqueId();
		List<InventoryCloseConsumer> events = ListData.remove(uuid, InvMenuDataKey.CLOSE);
		if (events == null) {
			return;
		}
		List<NamespacedKey> keys = ListData.remove(uuid, InvMenuDataKey.TIMER);
		ListData.remove(uuid, InvMenuDataKey.CLICK);
		ListData.remove(uuid, InvMenuDataKey.DRAG);
		if (keys != null) {
			for (NamespacedKey key : keys) {
				Timer.stop(key);
			}
		}
		for (InventoryCloseConsumer e : events) {
			MenuEventException.accept(p, "Inventory menu: CloseEvent", e, ev);
		}
	}
}
