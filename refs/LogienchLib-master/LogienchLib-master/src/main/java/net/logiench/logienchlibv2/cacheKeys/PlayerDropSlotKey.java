package net.logiench.logienchlibv2.cacheKeys;

import net.logiench.logienchlibv2.LogienchLib;
import net.logiench.logienchlibv2.api.cache.key.player.SingleKey;
import net.logiench.logienchlibv2.base.minecraft.event.DropSlotData;

public interface PlayerDropSlotKey {
	SingleKey<DropSlotData> DROP_SLOT = new SingleKey<>(LogienchLib.getInstance(), "slot", DropSlotData.class);
}
