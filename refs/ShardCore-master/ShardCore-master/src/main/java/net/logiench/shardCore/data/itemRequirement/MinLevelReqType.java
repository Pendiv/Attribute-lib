package net.logiench.shardCore.data.itemRequirement;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.logiench.shardCore.core.item.base.def.EquipmentItem;
import net.logiench.shardCore.core.item.system.module.context.CalculationContext;
import net.logiench.shardCore.core.itemRequirement.base.ItemRequirement;
import net.logiench.shardCore.core.itemRequirement.base.RequirementDef;
import net.logiench.shardCore.core.itemRequirement.base.RequirementResolver;
import net.logiench.shardCore.core.itemRequirement.base.RequirementType;
import net.logiench.shardLib.api.player.PlayerCharacterAPI;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.random.RandomGenerator;

@Singleton
public class MinLevelReqType extends RequirementType<Long> {
	public MinLevelReqType() {
		super(PersistentDataType.LONG, "min_level");
	}

	@Override
	public boolean check(@NonNull Player player, @NonNull PlayerCharacterAPI characterAPI, Long value) {
		return player.getLevel() >= value;
	}

	@Override
	public Component getLoreFormat(Long value) {
		return Component.text("最低レベル: ", NamedTextColor.GREEN).append(Component.text(value));
	}

	public record MinLevelDef(long min, long max) implements RequirementDef<MinLevelDef> {
		public MinLevelDef(long level) {
			this(level, level);
		}

		@Override
		public Class<? extends RequirementResolver<MinLevelDef, EquipmentItem>> getResolverType() {
			return MinLevelResolver.class;
		}
	}

	@Singleton
	public static class MinLevelResolver implements RequirementResolver<MinLevelDef, EquipmentItem> {

		private final MinLevelReqType type;

		@Inject
		private MinLevelResolver(MinLevelReqType type) {
			this.type = type;
		}

		@Override
		public Class<EquipmentItem> getContextDataType() {
			return EquipmentItem.class;
		}

		@Override
		public Collection<ItemRequirement<?>> resolver(RandomGenerator random, MinLevelDef def, CalculationContext<? extends EquipmentItem> context) {
			return List.of(
				new ItemRequirement<>(type, random.nextLong(def.min(), def.max() + 1))
			);
		}
	}
}
