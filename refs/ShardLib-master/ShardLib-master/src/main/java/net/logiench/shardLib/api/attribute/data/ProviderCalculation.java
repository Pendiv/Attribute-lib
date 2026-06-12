package net.logiench.shardLib.api.attribute.data;

import net.logiench.shardLib.api.register.attribute.AttributeValueProviderRegister;

import java.util.function.ToDoubleFunction;

/**
 * {@link AttributeValueProvider}の計算式登録に使用します。
 * このクラスを取得するには{@link AttributeValueProviderRegister}で式を登録してください。
 */
public interface ProviderCalculation {
	/**
	 * この式の登録キー
	 */
	String key();

	/**
	 * 計算式の本体
	 */
	ToDoubleFunction<CalculationContext> function();
}
