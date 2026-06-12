package net.logiench.logienchlibv2.api.minecraft.item;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * プレイヤーにアイテムを与え、インベントリに入りきらなかったアイテムを地面にスポーンさせます
 */
@SuppressWarnings("unused")
public class GiveOrSpawn {

	public static void give(Player p, List<SuperItemStack> items) {
		give_(p, items.stream().map(SuperItemStack::build).toList());
	}

	public static void give_(Player p, List<ItemStack> items) {
		Location loc = p.getLocation();
		PlayerInventory inv = p.getInventory();
		for (ItemStack item : items) {
			give(loc, inv, item);
		}
	}

	public static void give(Player p, SuperItemStack... item) {
		give(p, Arrays.stream(item).map(SuperItemStack::build).toArray(ItemStack[]::new));
	}

	public static void give(Player p, ItemStack... items) {
		give(p.getLocation(), p.getInventory(), items);
	}


	public static void give(Location location, Inventory inventory, SuperItemStack... item) {
		give(location, inventory, Arrays.stream(item).map(SuperItemStack::build).toArray(ItemStack[]::new));
	}

	public static void give(Location location, Inventory inventory, ItemStack... items) {
		for (ItemStack item : items) {
			give(location, inventory, item);
		}
	}

	public static void give(Location location, Inventory inventory, List<SuperItemStack> item) {
		give_(location, inventory, item.stream().map(SuperItemStack::build).toList());
	}

	public static void give_(Location location, Inventory inventory, List<ItemStack> items) {
		for (ItemStack item : items) {
			give(location, inventory, item);
		}
	}

	private static void give(Location location, Inventory inventory, ItemStack item) {
		int amount = inventory.addItem(splitMaxStackSize(item)).values().stream().map(ItemStack::getAmount).reduce(0, Integer::sum);
		if (amount > 0) {
			ItemStack newItem = item.clone();
			newItem.setAmount(amount);
			for (ItemStack drop : splitMaxStackSize(newItem)) {
				location.getWorld().dropItem(location, drop);
			}
		}
	}

	public static ItemStack[] splitMaxStackSize(ItemStack... items) {
		List<ItemStack> result = new ArrayList<>();
		for (ItemStack item : items) {
			int max = item.getMaxStackSize();
			int amount = item.getAmount();
			if (max >= amount) {
				result.add(item);
				continue;
			}
			do {
				result.add(item.asQuantity(Math.min(max, amount)));
			} while ((amount -= max) > 0);
		}
		return result.toArray(ItemStack[]::new);
	}
}
