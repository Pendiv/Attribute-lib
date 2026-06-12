package net.logiench.shardLib.api.item;

import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import net.logiench.shardLib.api.data.CustomDataContainerAPI;
import org.bukkit.Material;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public interface ItemAPI {

	/**
	 * ShardLibアイテムを生成します
	 */
	@NotNull
	SuperItemStack generate(@NotNull Material material);

	/**
	 * 与えられたアイテムがShardLibによって生成されたものかを判定します。
	 * 意図的に同様のデータ構造を持たせているアイテムの場合、間違った判定を返す可能性があります。
	 *
	 * @param item 検証するアイテム
	 * @return ShardLibにより生成されたものならtrue, それ以外はfalse
	 */
	@Contract(pure = true, value = "null -> false")
	boolean isShardItem(@Nullable SuperItemStack item);

	/**
	 * 与えられたアイテムからItemDataを作成します。
	 * ItemDataを編集する際は {@link ItemData#toBuilder()}を利用してください。
	 *
	 * @param item 対象のアイテム
	 * @return 対象のアイテムのItemData。 {@link #isShardItem(SuperItemStack)}がfalseの場合はempty
	 */
	@NotNull
	Optional<ItemData> getItemData(@Nullable SuperItemStack item);

	/**
	 * 与えられたItemDataをアイテムにセットします。
	 *
	 * @param item セットの対象となるアイテム、このインスタンスに対して直接編集されます
	 * @param data セットするデータ、参照されるのはデータのみなので、再利用可能です
	 * @return アイテムへのデータセットが成功した場合はtrue, それ以外はfalse
	 */
	boolean setItemData(@Nullable SuperItemStack item, @Nullable ItemData data);

	/**
	 * 与えられたアイテムからCustomDataContainerを作成します。
	 *
	 * @param item 対象のアイテム
	 * @return 対象のアイテムのCustomDataContainer
	 */
	@NotNull
	CustomDataContainerAPI getCustomData(@NotNull SuperItemStack item);

	/**
	 * 与えられたItemDataをアイテムにセットします。
	 * データの異なる(無い)アイテムとはスタックできなくなるので、スタック可能なアイテムにセットする場合は注意してください。
	 *
	 * @param item セットの対象となるアイテム、このインスタンスに対して直接編集されます
	 * @param data セットするデータ、参照されるのはデータのみなので、再利用可能です
	 * @return アイテムへのデータセットが成功した場合はtrue, それ以外はfalse
	 */
	boolean setCustomData(@NotNull SuperItemStack item, @NotNull CustomDataContainerAPI data);
}
