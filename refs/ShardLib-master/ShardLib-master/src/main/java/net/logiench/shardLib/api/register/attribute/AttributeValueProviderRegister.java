package net.logiench.shardLib.api.register.attribute;

import net.logiench.shardLib.api.attribute.data.AttributeValueProvider;
import net.logiench.shardLib.api.attribute.data.CalculationContext;
import net.logiench.shardLib.api.attribute.data.ProviderCalculation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Map;
import java.util.Optional;
import java.util.function.ToDoubleFunction;

public interface AttributeValueProviderRegister {
	/**
	 * 指定されたキーで{@link AttributeValueProvider}で使用する計算式を登録します。
	 *
	 * @param key      作成する計算式のID
	 * @param provider 計算式
	 * @return 登録に使用する計算式
	 */
	@NotNull
	ProviderCalculation register(String key, ToDoubleFunction<CalculationContext> provider);

	/**
	 * 計算式を取得します。
	 *
	 * @param key 計算式のキー
	 * @return ステータスの計算式。存在しない場合はempty
	 */
	@NotNull
	Optional<ProviderCalculation> get(String key);

	/**
	 * 全てのキーと計算式を取得します。
	 *
	 * @return 編集不可なMap
	 */
	@Unmodifiable
	@NotNull
	Map<String, ProviderCalculation> getAll();
}
