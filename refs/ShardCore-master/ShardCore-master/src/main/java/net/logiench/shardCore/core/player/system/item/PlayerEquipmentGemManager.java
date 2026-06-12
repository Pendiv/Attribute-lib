package net.logiench.shardCore.core.player.system.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.logiench.shardCore.core.item.base.def.GemItem;
import net.logiench.shardCore.core.item.base.gem.GemAction;
import net.logiench.shardCore.core.item.base.gem.GemActionRegistry;
import net.logiench.shardCore.core.item.base.gem.GemTrigger;
import net.logiench.shardCore.core.item.system.gem.context.GemContext;
import net.logiench.shardCore.core.item.system.loader.ItemInspector;
import net.logiench.shardCore.core.item.system.loader.ItemLoader;
import net.logiench.shardCore.core.item.system.module.context.ReadContext;
import net.logiench.shardCore.data.item.module.gem.GemKeys;
import net.logiench.shardCore.register.GemRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Singleton
public class PlayerEquipmentGemManager {

	private final Map<UUID, Map<GemTrigger.Trigger<?>, List<GemAction<?>>>> playerActiveActions = new HashMap<>();
	private final ItemInspector inspector;
	private final GemRegistry gemRegistry;

	@Inject
	private PlayerEquipmentGemManager(ItemInspector inspector, GemRegistry gemRegistry) {
		this.inspector = inspector;
		this.gemRegistry = gemRegistry;
	}

	public void applyItemLoaders(UUID playerId, List<ItemLoader> loaders) {
		applyItems(playerId, loaders.stream().map(inspector::inspect).toList());
	}

	public void applyItems(@NotNull UUID playerId, @NotNull List<ReadContext> itemContexts) {
		Map<GemTrigger.Trigger<?>, List<GemAction<?>>> activeActions = new HashMap<>();

		for (ReadContext itemContext : itemContexts) {
			List<GemItem> gemItems = itemContext.get(GemKeys.CTX_GEM_DATA);
			if (gemItems == null) {
				continue;
			}
			for (GemItem item : gemItems) {
				GemActionRegistry registry = gemRegistry.getActionRegistry(item.getClass());
				if (registry == null) {
					continue;
				}
				// ジェムが持っている全アクションを、トリガーごとにリストに詰める
				for (var entry : registry.getActions().entrySet()) {
					activeActions.computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
						.add(entry.getValue());
				}
			}
		}
		playerActiveActions.put(playerId, activeActions);
	}

	/**
	 * プレイヤーのジェムアクションをトリガーします
	 */
	@SuppressWarnings("unchecked")
	public <C extends GemContext> void doGemActions(GemTrigger.Trigger<C> trigger, UUID playerId, C context) {
		Map<GemTrigger.Trigger<?>, List<GemAction<?>>> cache = playerActiveActions.get(playerId);
		if (cache == null) {
			return;
		}

		List<GemAction<?>> actions = cache.get(trigger);
		if (actions == null) {
			return;
		}
		for (GemAction<?> action : actions) {
			((GemAction<C>) action).execute(context);
		}
	}

	public void quitPlayer(UUID playerId) {
		playerActiveActions.remove(playerId);
	}
}
