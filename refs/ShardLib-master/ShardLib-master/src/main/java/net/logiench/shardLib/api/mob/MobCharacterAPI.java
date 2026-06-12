package net.logiench.shardLib.api.mob;

import net.logiench.shardLib.api.attribute.AttributeAPI;
import net.logiench.shardLib.api.data.CustomDataContainerAPI;
import org.jetbrains.annotations.NotNull;

public interface MobCharacterAPI {

	/**
	 * エンティティのステータスに関してのAPIを取得します
	 *
	 * @return エンティティに紐づいたAttributeAPI
	 */
	@NotNull
	AttributeAPI getAttributeAPI();

	/**
	 * 現在のデータをエンティティのPDCに保存します
	 */
	void save();

	/**
	 * {@link #save()}が実行されてからデータが変更されたかを表します
	 *
	 * @return 変更されていればtrue, それ以外はfalse
	 */
	boolean isDirty();

	/**
	 * 外部プラグインが独自のデータを安全に保存するための専用データ領域を取得します。
	 *
	 * @return カスタムデータコンテナ、まだ作成されていない場合はempty
	 */
	@NotNull
	CustomDataContainerAPI getPersistentData();

	/**
	 * データをエンティティに保存します。
	 *
	 * @param dataContainer 保存するデータ。データのみの参照なので再利用が可能です
	 */
	void setPersistentData(@NotNull CustomDataContainerAPI dataContainer);
}
