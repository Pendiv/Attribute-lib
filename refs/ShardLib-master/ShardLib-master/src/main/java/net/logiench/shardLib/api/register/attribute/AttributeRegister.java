package net.logiench.shardLib.api.register.attribute;

import net.logiench.shardLib.api.attribute.data.AttributeValueProvider;

public interface AttributeRegister {
	/**
	 * 全てのステータスに共通する、根幹となるステータスを定義します。
	 * AttributeのシステムについてはREADMEを参照してください。
	 *
	 * @return 根幹のステータス定義
	 */
	AttributeDefinitionRegister coreAttribute();

	/**
	 * {@link AttributeValueProvider}で使用する計算式を定義します。
	 *
	 * @return 計算式定義
	 */
	AttributeValueProviderRegister valueProvider();
}
