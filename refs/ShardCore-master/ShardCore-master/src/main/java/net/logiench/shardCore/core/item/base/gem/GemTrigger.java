package net.logiench.shardCore.core.item.base.gem;

import net.logiench.shardCore.core.item.system.gem.context.AttackGemContext;
import net.logiench.shardCore.core.item.system.gem.context.GemContext;

public interface GemTrigger {
	Trigger<AttackGemContext> ON_ATTACK = new Trigger<>(AttackGemContext.class);

	record Trigger<C extends GemContext>(Class<C> contextClass) {
	}
}
