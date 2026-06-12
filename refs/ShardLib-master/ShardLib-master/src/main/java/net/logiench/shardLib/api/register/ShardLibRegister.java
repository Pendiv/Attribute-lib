package net.logiench.shardLib.api.register;

import net.logiench.shardLib.api.register.attribute.AttributeRegister;
import net.logiench.shardLib.api.register.mob.MobRegister;
import net.logiench.shardLib.api.register.player.PlayerRegister;

public interface ShardLibRegister {
	/**
	 * ステータスの様々な定義をまとめたクラスを取得します。
	 *
	 * @return ステータス定義まとめクラス。
	 */
	AttributeRegister attribute();

	/**
	 * モブの様々な定義をまとめたクラスを取得します。
	 *
	 * @return モブの定義まとめクラス
	 */
	MobRegister mob();

	/**
	 * プレイヤーの様々な定義をまとめたクラスを取得します。
	 *
	 * @return プレイヤーの定義まとめクラス
	 */
	PlayerRegister player();
}
