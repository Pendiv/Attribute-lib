package net.logiench.shardLib.api.attribute;

import net.logiench.shardLib.api.attribute.data.AttributeModifier;
import net.logiench.shardLib.api.attribute.data.AttributeOperationModifier;
import net.logiench.shardLib.api.attribute.data.StackingRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ステータスの確認、編集などが可能なAPI
 */
public interface AttributeAPI {
	/**
	 * 全ての影響を計算した、最終的なステータス値を取得します。
	 *
	 * @return 最終的なステータス値
	 */
	@NotNull
	Map<String, Double> getFinalAttributes();

	/**
	 * 全ての影響を計算した、最終的なステータス値を取得します。
	 *
	 * @param attributeId 取得したいステータスのID
	 * @return 最終的なステータス値
	 *
	 * @throws IllegalArgumentException 存在しないAttributeIdが指定された場合
	 */
	double getFinalAttribute(String attributeId) throws IllegalArgumentException;

	/**
	 * 全ての影響を計算した、最終的なステータス値を取得します。
	 *
	 * @param attributeKey 取得したいステータスキー
	 * @return 最終的なステータス値
	 *
	 * @throws IllegalArgumentException 存在しないAttributeIdが指定された場合
	 */
	default double getFinalAttribute(AttributeKey attributeKey) throws IllegalArgumentException {
		return getFinalAttribute(attributeKey.getId());
	}

	/**
	 * 全ての影響を計算した、最終的なステータス値を取得します。
	 *
	 * @param attributeId 取得したいステータスのID
	 * @return 最終的なステータス値
	 */
	@NotNull
	Optional<Double> getFinalAttributeOptional(String attributeId);

	/**
	 * 全ての影響を計算した、最終的なステータス値を取得します。
	 *
	 * @param attributeKey 取得したいステータスキー
	 * @return 最終的なステータス値
	 */
	@NotNull
	default Optional<Double> getFinalAttributeOptional(AttributeKey attributeKey) {
		return getFinalAttributeOptional(attributeKey.getId());
	}

	/**
	 * プレイヤー自体が持っているステータスを取得します
	 *
	 * @return 基礎ステータス値
	 */
	@NotNull
	Map<String, Double> getBaseAttributes();

	/**
	 * プレイヤー自体が持っているステータスを取得します
	 *
	 * @param attributeId 取得したいステータスのID
	 * @return 基礎ステータス値
	 *
	 * @throws IllegalArgumentException 存在しないAttributeIdが指定された場合
	 */
	double getBaseAttribute(String attributeId) throws IllegalArgumentException;

	/**
	 * プレイヤー自体が持っているステータスを取得します
	 *
	 * @param attributeKey 取得したいステータスキー
	 * @return 基礎ステータス値
	 *
	 * @throws IllegalArgumentException 存在しないAttributeIdが指定された場合
	 */
	default double getBaseAttribute(AttributeKey attributeKey) throws IllegalArgumentException {
		return getBaseAttribute(attributeKey.getId());
	}

	/**
	 * プレイヤー自体が持っているステータスを取得します
	 *
	 * @param attributeId 取得したいステータスのID
	 * @return 基礎ステータス値
	 */
	@NotNull
	Optional<Double> getBaseAttributeOptional(String attributeId);

	/**
	 * プレイヤー自体が持っているステータスを取得します
	 *
	 * @param attributeKey 取得したいステータスキー
	 * @return 基礎ステータス値
	 */
	@NotNull
	default Optional<Double> getBaseAttributeOptional(AttributeKey attributeKey) {
		return getBaseAttributeOptional(attributeKey.getId());
	}

	/**
	 * プレイヤーのステータスを設定します。
	 *
	 * @param attributeId 設定したいステータスのID
	 * @param value       設定する値
	 * @throws IllegalArgumentException 存在しないAttributeIdが指定された場合
	 */
	void setBaseAttribute(String attributeId, double value) throws IllegalArgumentException;

	/**
	 * プレイヤーのステータスを設定します。
	 *
	 * @param attributeKey 設定したいステータスキー
	 * @param value        設定する値
	 * @throws IllegalArgumentException 存在しないAttributeIdが指定された場合
	 */
	default void setBaseAttribute(AttributeKey attributeKey, double value) throws IllegalArgumentException {
		setBaseAttribute(attributeKey.getId(), value);
	}

	/**
	 * プレイヤーのステータスを追加します。
	 * すでにあるステータスは上書きされます。
	 *
	 * @param attributes 設定したいステータスのID
	 * @throws IllegalArgumentException 存在しないAttributeIdが指定された場合
	 */
	void setBaseAttributes(Map<String, Double> attributes) throws IllegalArgumentException;

	/**
	 * プレイヤーのステータスを削除します。
	 * 削除されたステータスを取得する際はデフォルト値が使用されます。
	 *
	 * @param attributeId 削除したいステータスのID
	 * @throws IllegalArgumentException 存在しないAttributeIdが指定された場合
	 */
	void removeBaseAttribute(String attributeId) throws IllegalArgumentException;

	/**
	 * プレイヤーのステータスを削除します。
	 * 削除されたステータスを取得する際はデフォルト値が使用されます。
	 *
	 * @param attributeKey 削除したいステータスキー
	 * @throws IllegalArgumentException 存在しないAttributeIdが指定された場合
	 */
	default void removeBaseAttribute(AttributeKey attributeKey) throws IllegalArgumentException {
		removeBaseAttribute(attributeKey.getId());
	}

	/**
	 * プレイヤーのステータスを編集します。
	 *
	 * @param attributeId  設定したいステータスのID
	 * @param editFunction そのステータスがない場合は、defaultValueを引数として与えられます
	 * @return 編集後のステータス
	 *
	 * @throws IllegalArgumentException 存在しないAttributeIdが指定された場合
	 */
	double editBaseAttribute(String attributeId, DoubleEditFunction editFunction) throws IllegalArgumentException;

	/**
	 * プレイヤーのステータスを編集します。
	 *
	 * @param attributeKey 設定したいステータスキー
	 * @param editFunction そのステータスがない場合は、defaultValueを引数として与えられます
	 * @return 編集後のステータス
	 *
	 * @throws IllegalArgumentException 存在しないAttributeIdが指定された場合
	 */
	default double editBaseAttribute(AttributeKey attributeKey, DoubleEditFunction editFunction) throws IllegalArgumentException {
		return editBaseAttribute(attributeKey.getId(), editFunction);
	}

	/**
	 * プレイヤーのステータスに加算します。
	 *
	 * @param attributeId 設定したいステータスのID
	 * @param value       そのステータスがない場合は、defaultValueに加算されます
	 * @return 編集後のステータス
	 *
	 * @throws IllegalArgumentException 存在しないAttributeIdが指定された場合
	 */
	double addBaseAttribute(String attributeId, double value) throws IllegalArgumentException;

	/**
	 * プレイヤーのステータスに加算します。
	 *
	 * @param attributeKey 設定したいステータスキー
	 * @param value        そのステータスがない場合は、defaultValueに加算されます
	 * @return 編集後のステータス
	 *
	 * @throws IllegalArgumentException 存在しないAttributeIdが指定された場合
	 */
	default double addBaseAttribute(AttributeKey attributeKey, double value) throws IllegalArgumentException {
		return addBaseAttribute(attributeKey.getId(), value);
	}

	/**
	 * プレイヤーのステータスから減算します。
	 * 対象のステータスが存在しない場合は実行されません。
	 *
	 * @param attributeId 設定したいステータスのID
	 * @param value       そのステータスがない場合は、defaultValueから減算します
	 * @return 編集後のステータス
	 *
	 * @throws IllegalArgumentException 存在しないAttributeIdが指定された場合
	 */
	double subtractBaseAttribute(String attributeId, double value) throws IllegalArgumentException;

	/**
	 * プレイヤーのステータスから減算します。
	 * 対象のステータスが存在しない場合は実行されません。
	 *
	 * @param attributeKey 設定したいステータスキー
	 * @param value        そのステータスがない場合は、defaultValueから減算します
	 * @return 編集後のステータス
	 *
	 * @throws IllegalArgumentException 存在しないAttributeIdが指定された場合
	 */
	default double subtractBaseAttribute(AttributeKey attributeKey, double value) throws IllegalArgumentException {
		return subtractBaseAttribute(attributeKey.getId(), value);
	}

	/**
	 * プレイヤーのステータスをすべて削除します。
	 */
	void clearBaseAttributes();

	/**
	 * このプレイヤーに一時的なステータス補正（バフ・デバフ）を追加します。
	 *
	 * @param modifier 追加するAttributeModifier
	 * @return 適応されている効果を削除するためのチケット。<b>StackingRuleによってチケットの対象が変わります。</b>
	 * <ul>
	 *  <li>{@link StackingRule#STACKABLE} {@link StackingRule#REPLACE}: 今回追加した効果</li>
	 *  <li>{@link StackingRule#HIGHEST_WINS} {@link StackingRule#LOWEST_WINS}: この条件で勝利した効果</li>
	 *  <li>{@link StackingRule#DENY}: 重複したsourceIdがない場合(AttributeModifierが適応できた場合)は追加した効果、重複したものがあった場合はempty(実行はできるが意味のないチケット)</li>
	 *  </ul>
	 *
	 * @throws IllegalArgumentException 存在しないAttributeIdが指定された場合
	 */
	@NotNull
	Ticket addModifier(AttributeModifier modifier) throws IllegalArgumentException;

	/**
	 * 指定されたsourceIdを持つ全てのAttributeModifierを削除します。
	 *
	 * @param sourceId 削除するModifierのsourceId
	 */
	void removeModifiers(String sourceId);

	/**
	 * 指定されたAttributeModifierを削除します。
	 *
	 * @param modifier 削除するModifier
	 */
	void removeModifier(AttributeModifier modifier);

	/**
	 * 全てのAttributeOperationModifierを削除します。
	 */
	void removeAll();

	/**
	 * 現在設定されている値の編集処理をすべて取得します。
	 *
	 * @return 編集不可な{@link AttributeOperationModifier}のList
	 */
	@Unmodifiable
	List<AttributeOperationModifier> getModifiers();

	/**
	 * 現在設定されているステータス値を使用して計算します。
	 * <h3>この処理を実行しないと変更は適応されません。</h3>
	 */
	void recalculateStats();
}
