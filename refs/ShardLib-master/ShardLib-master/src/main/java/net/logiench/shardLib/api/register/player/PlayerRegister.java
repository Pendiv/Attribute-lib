package net.logiench.shardLib.api.register.player;

import net.logiench.shardLib.api.register.attribute.AttributeDefinitionRegister;

public interface PlayerRegister {
	/**
	 * プレイヤーのステータス定義を取得します。
	 *
	 * @return プレイヤーのステータス定義。
	 */
	AttributeDefinitionRegister attributes();
}
