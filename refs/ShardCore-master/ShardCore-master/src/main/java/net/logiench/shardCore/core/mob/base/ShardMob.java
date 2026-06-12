package net.logiench.shardCore.core.mob.base;

import com.destroystokyo.paper.entity.ai.MobGoals;
import net.kyori.adventure.text.Component;
import net.logiench.shardCore.core.stats.base.AttributeEnum;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Map;
import java.util.Set;


public interface ShardMob {
	String getId();

	String getAttributeProfileId();

	@NotNull
	EntityType getEntityType();

	@NotNull
	Component getName();

	/**
	 * レベル1での最大体力を定義します。HPはレベルに応じてスケーリングされます。
	 * スケーリングの式は {@link net.logiench.shardCore.data.stats.keys.CoreStats#MAX_HP}
	 * 召喚時には計算後の最大体力が実際のHPとなります。
	 * MobのHPを確実に指定させるために{@link #getStats()}から分離しています。この内容はgetStatsの結果を上書きします。
	 */
	double getMaxHp();

	/**
	 * レベル1でのステータスを定義します。
	 * そのステータスのスケーリング式に従い、モブのレベルに応じてスケーリングされます。
	 */
	@NotNull
	@Unmodifiable
	Map<AttributeEnum, Double> getStats();

	@Nullable
	default Map<EquipmentSlot, Integer> getEquipment() {
		return null;
	}

	/**
	 * Minecraftデフォルトで搭載されているAI(Goal)のうち、削除するもののキーのリストを取得します。
	 * これはGoalを容易に削除することを目的としており、NamespacedKeyが重複している場合はすべて削除されます。
	 */
	@NotNull
	@Unmodifiable
	default Set<NamespacedKey> getRemoveGoals() {
		return Set.of();
	}

	/**
	 * 新たに搭載するMob-AI(Goal)を適応します。
	 * 重複して登録しないように処理を作成してください。
	 * 対象の限られたGoalも指定できるよう、{@link EntityType#getEntityClass()}
	 * で取得できるクラスにキャストして使用することをお勧めします。
	 * <br>例: <code>if (!(mob instanceof Zombie zombie)) return;</code>
	 *
	 * @param goals {@link Bukkit#getMobGoals()}
	 * @param mob   召喚されたShardMobの実体
	 */
	default void applyGoals(@NotNull MobGoals goals, @NotNull Mob mob) {
	}

	/**
	 * {@link net.logiench.shardCore.register.MobLootTableRegistry}に登録されたLootTableを呼び出すためのIdです。
	 * nullの場合、ドロップはありません
	 */
	@Nullable
	String getLootTableId();
}
