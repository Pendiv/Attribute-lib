package net.logiench.shardCore.register;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import net.logiench.shardCore.core.stats.base.AttributeEnum;
import net.logiench.shardCore.data.stats.keys.CoreStats;
import net.logiench.shardCore.data.stats.keys.item.ItemStats;
import net.logiench.shardCore.data.stats.keys.mob.MobStats;
import net.logiench.shardCore.data.stats.keys.player.PlayerStats;
import net.logiench.shardCore.data.stats.keys.player.PlayerStatsOverride;
import net.logiench.shardLib.api.ShardLibProvider;
import net.logiench.shardLib.api.register.attribute.AttributeDefinitionRegister;

import java.util.*;
import java.util.stream.Stream;

@Singleton
public class StatsRegistry {
	@Getter
	private final Map<String, AttributeEnum> statsMap;

	@Inject
	public StatsRegistry() {
		Map<String, AttributeEnum> statsMap = new HashMap<>();

		// 以下 ステータスEnumの登録

		add(statsMap, CoreStats.class, Target.CORE);

		add(statsMap, PlayerStats.class, Target.PLAYER);
		add(statsMap, PlayerStatsOverride.class, Target.PLAYER);

		add(statsMap, MobStats.class, Target.MOB_CORE);

		add(statsMap, ItemStats.class);

		// ここまで

		this.statsMap = Collections.unmodifiableMap(statsMap);
	}

	public AttributeEnum get(String attribute) {
		return statsMap.get(attribute);
	}


	private static void add(Map<String, AttributeEnum> statsMap, Class<?> attributesClass) {
		getValuesAndPut(statsMap, attributesClass);
	}

	/**
	 * 指定されたクラスのステータスを targetに登録し、statsMapに格納します
	 */
	private static void add(Map<String, AttributeEnum> statsMap, Class<?> attributesClass, Target target) {
		AttributeDefinitionRegister register = switch (target) {
			case CORE -> ShardLibProvider.get().getRegister().attribute().coreAttribute();
			case PLAYER -> ShardLibProvider.get().getRegister().player().attributes();
			case MOB_CORE -> ShardLibProvider.get().getRegister().mob().coreAttributes();
		};

		for (AttributeEnum key : getValuesAndPut(statsMap, attributesClass)) {
			register.register(key.toAttributeDefinition());
		}
	}

	private static void addMob(Map<String, AttributeEnum> statsMap, Class<?> attributesClass, String target) {
		AttributeDefinitionRegister register = ShardLibProvider.get().getRegister().mob().attributes().registerFor(target);
		for (AttributeEnum key : getValuesAndPut(statsMap, attributesClass)) {
			register.register(key.toAttributeDefinition());
		}
	}

	private static List<AttributeEnum> getValuesAndPut(Map<String, AttributeEnum> statsMap, Class<?> attributesClass) {
		return Arrays.stream(attributesClass.getFields())
			// フィールドの型がAttributeEnumのもののみ
			.filter(field -> field.getType().equals(AttributeEnum.class))
			.flatMap(field -> {
				try {
					// staticだからどのクラスを経由でアクセスするかはnull
					if (field.get(null) instanceof AttributeEnum key) {
						statsMap.put(key.getId(), key);
						return Stream.of(key);
					}
				} catch (IllegalArgumentException | IllegalAccessException ignored) {
				}
				return Stream.empty();
			}).toList();
	}

	private enum Target {
		CORE,
		PLAYER,
		MOB_CORE
	}
}
