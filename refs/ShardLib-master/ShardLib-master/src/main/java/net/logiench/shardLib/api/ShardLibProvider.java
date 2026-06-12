package net.logiench.shardLib.api;

import com.google.inject.Inject;

/**
 * ShardLibAPIを取得できます
 */
public class ShardLibProvider {
	private static ShardLibAPI api = null;

	@Inject
	private ShardLibProvider(ShardLibAPI api) {
		ShardLibProvider.api = api;
	}

	/**
	 * APIのインスタンスを取得します。
	 * すべてのAPIはここからアクセスできます。
	 *
	 * @throws IllegalStateException APIの初期化が完了する前に呼び出された場合
	 */
	public static ShardLibAPI get() {
		if (api == null) {
			throw new IllegalStateException("ShardLibAPI has not been initialized");
		}
		return api;
	}
}
