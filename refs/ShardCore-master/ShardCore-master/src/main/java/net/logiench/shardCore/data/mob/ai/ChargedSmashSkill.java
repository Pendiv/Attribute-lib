package net.logiench.shardCore.data.mob.ai;

import com.destroystokyo.paper.entity.ai.GoalType;
import net.logiench.shardCore.ShardCore;
import net.logiench.shardCore.core.mob.base.AbstractSkillGoal;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class ChargedSmashSkill extends AbstractSkillGoal<Mob> {

	private LivingEntity target;
	private static final int SKILL_DURATION = 15; // スキルの持続時間(tick)

	public ChargedSmashSkill(Mob mob) {
		// クールダウン100tick(5秒), ウォームアップ40tick(2秒)
		super(mob, new NamespacedKey(ShardCore.getInstance(), "charged_smash"), 100, 40);
	}

	// ★重要: スキル実行中（力を溜めている時）は歩き回らないようにする
	@Override
	@NotNull
	public EnumSet<GoalType> getTypes() {
		return EnumSet.of(GoalType.MOVE, GoalType.TARGET);
	}

	@Override
	public boolean _shouldActivate() {
		target = mob.getTarget();
		if (target == null || !target.isValid()) {
			return false;
		}

		// ターゲットが5ブロック以内(25.0)に近づいたら発動！
		return mob.getLocation().distanceSquared(target.getLocation()) <= 25.0;
	}

	@Override
	public boolean shouldStayActive() {
		// ウォームアップ中、またはスキルの持続時間(activeTicks)が規定値に達するまでGoalを維持する
		return getCurrentWarmup() > 0 || activeTicks() < SKILL_DURATION;
	}

	// ==========================================
	// ウォームアップ（予備動作）フェーズ
	// ==========================================
	@Override
	public void warmupStart() {
		mob.getPathfinder().stopPathfinding();
		// ドラゴンの唸り声でプレイヤーに警告
		mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.2f);
	}

	@Override
	public void warmupTick() {
		// 足元に炎のパーティクルを集めて「力を溜めている感」を出す
		mob.getWorld().spawnParticle(Particle.FLAME, mob.getLocation().add(0, 0.2, 0), 3, 0.5, 0.1, 0.5, 0.02);
	}

	@Override
	public void warmupStop() {
		// 必要であればここでパーティクルを消す等の処理
	}

	// ==========================================
	// スキル（発動）フェーズ
	// ==========================================
	@Override
	public void skillStart() {
		// 発動した瞬間に爆発音とド派手なエフェクト
		mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
		mob.getWorld().spawnParticle(Particle.EXPLOSION, mob.getLocation().add(0, 1, 0), 1);

		// 周囲4ブロック以内のエンティティに範囲ダメージ！
		mob.getNearbyEntities(4, 4, 4).forEach(entity -> {
			if (entity instanceof LivingEntity le && !entity.equals(mob)) {
				// ※ ここは後々 CalculateDamage に置き換える
				le.damage(15.0, mob);
			}
		});
	}

	@Override
	public void skillTick() {
		// スキル持続中（15tickの間）、地面が燃えているような余韻のパーティクルを出す
		mob.getWorld().spawnParticle(Particle.LAVA, mob.getLocation().add(0, 0.5, 0), 2, 1.0, 0.2, 1.0, 0.0);
	}

	@Override
	public void skillStop() {
		// ターゲットの参照を外してメモリリークを防止
		target = null;
	}
}
