package net.logiench.shardCore.core.loot.base;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 抽選回数や個数を決定するためのプロバイダ
 */
@FunctionalInterface
public interface NumberProvider {
	int nextInt();

	// 固定値を返す
	static NumberProvider constant(int value) {
		return () -> value;
	}

	// 最小〜最大の範囲でランダム (一様分布)
	static NumberProvider uniform(int min, int max) {
		return () -> ThreadLocalRandom.current().nextInt(min, max + 1);
	}

	// 二項分布 (確率pでn回試行した成功数) などもここに追加可能
}