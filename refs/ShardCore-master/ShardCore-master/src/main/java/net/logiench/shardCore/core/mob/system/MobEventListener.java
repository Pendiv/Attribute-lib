package net.logiench.shardCore.core.mob.system;

import net.logiench.shardCore.core.mob.system.ai.MobGoalManager;
import net.logiench.shardCore.core.mob.system.loader.MobLoader;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;

public class MobEventListener implements Listener {

	private final MobGoalManager goalManager;

	private MobEventListener(MobGoalManager goalManager) {
		this.goalManager = goalManager;
	}

	/*
	モブがロードされたときにAIを復元する
	それ以前にアンロード時にモブを全て自動で削除するべきかもしれない
	 */
	@EventHandler
	private void onEntityLoad(EntitiesLoadEvent ev) {
		for (Entity entity : ev.getEntities()) {
			goalManager.setMobGoals(MobLoader.of(entity));
		}
	}
}
