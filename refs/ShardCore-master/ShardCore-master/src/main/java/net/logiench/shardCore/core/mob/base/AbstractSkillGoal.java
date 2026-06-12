package net.logiench.shardCore.core.mob.base;

import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractSkillGoal<T extends Mob> extends AbstractGoal<T> {
	private final int cooldown;
	private final int warmup;

	@Getter
	private int currentCooldown = 0;
	@Getter
	private int currentWarmup = 0;

	/**
	 * 以下の順序で実行されます。cooldownはwarmupが終了したら開始されます。
	 * <ol>
	 *     <li>warmup</li>
	 *     <li>skill</li>
	 *     <li>cooldown</li>
	 * </ol>
	 *
	 * @param warmup   スキルが実行される前の事前動作tickを指定します。0が指定された場合はwarmup系のメソッドは実行されません
	 * @param cooldown スキルが発動してから次のGoal呼び出しが始まるまでのtick
	 */
	public AbstractSkillGoal(@NotNull T mob, @NotNull NamespacedKey key, int cooldown, int warmup) {
		super(mob, key);
		this.cooldown = cooldown;
		this.warmup = warmup;
	}

	@Override
	public final boolean shouldActivate() {
		if (currentCooldown > 0) {
			currentCooldown--;
			return false;
		}
		return _shouldActivate();
	}

	@Override
	public final void start() {
		if (warmup == 0) {
			skillStart();
		} else {
			currentWarmup = warmup;
			warmupStart();
		}
	}

	@Override
	public final void _tick() {
		if (currentWarmup > 0) {
			currentWarmup--;

			if (currentWarmup == 0) {
				warmupStop();
				currentCooldown = cooldown;
				skillStart();
			} else {
				warmupTick();
			}
			return;
		}
		if (currentCooldown > 0) {
			currentCooldown--;
		}
		skillTick();
	}

	@Override
	public final void _stop() {
		// エラー時もリセットできないと事故になりかねない物だけ一応囲んでおく
		try {
			skillStop();
		} finally {
			currentWarmup = 0;
		}
	}

	public abstract boolean _shouldActivate();

	@Override
	public abstract boolean shouldStayActive();

	public abstract void warmupStart();

	public abstract void warmupTick();

	public abstract void warmupStop();

	public abstract void skillStart();

	public abstract void skillTick();

	public abstract void skillStop();
}
