package net.logiench.shardCore.core.skill.system;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.logiench.shardCore.core.skill.base.ActiveSkill;
import net.logiench.shardCore.core.skill.base.SkillDefinition;
import net.logiench.shardCore.register.SkillRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class SkillManager implements Listener {
	/// 実行中のスキルとその定義元のセット
	private final Map<UUID, List<SkillDefAndActive>> activeSkills = new ConcurrentHashMap<>();
	/// クールダウンが終了するtick
	private final Map<UUID, Map<String, Integer>> cooldowns = new ConcurrentHashMap<>();
	private final SkillRegistry skillRegistry;

	@Inject
	private SkillManager(SkillRegistry skillRegistry) {
		this.skillRegistry = skillRegistry;
	}

	public boolean castSkill(@NotNull SkillContext context, @NotNull Class<? extends SkillDefinition> skillDefClass) {
		SkillDefinition def = skillRegistry.get(skillDefClass);
		if (def == null) {
			return false;
		}
		return castSkill(context, def);
	}

	public boolean castSkill(@NotNull SkillContext context, @NotNull SkillDefinition skillDef) {
		// スキルの実行可能チェック
		if (!skillDef.canCast(context, this)) {
			return false;
		}

		// 実行インスタンスの生成
		ActiveSkill instance = skillDef.createInstance(context);

		// マナ消費
		//	todo		character.setMana(character.getMana() - skillDef.getManaCost());

		// クールダウン設定 (サーバーの現在Tick + CD時間)
		UUID playerId = context.getUniqueId();
		setCooldown(playerId, skillDef);

		// start処理。ここでスキルがtick処理を要求するか確認
		boolean requireTick = instance.start();

		// tick処理が必要なスキルならリストに登録
		if (requireTick) {
			activeSkills.computeIfAbsent(playerId, k -> new ArrayList<>())
				.add(new SkillDefAndActive(skillDef, instance));
		} else {
			//そうでなければその瞬間に終了する
			setSkillFinish(instance);
		}
		return true;
	}

	public void onTick() {
		for (Map.Entry<UUID, List<SkillDefAndActive>> entry : activeSkills.entrySet()) {
			Iterator<SkillDefAndActive> iterator = entry.getValue().iterator();
			while (iterator.hasNext()) {
				ActiveSkill skill = iterator.next().activeSkill();

				// tick処理を実行し、終了条件を満たしたら終了処理をしてリストから外す
				if (!skill._tick()) {
					setSkillFinish(skill);
					iterator.remove();
				}
				// 内部的に強制キャンセルが発生したらキャンセル処理を行いその瞬間に削除する
				if (skill.isCancel()) {
					setSkillCancel(skill, ActiveSkill.CancelReason.SELF);
					iterator.remove();
				}
			}
		}
	}

	public void setCooldown(UUID playerId, SkillDefinition def) {
		if (def.getCooldownTicks() <= 0) {
			return;
		}
		int expirationTick = Bukkit.getCurrentTick() + def.getCooldownTicks();
		cooldowns.computeIfAbsent(playerId, k -> new HashMap<>()).put(def.getId(), expirationTick);
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent ev) {
		Player player = ev.getPlayer();
		setSkillCancel(activeSkills.remove(player.getUniqueId()), ActiveSkill.CancelReason.DEATH);
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent ev) {
		Player player = ev.getPlayer();

		// 発動中のものも、stopは呼び出さずキャンセルする
		setSkillCancel(activeSkills.remove(player.getUniqueId()), ActiveSkill.CancelReason.QUIT);
		Map<String, Integer> playerCds = cooldowns.remove(player.getUniqueId());
		if (playerCds == null || playerCds.isEmpty()) {
			return;
		}

		int currentTick = Bukkit.getCurrentTick();

		// ★ 各スキルの「残りTick数」を計算してPDCに保存する
		for (Map.Entry<String, Integer> entry : playerCds.entrySet()) {
			String skillId = entry.getKey();
			int expirationTick = entry.getValue();

			int remainingTicks = expirationTick - currentTick;

			if (remainingTicks > 0) {
				// TODO: PDC(またはShardLibのデータ)に、"cd_<skillId>" : remainingTicks を保存
				saveToPDC(player, skillId, remainingTicks);
			}
		}
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent ev) {
		Player player = ev.getPlayer();

		// TODO: PDCから "cd_<skillId>" の一覧を読み出す
		Map<String, Integer> savedCooldowns = loadFromPDC(player);

		int currentTick = Bukkit.getCurrentTick();

		// ★ 保存されていた「残りTick数」を、現在のTickに足して再設定する！
		for (Map.Entry<String, Integer> entry : savedCooldowns.entrySet()) {
			String skillId = entry.getKey();
			int remainingTicks = entry.getValue();

			// これにより、オフラインだった時間は一切CD回復にカウントされない
			int newExpirationTick = currentTick + remainingTicks;

			cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
				.put(skillId, newExpirationTick);
		}

		// 読み終わったPDCのデータは綺麗に消しておく
		clearPDC(player);
	}

	private void setSkillFinish(@NotNull ActiveSkill skillInstance) {
		skillInstance.onFinish();
		skillInstance.cleanup();
	}

	private void setSkillCancel(@Nullable List<SkillDefAndActive> skills, ActiveSkill.CancelReason reason) {
		if (skills == null) {
			return;
		}
		for (SkillDefAndActive activeData : skills) {
			setSkillCancel(activeData.activeSkill, reason);
		}
	}

	private void setSkillCancel(@Nullable ActiveSkill skillInstance, ActiveSkill.CancelReason reason) {
		if (skillInstance == null) {
			return;
		}
		skillInstance.onCancel(reason);
		skillInstance.cleanup();
	}

	// SkillDefinitionでのチェック用

	public boolean isOnCooldown(UUID playerId, String skillId/*このSkillIdだけど、SkillDefにしたほうが安全だよなーって*/) {
		Map<String, Integer> playerCds = cooldowns.get(playerId);
		if (playerCds == null || !playerCds.containsKey(skillId)) {
			return false;
		}

		int expirationTick = playerCds.get(skillId);
		if (Bukkit.getCurrentTick() >= expirationTick) {
			playerCds.remove(skillId); // 時間が過ぎていれば削除
			return false;
		}
		return true;
	}

	public boolean isActive(UUID playerId, String skillId) {
		List<SkillDefAndActive> playerActiveSkills = activeSkills.get(playerId);
		if (playerActiveSkills == null || playerActiveSkills.isEmpty()) {
			return false;
		}
		return playerActiveSkills.stream().anyMatch(skill -> skill.skillDef().getId().equals(skillId));
	}

	// このあたりは別のとこに

	private void saveToPDC(Player player, String skillId, int remainingTicks) {
	}

	private void clearPDC(Player player) {
	}

	private Map<String, Integer> loadFromPDC(Player player) {
		return Map.of();
	}

	private record SkillDefAndActive(SkillDefinition skillDef, ActiveSkill activeSkill) {}
}
