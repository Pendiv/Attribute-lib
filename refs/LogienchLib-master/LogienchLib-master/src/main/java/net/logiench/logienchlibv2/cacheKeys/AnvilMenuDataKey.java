package net.logiench.logienchlibv2.cacheKeys;

import net.logiench.logienchlibv2.LogienchLib;
import net.logiench.logienchlibv2.api.cache.key.player.SingleKey;

public interface AnvilMenuDataKey {
	SingleKey<String> INPUT_TEXT = new SingleKey<>(LogienchLib.getInstance(), "a_text", String.class);
	//	ListKey<AnvilMenuConsumers.AnvilEnterConsumer> ENTER = new ListKey<>(LogienchLib.getInstance(), "a_enter", AnvilMenuConsumers.AnvilEnterConsumer.class);
}
