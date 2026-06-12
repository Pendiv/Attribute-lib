package net.logiench.shardCore.core.item.base.def

import net.kyori.adventure.text.Component
import net.logiench.shardCore.core.item.base.gem.GemActionRegistry
import net.logiench.shardCore.core.stats.base.AttributeEnum
import org.bukkit.Material
import org.jetbrains.annotations.Unmodifiable
import java.util.*

abstract class GemItem(
	override val material: Material
) : ShardItem(
	ItemType.GEM, material
) {
	abstract val targetItemGroups: @Unmodifiable List<ItemGroup>

	abstract val additionalEffects: @Unmodifiable NavigableMap<AttributeEnum, Double>

	abstract val description: @Unmodifiable List<Component?>

	abstract fun registerAction(registry: GemActionRegistry)
}
