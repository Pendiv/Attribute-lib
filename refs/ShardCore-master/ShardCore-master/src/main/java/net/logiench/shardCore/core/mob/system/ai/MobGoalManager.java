package net.logiench.shardCore.core.mob.system.ai;

import com.destroystokyo.paper.entity.ai.MobGoals;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.logiench.shardCore.core.mob.base.ShardMob;
import net.logiench.shardCore.core.mob.system.loader.MobLoader;
import net.logiench.shardCore.register.MobRegistry;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

@Singleton
public class MobGoalManager {
	private final MobRegistry mobRegistry;

	@Inject
	public MobGoalManager(MobRegistry mobRegistry) {
		this.mobRegistry = mobRegistry;
	}

	/**
	 * モブが持つIDをもとに定義クラスを取得し、Goalを設定します。
	 *
	 * @param loader モブのローダー
	 */
	public void setMobGoals(@Nullable MobLoader loader) {
		if (loader == null) {
			return;
		}
		ShardMob data = mobRegistry.get(loader.getId());
		if (data == null) {
			return;
		}
		Entity entity = loader.getLoadedEntity();
		if (!(entity instanceof Mob mob)) {
			return;
		}
		MobGoals goalManager = Bukkit.getMobGoals();
		// 指定されたNamespacedKeyをもとにフィルターをかけてGoalを削除
		Set<NamespacedKey> keyList = data.getRemoveGoals();
		goalManager.getAllGoals(mob).stream()
			.filter(goal -> keyList.contains(goal.getKey().getNamespacedKey()))
			.forEach(goal -> goalManager.removeGoal(mob, goal.getKey()));

		data.applyGoals(goalManager, mob);
	}
}
