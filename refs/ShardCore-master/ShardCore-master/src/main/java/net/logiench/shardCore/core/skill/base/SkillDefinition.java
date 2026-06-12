package net.logiench.shardCore.core.skill.base;

import net.kyori.adventure.text.Component;
import net.logiench.shardCore.core.skill.system.SkillContext;
import net.logiench.shardCore.core.skill.system.SkillManager;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public abstract class SkillDefinition {
	public static final int MIN_SKILL_LEVEL = 1;

	public SkillDefinition() {
		if (getMinLevel() > getMaxLevel()) {
			throw new IllegalStateException(getClass().getSimpleName() + " のスキルの最大レベルが " + getMinLevel() + " を下回っています: " + getMaxLevel());
		}
	}

	/**
	 * このチェックは{@link #createInstance(SkillContext)}が行われる前に必ず実行されます。
	 *
	 * @param context スキルを実行するための情報
	 * @param manager このスキルを呼び出したSkillManager。発動可能な状態にあるかの確認に使用します。
	 * @return このスキルが実行可能な状態にあるか。trueの場合は実行可能、falseの場合は不可と判定されます。
	 */
	public boolean canCast(@NotNull SkillContext context, @NotNull SkillManager manager) {
		UUID playerId = context.getUniqueId();
		// スキルレベルが正常な範囲内で、マナを十分に持っていて
		return isInSkillLevel(context) && hasEnoughMana(context) &&
			// スキルの実行中ではなく、クールダウン中でもない
			!(manager.isActive(playerId, getId()) || manager.isOnCooldown(playerId, getId()));
	}

	// canCastをオーバーライドしたときに使うためのメソッド

	protected final boolean hasEnoughMana(@NotNull SkillContext context) {
		//		character.getMana() < getManaCost()
		return true;
	}

	protected final boolean isInSkillLevel(@NotNull SkillContext context) {
		return getMinLevel() <= context.level() && context.level() <= getMaxLevel();
	}

	// ここまで

	public final int getMinLevel() {
		return MIN_SKILL_LEVEL;
	}

	public int getMaxLevel() {
		return MIN_SKILL_LEVEL;
	}

	// --- インスタンスの生成 ---
	// ここで、このスキルタイプに対応する ActiveSkill を new して返す

	/**
	 * スキルのインスタンスを作成します。
	 * スキルの発動時に呼び出され、レベルによって呼び出すActiveSkillを変更することが可能です。
	 * これが呼び出される際、{@link #canCast(SkillContext, SkillManager)}がtrueであることが保証されます。
	 * 外部から異常なレベルで実行されないため、{@link #isInSkillLevel(SkillContext)}を呼び出し、エラーとすることを推奨します
	 * @param context スキルを実行するための情報
	 * @return スキルのインスタンス
	 */
	@NotNull
	public abstract ActiveSkill createInstance(SkillContext context) throws IllegalArgumentException;

	@NotNull
	public abstract String getId();

	@NotNull
	public abstract Component getDisplayName();

	public abstract double getManaCost();

	public int getCooldownTicks() {
		return 0;
	}
}
