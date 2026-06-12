package net.logiench.shardLib.api.player;

import net.logiench.shardLib.api.attribute.AttributeAPI;
import net.logiench.shardLib.api.attribute.Ticket;
import net.logiench.shardLib.api.attribute.data.AttributeOperationModifier;
import net.logiench.shardLib.api.attribute.data.AttributeValueProvider;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalLong;

/**
 * プレイヤー用に機能が拡張されたAttributeAPI。
 * プレイヤーの状況に応じて変化するステータスを容易に作成できます。
 */
public interface PlayerAttributeAPI extends AttributeAPI {
	/**
	 * ステータスに加算値を与えます
	 *
	 * @param provider 追加するAttributeValueProvider
	 * @return このproviderを削除するためのチケット
	 *
	 * @throws IllegalArgumentException 存在しないAttributeIdが指定された場合
	 */
	@NotNull
	Ticket addProvider(AttributeValueProvider provider) throws IllegalArgumentException;

	/**
	 * ステータスに加算値を与えます
	 *
	 * @param modifier 追加するAttributeOperationModifier
	 * @return このmodifierを削除するためのチケット
	 *
	 * @throws IllegalArgumentException 存在しないAttributeIdが指定された場合
	 */
	@NotNull
	Ticket addOperationModifier(AttributeOperationModifier modifier) throws IllegalArgumentException;

	/**
	 * 指定されたAttributeValueProviderを削除します。
	 *
	 * @param provider 削除するProvider
	 */
	void removeProvider(AttributeValueProvider provider);

	/**
	 * 指定されたAttributeOperationModifierを削除します。
	 *
	 * @param modifier 削除するProvider
	 */
	void removeOperationModifier(AttributeOperationModifier modifier);

	/**
	 * 指定されたAttributeOperationModifierのタイマーが開始されたゲーム内Tick{@link Bukkit#getCurrentTick()}を取得します。
	 *
	 * @param modifier 取得する対象
	 * @return タイマーが有効な場合は開始されたゲーム内Tick、タイマーがすでに終了しているもしくは指定されたModifierにタイマーが存在しない場合はempty
	 */
	OptionalLong getStartTicks(AttributeOperationModifier modifier);

	/**
	 * 指定されたAttributeOperationModifierの残りの有効tick数を取得します。
	 *
	 * @param modifier 取得する対象
	 * @return 残りtick数が1以上の場合はその時間、0以下または指定されたModifierにタイマーが存在しない場合はempty
	 */
	OptionalLong getRemainingTicks(AttributeOperationModifier modifier);
}
