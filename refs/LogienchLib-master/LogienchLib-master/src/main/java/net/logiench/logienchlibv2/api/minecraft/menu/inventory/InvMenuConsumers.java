package net.logiench.logienchlibv2.api.minecraft.menu.inventory;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface InvMenuConsumers {
	interface InventoryOpenConsumer extends BiConsumer<Player, InventoryMenu> {
	}

	interface InventoryClickConsumer extends Consumer<InventoryClickEvent> {
	}

	interface InventoryDragConsumer extends Consumer<InventoryDragEvent> {
	}

	interface InventoryCloseConsumer extends Consumer<InventoryCloseEvent> {
	}

	interface InventoryConsumer {
		default @Nullable InventoryOpenConsumer getOpenConsumer() {
			return null;
		}

		default @Nullable InventoryClickConsumer getClickConsumer() {
			return null;
		}

		default @Nullable InventoryDragConsumer getDragConsumer() {
			return null;
		}

		default @Nullable InventoryCloseConsumer getCloseConsumer() {
			return null;
		}
	}
}
