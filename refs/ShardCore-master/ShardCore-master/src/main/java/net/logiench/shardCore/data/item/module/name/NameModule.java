package net.logiench.shardCore.data.item.module.name;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.logiench.shardCore.core.item.base.def.ShardItem;
import net.logiench.shardCore.core.item.base.module.ItemModule;
import net.logiench.shardCore.core.item.base.module.ItemProcessor;
import net.logiench.shardCore.core.item.system.module.context.BaseContext;
import net.logiench.shardCore.core.item.system.module.context.GenerationContext;
import net.logiench.shardCore.data.item.module.prefix.PrefixKeys;
import net.logiench.shardCore.data.prefix.Prefix;
import org.jetbrains.annotations.Nullable;

@Singleton
public class NameModule implements ItemModule<ShardItem> {

	private final NameLogic logic;

	@Inject
	private NameModule() {
		this.logic = new NameLogic();
	}

	@Override
	public Class<ShardItem> getTargetType() {
		return ShardItem.class;
	}

	@Override
	public String getModuleKey() {
		return "name";
	}

	@Override
	public @Nullable ItemProcessor<ShardItem> getProcessor() {
		return logic;
	}


	private static class NameLogic implements ItemProcessor<ShardItem> {

		private NameLogic() {
		}

		@Override
		public void process(GenerationContext<? extends ShardItem> context) {
			Prefix prefix = context.get(PrefixKeys.CTX_PREFIX);
			Component name = context.getData().getName();
			if (prefix != null) {
				name = prefix.getName()
					.appendSpace()
					.append(name);
			}
			context.getItem().name(name);
		}

		@Override
		public int checksum(BaseContext context) {
			return 0;
		}
	}
}
