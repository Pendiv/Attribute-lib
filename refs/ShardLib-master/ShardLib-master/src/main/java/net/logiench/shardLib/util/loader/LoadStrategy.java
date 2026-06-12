package net.logiench.shardLib.util.loader;

import net.logiench.shardLib.util.ConfigLoader;
import org.jetbrains.annotations.NotNull;

public interface LoadStrategy {
	/**
	 * この戦略に従って、ロード処理を実行します。
	 *
	 * @param loader ロード処理のコンテキストを持つConfigLoader
	 * @return 処理が成功した場合は true, 失敗した場合は false
	 */
	boolean execute(@NotNull ConfigLoader loader);
}
