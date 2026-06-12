package net.logiench.shardLib.api.attribute.data;

import net.logiench.shardLib.api.player.PlayerAttributeAPI;
import net.logiench.shardLib.api.player.PlayerCharacterAPI;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

/**
 * {@link AttributeValueProvider}の計算で使用するためのデータレコード
 *
 * @param player    プレイヤー
 * @param character プレイヤーの{@link PlayerCharacterAPI}
 */
public record CalculationContext(
	Player player,
	PlayerCharacterAPI character
) {
	/**
	 * プレイヤーが現在いるワールドを取得します
	 */
	@NotNull
	public World world() {
		return player.getWorld();
	}

	/**
	 * プレイヤーのインベントリを取得します
	 */
	@NotNull
	public PlayerInventory inventory() {
		return player.getInventory();
	}

	/**
	 * プレイヤーの{@link PlayerAttributeAPI}を取得します
	 */
	@NotNull
	public PlayerAttributeAPI attributeAPI() {
		return character.getAttributeAPI();
	}
}
