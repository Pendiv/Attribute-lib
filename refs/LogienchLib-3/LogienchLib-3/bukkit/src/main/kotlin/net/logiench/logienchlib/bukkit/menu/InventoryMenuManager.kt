package net.logiench.logienchlib.bukkit.menu

import jakarta.inject.Singleton
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.logiench.logienchlib.api.menu.CancelPolicy
import net.logiench.logienchlib.bukkit.menu.handler.MenuClickContextImpl
import net.logiench.logienchlib.bukkit.menu.handler.MenuCloseContextImpl
import net.logiench.logienchlib.bukkit.menu.handler.MenuDragContextImpl
import net.logiench.logienchlib.bukkit.menu.handler.MenuOpenContextImpl
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.*
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Singleton
class InventoryMenuManager(private val plugin: JavaPlugin) : Listener {

	private val openMenus: MutableMap<UUID, InventoryMenuImpl> = mutableMapOf()

	fun register(player: Player, menu: InventoryMenuImpl) {
		openMenus[player.uniqueId] = menu
	}

	fun unregister(player: Player) {
		openMenus.remove(player.uniqueId)?.run {
			taskPool.cancel()
		}
	}

	fun getMenu(playerId: UUID): InventoryMenuImpl? {
		return openMenus[playerId]
	}

	@EventHandler
	private fun onInventoryOpen(ev: InventoryOpenEvent) {
		val player = ev.player as? Player ?: return
		val menu = openMenus[player.uniqueId] ?: return

		val ctx = MenuOpenContextImpl(player, menu, ev)

		try {
			menu.openListeners.forEach { it.accept(ctx) }
			menu.modules.forEach { it.onOpen(ctx) }
		} catch (e: Exception) {
			ev.isCancelled = true
			printInventoryMenuException("Open", player, e)
		} finally {
			if (ev.isCancelled) {
				unregister(player)
			}
		}
	}

	@EventHandler
	private fun onInventoryClose(ev: InventoryCloseEvent) {
		val player = ev.player as? Player ?: return
		val menu = openMenus[player.uniqueId] ?: return

		val ctx = MenuCloseContextImpl(player, menu, ev)

		try {
			menu.closeListeners.forEach { it.accept(ctx) }
			menu.modules.forEach { it.onClose(ctx) }
		} catch (e: Exception) {
			printInventoryMenuException("Close", player, e)
		} finally {
			unregister(player)
		}
	}

	@EventHandler
	private fun onInventoryClick(ev: InventoryClickEvent) {
		val player = ev.whoClicked as? Player ?: return
		val menu = openMenus[player.uniqueId] ?: return

		val ctx = MenuClickContextImpl(player, menu, ev)

		val cancel = when (menu.cancelPolicy.clickCancel) {
			CancelPolicy.ClickCancel.TOP_ONLY -> ctx.isTopInventory || ctx.isShiftClick || ctx.clickType == ClickType.DOUBLE_CLICK
			CancelPolicy.ClickCancel.BOTTOM_ONLY -> ctx.isBottomInventory || ctx.isShiftClick || ctx.clickType == ClickType.DOUBLE_CLICK
			CancelPolicy.ClickCancel.ALL -> true
			else -> false
		}
		if (cancel) ctx.cancel()

		try {
			menu.clickListeners.forEach { it.accept(ctx) }
			menu.modules.forEach { it.onClick(ctx) }

			if (ctx.isTopInventory) {
				menu.slotEvents[ctx.slot]?.accept(ctx)
			}

			ev.isCancelled = ctx.isCancelled
		} catch (e: Exception) {
			ev.isCancelled = true
			printInventoryMenuException("Click", player, e)
		}
	}

	@EventHandler
	private fun onInventoryDrag(ev: InventoryDragEvent) {
		val player = ev.whoClicked as? Player ?: return
		val menu = openMenus[player.uniqueId] ?: return

		val ctx = MenuDragContextImpl(player, menu, ev)
		val cancel = when (menu.cancelPolicy.dragCancel) {
			CancelPolicy.DragCancel.TOP_ONLY -> ctx.affectsTopInventory
			CancelPolicy.DragCancel.BOTTOM_ONLY -> ctx.affectsBottomInventory
			CancelPolicy.DragCancel.ALL -> true
			else -> false
		}
		if (cancel) ctx.cancel()

		try {
			menu.dragListeners.forEach { it.accept(ctx) }
			menu.modules.forEach { it.onDrag(ctx) }

			ev.isCancelled = ctx.isCancelled
		} catch (e: Exception) {
			ev.isCancelled = true
			printInventoryMenuException("Drag", player, e)
		}
	}

	@EventHandler
	private fun onPlayerQuit(ev: PlayerQuitEvent) {
		unregister(ev.player)
	}

	private fun printInventoryMenuException(source: String, player: Player, e: Exception) {
		val logger = plugin.logger

		val time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"))
		val reason = "InventoryMenu-$source"
		player.sendMessage(
			Component.text("", NamedTextColor.RED)
				.append(Component.text("[$time] '$reason' の処理中にエラーが発生しました", null, TextDecoration.BOLD))
				.append(Component.newline())
				.append(Component.text("このエラーを運営に報告してください"))
//				.append(Component.newline())
//				.append(Component.text("'$reason' an exception occurred while processing"))
		)

		logger.warning("=".repeat(40))
		logger.warning("** '$reason' の処理中にエラーが発生しました **")
		logger.warning(e.toString())
		logger.warning("Stack trace:")

		for (element in e.stackTrace) {
			logger.warning("\n\tat $element")
		}
		logger.warning("=".repeat(40))
	}
}