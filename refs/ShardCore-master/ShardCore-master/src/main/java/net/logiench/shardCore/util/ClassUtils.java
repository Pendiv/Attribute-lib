package net.logiench.shardCore.util;

import net.logiench.shardCore.ShardCore;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ClassUtils {
	private static final String FIND_PACKAGES = "net.logiench.shardCore";
	// 探索は非常に処理時間が長い(1000ms~)なのでキャッシュする
	private static final Reflections reflectionsCache = new Reflections(new ConfigurationBuilder()
		.forPackages(FIND_PACKAGES)
		.addScanners(Scanners.SubTypes));

	/**
	 * masterクラスを継承、実装した、抽象ではないクラスをすべて取得します。
	 * この探索は<code>net.logiench.shardCore</code>パッケージ内のみを対象とします。
	 *
	 * @param masterClass 親となるクラスのClass。interfaceやabstractなども指定できます
	 * @param <Z>         親クラスの型
	 * @return masterClassを継承、実装したクラスのClass
	 */
	public static <Z> Set<Class<? extends Z>> findSubClasses(@NotNull Class<Z> masterClass) {
		return reflectionsCache.getSubTypesOf(masterClass).stream()
			.filter(c -> !Modifier.isAbstract(c.getModifiers()))
			.collect(Collectors.toSet());
	}

	/**
	 * 指定されたパッケージの中からmasterクラスを継承、実装した、抽象ではないクラスをすべて取得します
	 * この探索は<code>net.logiench.shardCore</code>パッケージ内より下のものを対象とします。
	 *
	 * @param masterClass 親となるクラスのClass。interfaceやabstractなども指定できます
	 * @param packageName 検索対象のパッケージ。<code>net.logiench.shardCore</code>などと指定します
	 * @param <Z>         親クラスの型
	 * @return masterClassを継承、実装したクラスのClass
	 */
	public static <Z> Set<Class<? extends Z>> findSubClasses(@NotNull Class<Z> masterClass, @NotNull String packageName) {
		if (!packageName.startsWith(FIND_PACKAGES)) {
			// 探索処理は起動の重要なプロセスの一つだから、エラーでプラグインが起動せず不正行為ができるとかは避けたい
			// それよりかは登録されてないほうがまし
			ShardCore.getPLogger().warning("探索対象となるパッケージは '" + FIND_PACKAGES + "' に含まれる必要があります。 指定: " + packageName);
			return Collections.emptySet();
		}
		// 指定されたパッケージ名から始まるクラス
		// その中から非abstract（具象クラス）のものをフィルタリング
		return reflectionsCache.getSubTypesOf(masterClass).stream()
			.filter(c -> c.getName().startsWith(packageName))
			.filter(c -> !Modifier.isAbstract(c.getModifiers())) // abstractでない
			.collect(Collectors.toSet());
	}

	/**
	 * 引数のないコンストラクタを呼び出しインスタンス化します。
	 */
	@Nullable
	@Contract("null -> null")
	public static <T> T initialize(Class<T> clazz) {
		if (clazz == null) {
			return null;
		}
		try {
			Constructor<T> constructor = clazz.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			Logger logger = ShardCore.getPLogger();
			logger.warning("インスタンス化に失敗しました。 '" + clazz.getName() + "'");
			logger.warning(e.getCause().getMessage());
		} catch (NoSuchMethodException e) {
			ShardCore.getPLogger().warning("引数無しのコンストラクタが見つかりません。 '" + clazz.getName() + "'");
		}
		return null;

	}
}
