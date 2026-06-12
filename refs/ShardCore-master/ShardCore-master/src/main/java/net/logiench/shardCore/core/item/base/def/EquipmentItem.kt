package net.logiench.shardCore.core.item.base.def

import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack
import net.logiench.shardCore.core.itemRequirement.base.RequirementDef
import net.logiench.shardCore.core.stats.base.AttributeEnum
import net.logiench.shardLib.api.ShardLibProvider
import org.bukkit.Material
import org.jetbrains.annotations.Unmodifiable
import java.util.*

abstract class EquipmentItem(
	override val itemType: ItemType,
) : ShardItem(
	itemType = itemType,
) {
	override val material: Material by lazy { itemType.material }

	override fun createItemStack(): SuperItemStack {
		return ITEM_API.generate(material)
	}

	/**
	 * その装備固有のステータス。
	 * レベルによってスケーリングされます
	 *
	 * @return 固有のステータス
	 */
	abstract val uniqueBaseStats: @Unmodifiable NavigableMap<AttributeEnum, Double>

	/**
	 * メインステータス。
	 * レベルによって変化せず、完成度によりスケーリングされます。
	 *
	 * @return メインステータス
	 */
	abstract val mainStats: @Unmodifiable NavigableMap<AttributeEnum, Double>

	abstract val gemSlotSize: Int

	/**
	 * このアイテムの使用条件を取得します。
	 */
	abstract val requirementDefs: @Unmodifiable List<RequirementDef<*>>

	companion object {
		private val ITEM_API = ShardLibProvider.get().itemAPI
	}
}
