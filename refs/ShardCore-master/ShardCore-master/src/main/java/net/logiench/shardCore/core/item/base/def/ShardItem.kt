package net.logiench.shardCore.core.item.base.def

import net.kyori.adventure.text.Component
import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack
import net.logiench.shardCore.ShardCore
import net.logiench.shardCore.core.item.base.module.ItemModule
import org.bukkit.Material
import org.bukkit.NamespacedKey

abstract class ShardItem(
	open val itemType: ItemType,
	open val material: Material = itemType.material
		?: throw IllegalArgumentException("${itemType.name} にはmaterialが指定されていません。ShardItemでmaterialを指定してください。"),
) : Comparable<ShardItem> {

	val key: NamespacedKey by lazy {
		var cacheKey: NamespacedKey? = null
		if (id.contains(":")) {
			cacheKey = NamespacedKey.fromString(id)
		}
		if (cacheKey == null) {
			cacheKey = NamespacedKey(ShardCore.getInstance(), id)
		}
		cacheKey
	}

	/**
	 * アイテムに付与するIDを指定します。
	 * test_itemと指定した場合、`[ShardCore.getInstance]:test_item`のNamespacedKeyとして使用されます。
	 * minecraft:test_itemと指定した場合は、`minecraft:test_item`のNamespacedKeyとして使用されます。
	 * 
	 * @return 他のアイテムと重複しないID
	 */
	abstract val id: String

	abstract val name: Component

	abstract val rarity: Rarity

	/**
	 * アイテムにカスタムモデルを適応するかを判定します。
	 * trueの場合、[.getKey]を用いてカスタムモデルがアイテムに適応されます。
	 * falseの場合は変更されず、[.getMaterial]のモデルが使用されます。
	 * 
	 * @return アイテムにカスタムモデルを適応するか
	 */
	open fun hasCustomModel(): Boolean {
		return false
	}

	/**
	 * このアイテムデータから生成されるアイテムのベースを生成します。
	 * 生成されたアイテムを[net.logiench.shardCore.core.item.system.generator.ItemGenerator]で編集するため、新規インスタンスを生成するようにしてください。
	 * 
	 * @return 編集のベースとなるアイテム。
	 */
	abstract fun createItemStack(): SuperItemStack

	/**
	 * このアイテムの生成に使用するプロセッサのパイプラインを返す
	 * 
	 */
	abstract val modules: List<Class<out ItemModule<*>>>

	override fun compareTo(other: ShardItem): Int {
		return id.compareTo(other.id)
	}
}
