package net.logiench.shardCore.core.skill.base;

import lombok.Getter;
import net.logiench.shardCore.core.skill.system.SkillContext;
import org.jetbrains.annotations.NotNull;

public abstract class ActiveSkill {
	/// スキルを発動したプレイヤーなどの情報
	protected final SkillContext context;
	/// スキルの実行が開始されてからのTick数
	private int activeTicks = 0;
	@Getter
	private boolean isCancel = false;

	public ActiveSkill(SkillContext context) {
		this.context = context;
	}

	/**
	 * スキルの実行が開始されてからのTick数を取得します。
	 */
	public int activeTicks() {
		return activeTicks;
	}

	/**
	 * スキルを実行中に強制的にキャンセルさせます。
	 * 失敗理由は{@link ActiveSkill.CancelReason#SELF}となり、{@link #onCancel(CancelReason)}が呼び出されます。
	 */
	public void setCancel() {
		isCancel = true;
	}

	// activeTicksを更新し、tickメソッドを動かす。処理自体はtickに書くが、外部からの呼び出しは_tickを使う
	public final boolean _tick() {
		activeTicks++;
		return tick();
	}

	/**
	 * スキルが呼び出されたときに1回だけ実行されます。
	 * 呼び出された瞬間、同一tickに実行します。
	 *
	 * @return このスキルの実行状態をtickへ移行するか。falseの場合はこのスキルは0tickで終了します。
	 * trueの場合は実行段階が{@link #tick()}に移行します。
	 */
	public abstract boolean start();

	/**
	 * スキルの実行中に呼び出されます。
	 * この呼び出しが何ティック目かは{@link #activeTicks}を参照してください。
	 * プレイヤーがtick継続中に退出した場合は
	 *
	 * @return 実行を継続するか。falseの場合は{@link #onFinish()}へ、trueの場合は再びtickが呼び出されます。
	 */
	protected abstract boolean tick();

	/**
	 * このスキルの実行が完了する際に1回だけ呼び出されます。
	 * 完了とは、startでfalseが返される、tickでfalseが返されるのいずれかです。
	 */
	public void onFinish() {
	}

	/**
	 * スキルの実行が完了せず、強制的に終了させられた際に呼び出されます。
	 *
	 * @param reason 終了した原因
	 */
	public void onCancel(@NotNull CancelReason reason) {
	}

	/**
	 * スキルの実行が終了した際に呼び出されます。
	 * これは<code>try catch finally</code>の<code>finally</code>にあたり、スキルの実行が終了する際必ず呼び出されます。
	 * {@link #onFinish()}や{@link #onCancel(CancelReason)}が呼び出された後に実行されます。
	 */
	public void cleanup() {
	}

	public enum CancelReason {
		DEATH,
		QUIT,
		SKILL,
		SELF,
		OTHER
	}
}
