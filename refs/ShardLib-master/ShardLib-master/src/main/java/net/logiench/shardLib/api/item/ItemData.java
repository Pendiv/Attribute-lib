package net.logiench.shardLib.api.item;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Map;
import java.util.Optional;

public interface ItemData {

	/**
	 * このアイテムが持つ基礎ステータス値のマップを取得します。
	 *
	 * @return 変更不可なマップ
	 */
	@Unmodifiable
	@NotNull
	Map<String, Double> getBaseStats();

	/**
	 * このアイテムが持つ基礎ステータス値のマップを取得します。
	 *
	 * @param id ステータスのID
	 * @return 取得したステータス値、対象のステータスが存在しない場合はempty
	 */
	@NotNull
	Optional<Double> getBaseStat(String id);

	/**
	 * ItemDataを編集するためのビルダーを取得します。
	 */
	@NotNull
	Builder toBuilder();

	interface Builder {

		/**
		 * 登録されているすべてのBaseStatsをクリアします。
		 *
		 * @return 自身のビルダー
		 */
		@NotNull
		Builder clearBaseStats();


		/**
		 * アイテムの基礎ステータスを追加します。
		 * すでにあるステータスに対しては、上書き処理を行います。
		 *
		 * @param id    ステータスのID
		 * @param value ステータス値
		 * @return 自身のビルダー
		 */
		@NotNull
		Builder setBaseStat(@NotNull String id, double value);

		/**
		 * アイテムの基礎ステータスを追加します。
		 * {@link #build()}時にfinalStatsを計算する際のベースとなります。
		 * すでにあるステータスに対しては、上書き処理を行います。
		 *
		 * @param baseStats 基礎ステータスのマップ
		 * @return 自身のビルダー
		 */
		@NotNull
		Builder setBaseStats(@NotNull Map<String, Double> baseStats);

		/**
		 * これまでの設定を元に、最終的なステータスを計算し、
		 * 新しい不変のItemDataインスタンスを生成します。
		 *
		 * @return 生成されたItemDataインスタンス
		 */
		@NotNull
		ItemData build();
	}
}
