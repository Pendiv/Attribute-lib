package net.logiench.shardLib.api.player;

import net.logiench.shardLib.api.data.CustomDataContainerAPI;
import org.jetbrains.annotations.NotNull;

/**
 * プレイヤーが保持している各種ステータスなどにアクセス、編集し、適応できます
 */
public interface PlayerCharacterAPI {

	/**
	 * プレイヤーのステータスに関してのAPIを取得します
	 *
	 * @return プレイヤーに紐づいたAttributeAPI
	 */
	@NotNull
	PlayerAttributeAPI getAttributeAPI();

	/**
	 * 外部プラグインが独自のデータを安全に保存するための専用データ領域を取得します。
	 *
	 * @return カスタムデータコンテナ、まだ作成されていない場合はempty
	 */
	@NotNull
	CustomDataContainerAPI getPersistentData();

	/**
	 * データをプレイヤーに保存します。
	 *
	 * @param dataContainer 保存するデータ。データのみの参照なので再利用が可能です
	 */
	void setPersistentData(@NotNull CustomDataContainerAPI dataContainer);
}
