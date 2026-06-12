package net.logiench.shardCore.core.menu.main;

import com.google.inject.Inject;
import net.kyori.adventure.text.Component;
import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import net.logiench.logienchlibv2.api.minecraft.time.Timer;
import net.logiench.shardCore.core.item.system.generator.ItemGenerator;
import net.logiench.shardCore.core.menu.util.SimpleMenu;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.atomic.AtomicInteger;

public class AppraisalMenu extends SimpleMenu {
	private ItemStack clickedItem;
	@Inject
	private ItemGenerator itemGenerator;

	public AppraisalMenu(Player player) {
		super(player);
	}

	@Override
	protected void initMenu() {
		menu.addClickListener(ev -> {
			ev.setCancelled(true);
			Inventory clickedInventory = ev.getClickedInventory();
			if (clickedInventory == null || clickedInventory.getType() == InventoryType.PLAYER) {
				this.clickedItem = ev.getCurrentItem();
				menu.setItem(4, clickedItem);
				return;
			}
			if (ev.getSlot() != 13) {
				return;
			}
			if (clickedItem == null) {
				return;
			}
			final int baseTime = 0;
			AtomicInteger aa = new AtomicInteger(0);
			AtomicInteger fib2 = new AtomicInteger(1);
			Timer.startTimer(i -> {
				if (i + 1 == 70) {
					SuperItemStack item = itemGenerator.appraise(SuperItemStack.safeInit(clickedItem), null).item();
					menu.setItem(13, item);
					player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
					return;
				}
				if (i > baseTime) {
					if (i - baseTime != fib2.get()) {
						return;
					}
					fib2.addAndGet(Math.ceilDiv(fib2.get(), 14));
				}
				Material[] items = {Material.LEATHER_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE, Material.IRON_CHESTPLATE, Material.GOLDEN_CHESTPLATE, Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE};
				menu.setItem(13, new ItemStack(items[aa.getAndIncrement()]));
				if (aa.get() >= items.length) {
					aa.set(0);
				}
				player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
			}, 1, 1, 70);
		});
	}

	@Override
	public Component getTitle() {
		return Component.text("Appraisal");
	}

	@Override
	public int getSize() {
		return 27;
	}
}
