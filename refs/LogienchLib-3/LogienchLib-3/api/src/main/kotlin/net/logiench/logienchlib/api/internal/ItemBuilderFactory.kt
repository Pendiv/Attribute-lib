package net.logiench.logienchlib.api.internal

import io.avaje.inject.Secondary
import jakarta.inject.Singleton
import net.logiench.logienchlib.api.item.ItemBuilder
import org.bukkit.Material
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
interface ItemBuilderFactory {

	fun create(material: Material): ItemBuilder = throw UnsupportedService("ItemBuilder")
}

@Secondary
@Singleton
internal class UnsupportedItemBuilderFactory : ItemBuilderFactory
