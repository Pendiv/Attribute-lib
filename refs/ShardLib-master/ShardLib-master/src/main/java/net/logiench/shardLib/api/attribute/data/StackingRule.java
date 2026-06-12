package net.logiench.shardLib.api.attribute.data;

import com.google.gson.annotations.SerializedName;

/**
 * 同じソースから来たModifierの場合、どうするかを決定します。
 * STACKABLE以外では、同じsourceIdのModifierは重複しません。
 */
public enum StackingRule {
	/// 今まで適応されていた効果と重複し、そのまま適応されます
	@SerializedName("STACK")
	STACKABLE,
	/// 今まで適応されていた効果をすべて破棄し、新しく追加します
	REPLACE,
	/// よりvalueが大きい1つを適応します。効果時間は条件に含まれません
	@SerializedName("H_WIN")
	HIGHEST_WINS,
	/// よりvalueが小さい1つを適応します。効果時間は条件に含まれません
	@SerializedName("L_WIN")
	LOWEST_WINS,
	/// 新しい効果は適応されず、破棄されます
	DENY
}
