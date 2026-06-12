package net.logiench.shardLib.api.attribute;

/**
 * Modifierを削除するためのチケット。
 * チケット以外から削除する場合は{@link AttributeAPI#removeModifiers(String)}などを使用してください。
 */
public interface Ticket {
	/**
	 * Modifierを削除する
	 */
	void remove();
}
