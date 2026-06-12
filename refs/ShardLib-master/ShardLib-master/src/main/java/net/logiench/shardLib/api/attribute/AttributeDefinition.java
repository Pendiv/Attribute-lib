package net.logiench.shardLib.api.attribute;

import net.logiench.shardLib.api.attribute.data.AttributeFormula;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * ひとつのステータス（属性）の定義。
 *
 * @param id           そのAttributeを表す内部IDとなります。既に登録されているIDを指定するとそのAttributeの定義を上書きします。 (例: "max_health")
 * @param dependencies そのステータスの計算に使用するためのステータス。依存関係が循環すると計算ができずエラーになります。
 *                     指定されたステータスはnullではなく、必ず数値を持った状態でformulaに提供されます。
 * @param formula      そのステータスの計算式。この計算を行うためにdependenciesに与えられたステータスが変更不可なMapで渡されます。
 *                     nullの場合は前のものが引き継がれ、{@link AttributeFormula#NONE}では計算式なしとして置き換えられます。
 * @param defaultValue そのステータスの基本値。nullの場合は前のものが引き継がれます。前のものが存在しない場合は0になります。
 *
 */
public record AttributeDefinition(
	@NotNull String id,
	@NotNull List<String> dependencies,
	@Nullable AttributeFormula formula,
	@Nullable Double defaultValue
) {
	/**
	 * ひとつのステータス（属性）の定義。
	 * デフォルトの値は0になります。
	 *
	 * @param id 内部ID (例: "max_health")
	 */
	public AttributeDefinition(@NotNull String id) {
		this(id, List.of(), null, 0d);
	}

	/**
	 * ひとつのステータス（属性）の定義。
	 * デフォルトの値は0になります。
	 *
	 * @param id           そのAttributeを表す内部IDとなります。既に登録されているIDを指定するとそのAttributeの定義を上書きします。 (例: "max_health")
	 * @param dependencies そのステータスの計算に使用するためのステータス。依存関係が循環すると計算ができずエラーになります。
	 *                     指定されたステータスはnullではなく、必ず数値を持った状態でformulaに提供されます。
	 * @param formula      そのステータスの計算式。この計算を行うためにdependenciesに与えられたステータスが変更不可なMapで渡されます。
	 *                     nullの場合は前のものが引き継がれ、{@link AttributeFormula#NONE}では計算式なしとして置き換えられます。
	 */
	public AttributeDefinition(
		@NotNull String id,
		@NotNull List<String> dependencies,
		@Nullable AttributeFormula formula
	) {
		this(id, dependencies, formula, 0d);
	}

	/**
	 * formulaが計算可能かを判定します。
	 *
	 * @return 計算できる場合はtrue, できない場合はfalse
	 */
	public boolean canCalculate() {
		return formula != null && formula != AttributeFormula.NONE;
	}

	/**
	 * formulaを持っている場合はinputの値を使用してステータスを計算します。
	 *
	 * @param input 入力値。dependenciesで定義されているステータスを網羅していないと内部処理でエラーを吐く可能性があります。
	 * @return 計算した値
	 *
	 * @throws IllegalStateException formulaが計算できない場合
	 */
	public double calculateValue(@NotNull Map<String, Double> input) {
		if (canCalculate()) {
			return formula.calculate(input);
		}
		throw new IllegalStateException("Can't calculate the value for this attribute.");
	}

	/**
	 * ステータスのデフォルト値を取得します。
	 * 指定されていない場合は0になります。
	 *
	 * @return ステータスのデフォルト値
	 */
	@Override
	@NotNull
	public Double defaultValue() {
		return defaultValue == null ? 0 : defaultValue;
	}

	/**
	 * デフォルトのステータスがconfigで指定されているかどうか確認します。
	 *
	 * @return デフォルトステータスが明記されているか
	 */
	public boolean hasDefaultValue() {
		return defaultValue != null;
	}
}
