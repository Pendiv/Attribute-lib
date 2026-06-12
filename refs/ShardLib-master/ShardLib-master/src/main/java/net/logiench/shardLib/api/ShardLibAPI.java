package net.logiench.shardLib.api;

import net.logiench.shardLib.api.item.ItemAPI;
import net.logiench.shardLib.api.mob.MobAPI;
import net.logiench.shardLib.api.player.PlayerAPI;
import net.logiench.shardLib.api.register.ShardLibRegister;
import org.jetbrains.annotations.NotNull;

/**
 * <h1>ShardLibAPI</h1>
 */
public interface ShardLibAPI {
	/**
	 * PlayerAPIを取得します
	 */
	@NotNull
	PlayerAPI getPlayerAPI();

	/**
	 * MobAPIを取得します
	 */
	@NotNull
	MobAPI getMobAPI();

	/**
	 * ItemAPIを取得します
	 */
	@NotNull
	ItemAPI getItemAPI();

	/**
	 * JavaからConfigと同様の定義を行えるShardRegisterを取得します。<br>
	 * API経由で行われた登録は<b>Configでの指定よりも優先され、上書きされます。</b><br>
	 * 上書きされた際はログが表示されます。
	 */
	@NotNull
	ShardLibRegister getRegister();
}
