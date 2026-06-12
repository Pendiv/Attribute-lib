package net.logiench.shardCore.util;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.VoxelShape;

public class PlayerUtils {
	/**
	 * クライアントからの情報を信用せず、サーバーで当たり判定を計算します。
	 *
	 * @param player 判定するプレイヤー
	 * @return プレイヤーが地面に着地しているか
	 */
	public static boolean isOnGround(Player player) {
		BoundingBox box = player.getBoundingBox();

		// 柵だとブロックの上0.5ブロックまで当たり判定があるから
		// プレイヤーの足元から下0.6ブロックまでを当たり判定の確認範囲とする
		BoundingBox footBox = new BoundingBox(
			box.getMinX(), box.getMinY(), box.getMinZ(),
			box.getMaxX(), box.getMinY() - 0.6, box.getMaxZ()
		);

		World world = player.getWorld();

		// 判定範囲内のブロックをすべて取得するための変数
		int minX = (int) Math.floor(footBox.getMinX());
		int maxX = (int) Math.floor(footBox.getMaxX());
		int minY = (int) Math.floor(footBox.getMinY());
		int maxY = (int) Math.floor(footBox.getMaxY());
		int minZ = (int) Math.floor(footBox.getMinZ());
		int maxZ = (int) Math.floor(footBox.getMaxZ());

		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					Block block = world.getBlockAt(x, y, z);

					// 当たり判定があるブロックのみ判定
					if (block.isPassable()) {
						continue;
					}

					// VoxelShapeは「ブロック内のローカル座標(0.0~1.0)」で計算されるため、
					// プレイヤーのチェック領域をブロックの座標分だけマイナスしてローカル座標に変換する
					BoundingBox localCheckArea = footBox.clone().shift(-x, -y, -z);
					VoxelShape voxel = block.getCollisionShape();
					if (voxel.overlaps(localCheckArea)) {
						return true;
					}
				}
			}
		}

		return false;
	}
}
