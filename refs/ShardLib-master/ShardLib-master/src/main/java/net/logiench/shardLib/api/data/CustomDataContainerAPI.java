package net.logiench.shardLib.api.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * カスタムデータを容易に管理するためのAPIです。<br>
 * <b>高頻度の書き換えには向かないので注意してください。</b>
 */
public interface CustomDataContainerAPI extends Cloneable {

	/**
	 * 指定されたキーに値を設定します
	 * キーは "myplugin:quest_step" のようにプラグイン名で名前空間を分けることを推奨
	 */
	<T> void set(@NotNull String key, T value);

	/**
	 * 指定されたキーから値を取得します
	 *
	 * @param type valueはこのクラスにキャストし、返されます
	 * @return 値が発見できなかった、もしくはtypeにキャストできなかった場合はempty
	 */
	@NotNull
	<T> Optional<T> get(@NotNull String key, @NotNull Class<T> type);

	/**
	 * 指定されたキーから値を取得します
	 */
	@NotNull
	Optional<Object> get(@NotNull String key);

	/**
	 * 指定されたキーから値を取得します。値が見つからなかった場合はdefaultの値が適応されます
	 *
	 * @param type         valueはこのクラスにキャストし、返されます
	 * @param defaultValue 見つからなかった場合の値
	 */
	@NotNull
	<T> T getOrDefault(@NotNull String key, @NotNull Class<T> type, @NotNull T defaultValue);

	/**
	 * 指定されたキーから値を取得し、編集、適応します
	 *
	 * @param type valueはこのクラスにキャストし、返されます
	 * @param edit {@link #get(String, Class)} をもらい、それを編集した値を返す関数
	 * @return editで編集した値
	 */
	<T> T edit(@NotNull String key, @NotNull Class<T> type, @NotNull Function<Optional<T>, T> edit);

	@NotNull
	Optional<Class<?>> getClass(@NotNull String key);

	/**
	 * 指定されたキーを持っているか判定します
	 */
	boolean has(@NotNull String key);

	/**
	 * 指定されたキーを削除します
	 * このキーに紐づいている値も削除されます
	 *
	 * @return 削除された値
	 */
	@NotNull
	Optional<Object> remove(@NotNull String key);

	/**
	 * 指定されたキーを削除します
	 * このキーに紐づいている値も削除されます
	 *
	 * @return 削除された、typeにキャストされた値。キャストできなかった場合は削除対象がなかった時と同様にempty
	 */
	@NotNull
	<T> Optional<T> remove(@NotNull String key, @NotNull Class<T> type);

	/**
	 * 登録されているすべてのキーを取得します
	 *
	 * @return 変更不可なセット
	 */
	@NotNull
	Set<String> getKeys();

	@Unmodifiable
	@NotNull
	List<Object> getValues();

	@NotNull
	CustomDataContainerAPI clone();
}
