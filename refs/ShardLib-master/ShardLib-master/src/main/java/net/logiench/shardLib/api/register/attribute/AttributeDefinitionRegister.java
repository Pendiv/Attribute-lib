package net.logiench.shardLib.api.register.attribute;

import net.logiench.shardLib.api.attribute.AttributeDefinition;
import net.logiench.shardLib.api.attribute.data.AttributeFormula;
import net.logiench.shardLib.api.event.ShardLibReadyEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public interface AttributeDefinitionRegister {
	/**
	 * Attributeを登録、もしくは上書き(オーバーライド)します。
	 * 指定した要素は上書きされ、nullとした要素は元の内容が維持されます。計算式を継承によって削除したい場合は{@link AttributeFormula#NONE}を使用します。
	 *
	 * @param def 登録するAttributeの定義。
	 * @return 実際に登録されたAttributeのデータを取得できる関数。オーバーライドなどで変化する可能性があるため、{@link ShardLibReadyEvent}がコールされる前に呼び出すと、{@link IllegalStateException}をコールします。
	 *
	 * @throws IllegalArgumentException 同様のスコープで既に登録されているIDを対象とした場合。
	 * @throws IllegalStateException    登録処理のタイミングを既に過ぎている場合。
	 */
	@NotNull
	Supplier<AttributeDefinition> register(@NotNull AttributeDefinition def);

	/**
	 * 登録されているAttributeを取得します。
	 *
	 * @param id 取得する対象のAttributeの内部id。
	 * @return 登録されていたAttributeの定義。存在しない場合はempty。
	 *
	 * @throws IllegalStateException 登録処理前に呼び出された場合。
	 */
	@NotNull
	Optional<AttributeDefinition> get(String id);

	/**
	 * 登録されている全てのAttributeを取得します。
	 *
	 * @return 編集不可な登録されているすべてのAttributeの定義。
	 *
	 * @throws IllegalStateException 登録処理前に呼び出された場合。
	 */
	@Unmodifiable
	@NotNull
	Map<String, AttributeDefinition> getAll();

	/**
	 * Attributeが登録されているかを取得します。
	 *
	 * @param id 確認する対象のAttribute
	 * @return 指定されたIDがAttributeが登録されているか
	 */
	boolean contains(String id);
}
