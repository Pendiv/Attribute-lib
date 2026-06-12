package net.logiench.shardLib.api.attribute.data;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface AttributeFormula {
	/**
	 * 何もしないけど存在はする計算式。
	 * 元の計算式がある場合、nullでは前のものが引き継がれますが、NONEでは計算式なしとして上書きされます。
	 */
	AttributeFormula NONE = s -> {
		throw new UnsupportedOperationException("The 'NONE' formula should never be executed.");
	};

	/**
	 * 入力されたステータスをもとに計算を行います
	 *
	 * @param inputStats 計算のためのステータス
	 * @return 計算結果
	 */
	double calculate(@NotNull Map<String, Double> inputStats);
}
