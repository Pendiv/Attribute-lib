package net.logiench.logienchlibv2.base.minecraft.event;

import org.bukkit.inventory.Inventory;

public record DropSlotData(Inventory inventory, int slot) {
}