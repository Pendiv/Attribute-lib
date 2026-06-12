package net.logiench.logienchlibv2.cacheKeys;

import net.logiench.logienchlibv2.LogienchLib;
import net.logiench.logienchlibv2.api.cache.key.player.ListKey;
import net.logiench.logienchlibv2.api.cache.key.player.SingleKey;
import org.bukkit.NamespacedKey;

import static net.logiench.logienchlibv2.api.minecraft.menu.inventory.InvMenuConsumers.*;

public interface InvMenuDataKey {
	SingleKey<Runnable> TO_OPEN = new SingleKey<>(LogienchLib.getInstance(), "i_open", Runnable.class);
	ListKey<InventoryClickConsumer> CLICK = new ListKey<>(LogienchLib.getInstance(), "i_click", InventoryClickConsumer.class);
	ListKey<InventoryDragConsumer> DRAG = new ListKey<>(LogienchLib.getInstance(), "i_drag", InventoryDragConsumer.class);
	ListKey<InventoryCloseConsumer> CLOSE = new ListKey<>(LogienchLib.getInstance(), "i_close", InventoryCloseConsumer.class);
	ListKey<NamespacedKey> TIMER = new ListKey<>(LogienchLib.getInstance(), "i_time", NamespacedKey.class);
}
