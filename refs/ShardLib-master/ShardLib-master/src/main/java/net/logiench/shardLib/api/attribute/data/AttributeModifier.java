package net.logiench.shardLib.api.attribute.data;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalLong;

/**
 * 単一のAttributeを対象とする、補正の情報を保持するクラス
 */
public final class AttributeModifier implements AttributeOperationModifier {
	@SerializedName("source")
	private final String sourceId;
	@SerializedName("target")
	private final String targetAttributeId;
	@SerializedName("op")
	private final ModifierOperation operation;
	@SerializedName("rule")
	private final StackingRule stackingRule;
	private final double value;
	private final transient long durationTicks;
	private final transient boolean persistent;

	/**
	 * 単一のAttributeを対象とする、補正の情報を作成します。
	 * persistentがfalseの場合、アンロード(退出)されると消滅し、<b>保存されません。</b>
	 * trueの場合は保存されます。
	 *
	 * @param sourceId          このModifierの源泉のID、StackingRuleを適応するために使用されます
	 * @param targetAttributeId 対象とするAttributeのID
	 * @param operation         対象に対しての値の計算方法
	 * @param stackingRule      同じ源泉から来たModifierの場合に使用され、後に来たModifierのルールが適応されます
	 * @param value             補正する値
	 * @param durationTicks     この補正の適応される時間(Tick)
	 * @param persistent        このModifierを保存するか
	 */
	public AttributeModifier(@NotNull String sourceId, @NotNull String targetAttributeId, @NotNull ModifierOperation operation, @NotNull StackingRule stackingRule, double value, long durationTicks, boolean persistent) {
		this.sourceId = sourceId;
		this.targetAttributeId = targetAttributeId;
		this.operation = operation;
		this.stackingRule = stackingRule;
		this.value = value;
		this.durationTicks = durationTicks;
		this.persistent = persistent;
	}

	/**
	 * 単一のAttributeを対象とする、補正の情報を作成します。
	 * エンティティを対象とする際、時間の指定がある補正はエンティティがアンロードされると消滅し、<b>保存されません。</b>
	 * プレイヤーを対象とする際は、時間の指定に関係なく保存されます。
	 *
	 * @param sourceId          このModifierの源泉のID、StackingRuleを適応するために使用されます
	 * @param targetAttributeId 対象とするAttributeのID
	 * @param operation         対象に対しての値の計算方法
	 * @param stackingRule      同じ源泉から来たModifierの場合に使用され、後に来たModifierのルールが適応されます
	 * @param value             補正する値
	 * @param durationTicks     この補正の適応される時間(Tick)。0以下の場合は保存されます
	 */
	public AttributeModifier(@NotNull String sourceId, @NotNull String targetAttributeId, @NotNull ModifierOperation operation, @NotNull StackingRule stackingRule, double value, long durationTicks) {
		this(sourceId, targetAttributeId, operation, stackingRule, value, durationTicks, true);
	}

	/**
	 * 単一のAttributeを対象とする、永続的な補正の情報を作成します。
	 * 時間の指定がない補正は消滅せず、<b>保存されます。</b>
	 *
	 * @param sourceId          このModifierの源泉のID、StackingRuleを適応するために使用されます
	 * @param targetAttributeId 対象とするAttributeのID
	 * @param operation         対象に対しての値の計算方法
	 * @param stackingRule      同じ源泉から来たModifierの場合に使用され、後に来たModifierのルールが適応されます
	 * @param value             補正する値
	 */
	public AttributeModifier(@NotNull String sourceId, @NotNull String targetAttributeId, @NotNull ModifierOperation operation, @NotNull StackingRule stackingRule, double value) {
		this(sourceId, targetAttributeId, operation, stackingRule, value, -1, true);
	}

	@Override
	@NotNull
	public String getSourceId() {
		return sourceId;
	}

	@Override
	@NotNull
	public String getTargetAttributeId() {
		return targetAttributeId;
	}

	@Override
	@NotNull
	public ModifierOperation getOperation() {
		return operation;
	}

	/**
	 * 同じ源泉から来たModifierの場合に使用され、後に来たModifierのルールが適応されます。
	 */
	@NotNull
	public StackingRule getStackingRule() {
		return stackingRule;
	}

	/**
	 * 補正する値を取得します。
	 */
	public double getValue() {
		return value;
	}

	@Override
	@NotNull
	public OptionalLong getDurationTicks() {
		return durationTicks > 0 ? OptionalLong.of(durationTicks) : OptionalLong.empty();
	}

	@Override
	public boolean isPersistentToPlayer() {
		return persistent;
	}

	@Override
	public boolean isPersistentToMob() {
		return persistent && durationTicks <= 0;
	}
}
