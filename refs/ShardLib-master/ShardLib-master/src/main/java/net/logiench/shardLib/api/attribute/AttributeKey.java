package net.logiench.shardLib.api.attribute;

import org.jetbrains.annotations.NotNull;

/**
 * Javaでステータス一覧を作成する際に実装することで、列挙をそのままキーとして使用できるようになります。
 */
public interface AttributeKey {
	/**
	 * AttributeのIDを取得するメソッド
	 *
	 * @return Attributeの内部ID
	 */
	@NotNull
	String getId();
}
