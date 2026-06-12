package net.logiench.shardLib.api.register.mob;

import net.logiench.shardLib.api.event.ShardLibReadyEvent;
import net.logiench.shardLib.api.register.attribute.AttributeDefinitionRegister;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Map;
import java.util.Optional;

public interface MobAttributeRegister {
	/**
	 * 指定されたIDのエンティティ用ステータス({@link AttributeDefinitionRegister})を作成、登録し、取得します。
	 * 取得には{@link #get(String)}を使用してください。
	 *
	 * @param id 作成する定義のID
	 * @return 作成された定義
	 */
	@NotNull
	AttributeDefinitionRegister registerFor(@NotNull String id);

	/**
	 * エンティティ用ステータスの定義を取得します。
	 *
	 * @param id 取得するステータスの内部ID。
	 * @return ステータスの定義。存在しない場合はempty
	 */
	@NotNull
	Optional<AttributeDefinitionRegister> get(String id);

	/**
	 * ステータスの定義をすべて取得します。
	 *
	 * @return 編集不可なMapに入った全てのステータスの定義。定義自体は{@link ShardLibReadyEvent}まで編集可能です。
	 * <code>Map<内部ID, 定義></code>
	 */
	@Unmodifiable
	@NotNull
	Map<String, AttributeDefinitionRegister> getAll();
}
