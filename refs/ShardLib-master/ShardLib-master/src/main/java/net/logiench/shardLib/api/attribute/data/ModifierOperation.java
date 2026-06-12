package net.logiench.shardLib.api.attribute.data;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Modifierが対象の値に対して、どのように適応されるかを決定します
 */
public enum ModifierOperation {
	/// <code>+</code>  元の値に加算
	ADD(1),
	/// <code>*</code>  元の値に乗算
	@SerializedName("MULTI")
	MULTIPLY(2),
	/// <code>-</code>  元の値から減算
	@SerializedName("SUB")
	SUBTRACT(1),
	/// <code>/</code>  元の値を徐算
	@SerializedName("DIV")
	DIVIDE(2),
	/// <code>=</code>  元の値を無視し、値を設定
	SET(3),
	;

	private final int priority;

	ModifierOperation(int priority) {
		this.priority = priority;
	}

	private static final List<ModifierOperation> SORTED_VALUES = Arrays.stream(values()).sorted(Comparator.comparingInt(a -> a.priority)).toList();

	/**
	 * 優先度の値が低い順に取得できます
	 *
	 * @return 編集不可な優先度順のList
	 */
	@Unmodifiable
	public static List<ModifierOperation> getSorted() {
		return SORTED_VALUES;
	}
}
