package net.logiench.shardCore.data.item.module.rarity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.logiench.shardCore.core.item.base.def.ShardItem;
import net.logiench.shardCore.core.item.base.module.ItemModule;
import net.logiench.shardCore.core.item.base.module.LoreProvider;
import net.logiench.shardCore.core.item.base.module.tools.LoreSection;
import net.logiench.shardCore.core.item.base.module.tools.StructuredLore;
import net.logiench.shardCore.core.item.system.module.context.GenerationContext;
import org.jetbrains.annotations.Nullable;

@Singleton
public class RarityModule implements ItemModule<ShardItem> {

	private final RarityLogic logic;

	@Inject
	private RarityModule() {
		this.logic = new RarityLogic();
	}


	@Override
	public Class<ShardItem> getTargetType() {
		return ShardItem.class;
	}

	@Override
	public String getModuleKey() {
		return "rarity";
	}

	@Override
	public @Nullable LoreProvider<ShardItem> getLoreProvider() {
		return logic;
	}


	private static class RarityLogic implements LoreProvider<ShardItem> {

		private RarityLogic() {
		}

		@Override
		public void updateLore(StructuredLore structuredLore, GenerationContext<? extends ShardItem> context) {
			ShardItem data = context.getData();
			structuredLore.add(LoreSection.RARITY,
				data.getRarity().getComponent()
					.appendSpace()
					.append(data.getItemType().getItemTypeName())
			);
		}
	}
}
