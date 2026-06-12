package net.logiench.shardLib.api.mob;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public interface MobAPI {

	/**
	 * エンティティの詳細を取得します。プレイヤーは対象外です。
	 *
	 * @param uuid 取得する対象のエンティティのUUID
	 * @return MobCharacterAPIを取得します。モブが見つからない、{@link #isShardEntity(Entity)}がfalseの場合はempty
	 */
	@NotNull
	Optional<MobCharacterAPI> getCharacterAPI(UUID uuid);

	/**
	 * エンティティの詳細を取得します。プレイヤーは対象外です。
	 *
	 * @param entity 取得する対象のエンティティ
	 * @return MobCharacterAPIを取得します。モブが見つからない、{@link #isShardEntity(Entity)}がfalseの場合はempty
	 */
	@NotNull
	Optional<MobCharacterAPI> getCharacterAPI(@NotNull Entity entity);

	/**
	 * 指定されたIDのエンティティのスポーンさせます。
	 *
	 * @param location           スポーンさせる場所
	 * @param type               スポーンさせるエンティティのType
	 * @param attributeProfileId エンティティのステータス
	 * @return 召喚されたエンティティ。IDが存在しない場合はempty
	 */
	@NotNull
	Optional<Entity> spawnEntity(@NotNull Location location, EntityType type, String attributeProfileId);

	/**
	 * 与えられたエンティティがShardLibによって召喚された物かを判定します。
	 * 意図的に同様のデータ構造を持たせているエンティティの場合、間違った判定を返す可能性があります。
	 *
	 * @param entity 検証するエンティティ
	 * @return ShardLibにより生成されたものならtrue, それ以外はfalse
	 */
	@Contract(pure = true, value = "null -> false")
	boolean isShardEntity(@Nullable Entity entity);
}
