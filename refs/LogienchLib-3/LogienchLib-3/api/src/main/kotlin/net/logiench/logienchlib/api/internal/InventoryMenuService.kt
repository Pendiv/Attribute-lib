package net.logiench.logienchlib.api.internal

import io.avaje.inject.Secondary
import jakarta.inject.Singleton
import net.kyori.adventure.text.Component
import net.logiench.logienchlib.api.menu.InventoryMenu
import net.logiench.logienchlib.api.menu.InventoryMenuBuilder
import org.bukkit.event.inventory.InventoryType
import org.jetbrains.annotations.ApiStatus
import java.util.*


@ApiStatus.Internal
interface InventoryMenuService {

	fun chest(title: Component, rows: Int): InventoryMenuBuilder = throw UnsupportedService("GUIメニュー")

	fun chest(title: String, rows: Int): InventoryMenuBuilder = throw UnsupportedService("GUIメニュー")

	fun of(type: InventoryType, title: Component): InventoryMenuBuilder = throw UnsupportedService("GUIメニュー")

	fun get(playerId: UUID): InventoryMenu? = throw UnsupportedService("GUIメニュー")
}

@Secondary
@Singleton
internal class UnsupportedInventoryMenuService : InventoryMenuService
