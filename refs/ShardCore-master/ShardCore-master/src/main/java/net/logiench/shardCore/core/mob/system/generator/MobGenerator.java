package net.logiench.shardCore.core.mob.system.generator;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.logiench.logienchlibv2.api.minecraft.data.ContainerKey;
import net.logiench.logienchlibv2.api.minecraft.data.DataContainer;
import net.logiench.shardCore.ShardCore;
import net.logiench.shardCore.core.mob.base.ShardMob;
import net.logiench.shardCore.core.mob.system.ai.MobGoalManager;
import net.logiench.shardCore.core.mob.system.loader.MobLoader;
import net.logiench.shardCore.core.stats.base.AttributeEnum;
import net.logiench.shardCore.data.stats.keys.CoreStats;
import net.logiench.shardCore.register.MobRegistry;
import net.logiench.shardLib.api.ShardLibProvider;
import net.logiench.shardLib.api.attribute.AttributeAPI;
import net.logiench.shardLib.api.mob.MobAPI;
import net.logiench.shardLib.api.mob.MobCharacterAPI;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

@Singleton
public class MobGenerator {
	public static final ContainerKey<String, String> MOB_ID = new ContainerKey<>(PersistentDataType.STRING, ShardCore.getInstance(), "id");
	private static final MobAPI MOB_API = ShardLibProvider.get().getMobAPI();
	private static final AttributeEnum MAX_HP = CoreStats.MAX_HP;
	private static final AttributeEnum HP = CoreStats.HP;

	private final MobRegistry mobRegistry;
	private final MobGoalManager goalManager;

	@Inject
	private MobGenerator(MobRegistry mobRegistry, MobGoalManager goalManager) {
		this.mobRegistry = mobRegistry;
		this.goalManager = goalManager;
	}

	public <T extends ShardMob> SpawnResult spawn(@NotNull Class<? extends T> dataClass, @NotNull Location location, long level) {
		T data = mobRegistry.get(dataClass);
		if (data == null) {
			return new SpawnResult(SpawnResult.State.NOT_FOUND, null, null,
				"指定されたクラスはMobRegistryに登録されていません。 class: " + dataClass.getName());
		}
		return spawn(data, location, level);
	}

	public <T extends ShardMob> SpawnResult spawn(@NotNull T data, @NotNull Location location, long level) {
		Optional<Entity> optionalEntity = MOB_API.spawnEntity(location, data.getEntityType(), data.getAttributeProfileId());
		if (optionalEntity.isEmpty()) {
			return new SpawnResult(SpawnResult.State.NOT_FOUND, null, null,
				"ShardLibでのエンティティ召喚に失敗しました。 Id: " + data.getId());
		}
		if (!(optionalEntity.get() instanceof LivingEntity entity)) {
			return new SpawnResult(SpawnResult.State.ERROR, null, null,
				"召喚したエンティティはLivingEntityに変換できません。 Id: " + data.getId() + ", EntityType: " + optionalEntity.get().getType());
		}
		DataContainer container = new DataContainer(entity);
		container.set(MOB_ID, data.getId());

		Optional<MobCharacterAPI> optionalCharacter = MOB_API.getCharacterAPI(entity);
		if (optionalCharacter.isEmpty()) {
			// ここで召喚しているから通常ありえない
			throw new IllegalStateException("召喚したエンティティが異常な状態です。 Id: " + data.getId());
		}
		MobCharacterAPI character = optionalCharacter.get();
		AttributeAPI attribute = character.getAttributeAPI();

		for (Map.Entry<AttributeEnum, Double> entry : data.getStats().entrySet()) {
			attribute.setBaseAttribute(entry.getKey(), entry.getValue());
		}
		double maxHp = MAX_HP.getScalingValue(data.getMaxHp(), level);
		attribute.setBaseAttribute(MAX_HP, maxHp);
		attribute.setBaseAttribute(HP, maxHp);

		MobLoader loader = MobLoader.of(entity);
		goalManager.setMobGoals(loader);

		return new SpawnResult(SpawnResult.State.SUCCESS, entity, character, null);
	}
}
