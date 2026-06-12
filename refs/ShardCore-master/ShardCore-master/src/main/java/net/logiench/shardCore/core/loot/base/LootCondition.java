package net.logiench.shardCore.core.loot.base;

import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface LootCondition {
	/**
	 * 常にtrueを返す（デフォルト用）
	 */
	static LootCondition alwaysTrue() {
		return ctx -> true;
	}

	/**
	 * この条件を満たすかどうかを判定します
	 *
	 * @param context 判定に必要な情報 (EntityDeathEventやPlayerなど。nullの場合もある)
	 * @return trueなら実行を許可する
	 */
	// @todo 将来的な実装であり、使用には不十分な状態
	boolean test(@Nullable Object context);
}
