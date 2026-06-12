package net.logiench.shardLib.api.attribute.data;

import org.jetbrains.annotations.NotNull;

import java.util.OptionalLong;

public sealed interface AttributeOperationModifier permits AttributeModifier, AttributeValueProvider {
	/**
	 * このModifierの源泉のID、StackingRuleを適応するために使用されます。
	 */
	@NotNull String getSourceId();

	/**
	 * 対象とするAttributeのIDを取得します。
	 */
	@NotNull String getTargetAttributeId();

	/**
	 * 対象に対しての値の計算方法を取得します。
	 */
	@NotNull ModifierOperation getOperation();

	/**
	 * この補正の持続tickを取得します。
	 *
	 * @return 永久の場合はempty、それ以外はtick数
	 */
	@NotNull OptionalLong getDurationTicks();

	/**
	 * プレイヤーに対して、この補正を永続的なものにするか。(キャッシュだけでなく、保存をするか)
	 */
	boolean isPersistentToPlayer();

	/**
	 * モブに対して、この補正を永続的なものにするか。(キャッシュだけでなく、保存をするか)
	 */
	boolean isPersistentToMob();
}
