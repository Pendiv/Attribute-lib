package net.logiench.shardCore.data.item.module.level;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.logiench.shardCore.core.item.base.def.EquipmentItem;
import net.logiench.shardCore.core.item.base.module.*;
import net.logiench.shardCore.core.item.system.loader.ItemLoader;
import net.logiench.shardCore.core.item.system.module.context.*;
import net.logiench.shardCore.core.item.system.module.params.UpdateParameters;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.random.RandomGenerator;

@Singleton
public class LevelModule implements ItemModule<EquipmentItem> {

	private final LevelLogic logic;

	@Inject
	private LevelModule() {
		this.logic = new LevelLogic();
	}

	@Override
	public Class<EquipmentItem> getTargetType() {
		return EquipmentItem.class;
	}

	@Override
	public String getModuleKey() {
		return "level";
	}

	@Override
	public @Nullable ItemReader<EquipmentItem> getReader() {
		return logic;
	}

	@Override
	public @Nullable ItemStatsUpdater<EquipmentItem> getUpdater() {
		return logic;
	}

	@Override
	public @Nullable ItemStatsCalculator<EquipmentItem> getCalculator() {
		return logic;
	}

	@Override
	public @Nullable ItemProcessor<EquipmentItem> getProcessor() {
		return logic;
	}


	private static class LevelLogic implements ItemReader<EquipmentItem>, ItemStatsUpdater<EquipmentItem>, ItemStatsCalculator<EquipmentItem>, ItemProcessor<EquipmentItem> {

		private LevelLogic() {
		}

		@Override
		public void read(@NonNull ItemLoader loader, @NonNull EquipmentItem data, @NonNull ReadContext context) {
			Long level = loader.getLoadedItem().getItemData(LevelKeys.PDC_LEVEL);
			if (level == null) {
				throw new IllegalArgumentException("[LevelModule] アイテムからレベルを取得できません");
			}
			context.put(LevelKeys.CTX_LEVEL, level);
		}

		@Override
		public void update(UpdateContext<? extends EquipmentItem> context) {
			Long level = context.get(LevelKeys.CTX_LEVEL);
			if (level == null) {
				throw new IllegalArgumentException("[LevelModule] レベルのデータが存在しません");
			}
			UpdateParameters params = context.getUParams();
			long set = params.get(LevelKeys.UDT_SET_LEVEL, level);
			long add = params.get(LevelKeys.UDT_ADD_LEVEL, 0L);

			long newLevel = set + add;
			context.put(LevelKeys.CTX_LEVEL, newLevel);
			context.editGParams(p -> p.put(LevelKeys.GEN_LEVEL, newLevel));
		}

		@Override
		public void calculate(RandomGenerator random, CalculationContext<? extends EquipmentItem> context) {
			Long level = context.getGParams().get(LevelKeys.GEN_LEVEL, 0L);
			context.put(LevelKeys.CTX_LEVEL, level);
		}

		@Override
		public void process(GenerationContext<? extends EquipmentItem> context) {
			Long level = context.get(LevelKeys.CTX_LEVEL);
			if (level == null) {
				throw new IllegalStateException("[LevelModule] レベルのデータが存在しません");
			}
			context.getItem().setItemData(LevelKeys.PDC_LEVEL, level);
		}

		@Override
		public int checksum(BaseContext context) {
			return context.get(LevelKeys.CTX_LEVEL, 0L).hashCode();
		}
	}
}
