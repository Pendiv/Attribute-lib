package net.logiench.logienchlib.api.item

import net.kyori.adventure.text.Component
import net.logiench.logienchlib.api.InstanceHolder
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

interface ItemBuilder {
	companion object {
		@JvmStatic
		fun of(material: Material): ItemBuilder = InstanceHolder.itemBuilderFactory.create(material)
	}

	fun build(): ItemStack

	fun name(name: Component): ItemBuilder

	fun lore(lore: List<Component>): ItemBuilder

	fun lore(vararg lore: Component): ItemBuilder

	fun amount(amount: Int): ItemBuilder

	fun onLoreBuilder(builder: LoreBuilder.() -> Unit): ItemBuilder
}