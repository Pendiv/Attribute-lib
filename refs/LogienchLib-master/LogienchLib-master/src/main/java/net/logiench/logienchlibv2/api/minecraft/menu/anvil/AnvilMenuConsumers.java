package net.logiench.logienchlibv2.api.minecraft.menu.anvil;

import net.logiench.logienchlibv2.api.minecraft.menu.inventory.InvMenuConsumers;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface AnvilMenuConsumers extends InvMenuConsumers {
	interface AnvilEnterConsumer extends Consumer<AnvilEnterEvent> {
	}

	interface InventoryConsumer extends InvMenuConsumers.InventoryConsumer {
		default @Nullable AnvilEnterConsumer getAnvilEnterConsumer() {
			return null;
		}
	}
}
