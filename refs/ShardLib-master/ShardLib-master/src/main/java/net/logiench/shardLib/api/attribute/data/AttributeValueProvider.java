package net.logiench.shardLib.api.attribute.data;

import net.logiench.shardLib.api.register.attribute.AttributeValueProviderRegister;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalLong;

/**
 * 単一のAttributeを対象とする、状態によって変化する補正を保持するクラス。
 */
public final class AttributeValueProvider implements AttributeOperationModifier {
	private final String sourceId;
	private final String targetAttributeId;
	private final ModifierOperation operation;
	private final ProviderCalculation provider;
	private final long durationTicks;
	private final boolean persistent;

	/**
	 * 単一のAttributeを対象とする、状態によって変化する補正を作成します。
	 * persistentがfalseの場合はプレイヤーが退出すると消滅し、<b>保存されません。</b>
	 *
	 * @param targetAttributeId 対象とするAttributeのID
	 * @param operation         対象に対しての値の計算方法
	 * @param provider          値を計算するための式。{@link AttributeValueProviderRegister}で取得した{@link ProviderCalculation}を指定します。
	 * @param durationTicks     この補正の適応される時間(Tick)
	 * @param persistent        この補正を保存するか。falseの場合はプレイヤーが退出すると破棄されます。
	 * @throws IllegalArgumentException 入力された<code>function</code>が存在しなかった場合。
	 */
	public AttributeValueProvider(@NotNull String sourceId, @NotNull String targetAttributeId, @NotNull ModifierOperation operation, @NotNull ProviderCalculation provider, long durationTicks, boolean persistent) {
		this.sourceId = sourceId;
		this.targetAttributeId = targetAttributeId;
		this.operation = operation;
		this.provider = provider;
		this.durationTicks = durationTicks;
		this.persistent = persistent;
	}

	/**
	 * 単一のAttributeを対象とする、状態によって変化する補正を作成します。
	 * 補正はプレイヤーが退出しても消滅せず、<b>保存されます。</b>
	 *
	 * @param targetAttributeId 対象とするAttributeのID
	 * @param operation         対象に対しての値の計算方法
	 * @param provider          値を計算するための式。{@link AttributeValueProviderRegister}で取得した{@link ProviderCalculation}を指定します。
	 * @param durationTicks     この補正の適応される時間(Tick)
	 * @throws IllegalArgumentException 入力された<code>function</code>が存在しなかった場合。
	 */
	public AttributeValueProvider(@NotNull String sourceId, @NotNull String targetAttributeId, @NotNull ModifierOperation operation, @NotNull ProviderCalculation provider, long durationTicks) {
		this(sourceId, targetAttributeId, operation, provider, durationTicks, true);
	}

	/**
	 * 単一のAttributeを対象とする、状態によって変化する永続的な補正を作成します。
	 * 補正はプレイヤーが退出しても消滅せず、<b>保存されます。</b>
	 *
	 * @param targetAttributeId 対象とするAttributeのID
	 * @param operation         対象に対しての値の計算方法
	 * @param provider          値を計算するための式。{@link AttributeValueProviderRegister}で取得した{@link ProviderCalculation}を指定します
	 * @throws IllegalArgumentException 入力された<code>function</code>が存在しなかった場合。
	 */
	public AttributeValueProvider(@NotNull String sourceId, @NotNull String targetAttributeId, @NotNull ModifierOperation operation, @NotNull ProviderCalculation provider) {
		this(sourceId, targetAttributeId, operation, provider, -1);
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
	 * 計算式
	 */
	@NotNull
	public ProviderCalculation getProviderCalculation() {
		return provider;
	}

	/**
	 * 計算した結果の値
	 *
	 * @param context 計算に使用するステータス
	 * @return 計算結果
	 */
	public double getValue(CalculationContext context) {
		return provider.function().applyAsDouble(context);
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

	/**
	 * モブに対してこの補正は適応できません。
	 *
	 * @throws UnsupportedOperationException このメソッドがコールされると必ずスローされます
	 */
	@Override
	public boolean isPersistentToMob() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}
}