package net.logiench.shardCore.core.item.base.gem;

import net.logiench.shardCore.core.item.system.gem.context.GemContext;
import org.jetbrains.annotations.Unmodifiable;

import java.util.HashMap;
import java.util.Map;

public class GemActionRegistry {
	private final Map<GemTrigger.Trigger<?>, GemAction<?>> gemActions = new HashMap<>();

	public <C extends GemContext> GemActionRegistry addListener(GemTrigger.Trigger<C> trigger, GemAction<C> action) {
		gemActions.put(trigger, action);
		return this;
	}

	@SuppressWarnings("unchecked")
	public <C extends GemContext> GemAction<C> getAction(GemTrigger.Trigger<C> trigger) {
		return (GemAction<C>) gemActions.get(trigger);
	}

	@Unmodifiable
	public Map<GemTrigger.Trigger<?>, GemAction<?>> getActions() {
		return Map.copyOf(gemActions);
	}
}
