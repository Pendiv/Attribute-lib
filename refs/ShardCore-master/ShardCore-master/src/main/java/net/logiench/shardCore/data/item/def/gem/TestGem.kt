package net.logiench.shardCore.data.item.def.gem

import net.kyori.adventure.text.Component
import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack
import net.logiench.shardCore.ShardCore
import net.logiench.shardCore.core.item.base.def.GemItem
import net.logiench.shardCore.core.item.base.def.ItemGroup
import net.logiench.shardCore.core.item.base.def.Rarity
import net.logiench.shardCore.core.item.base.gem.GemActionRegistry
import net.logiench.shardCore.core.item.base.gem.GemTrigger
import net.logiench.shardCore.core.item.base.module.ItemModule
import net.logiench.shardCore.core.stats.base.AttributeEnum
import org.bukkit.Material
import java.util.*

class TestGem : GemItem(Material.DIAMOND) {
	override val id = "test"

	override val name = Component.text("Test-Gem")

	override val rarity = Rarity.COMMON

	override fun createItemStack(): SuperItemStack {
		return SuperItemStack.init(material)
	}

	override val modules = listOf<Class<out ItemModule<*>>>()

	override val targetItemGroups = listOf<ItemGroup>()

	override val additionalEffects: NavigableMap<AttributeEnum, Double> = Collections.emptyNavigableMap()

	override val description = listOf<Component>()

	override fun registerAction(registry: GemActionRegistry) {
		registry.addListener(GemTrigger.ON_ATTACK) { ctx ->
			ctx.character.player.sendMessage("ジェム！")
			ShardCore.getInstance().logger.info("ジェムの効果発動!")
		}
	}
}
