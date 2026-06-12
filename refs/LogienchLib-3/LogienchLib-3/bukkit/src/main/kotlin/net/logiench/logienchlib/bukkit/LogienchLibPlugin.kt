package net.logiench.logienchlib.bukkit

import io.avaje.inject.BeanScope
import net.logiench.logienchlib.api.InstanceHolder
import net.logiench.logienchlib.api.LlApiModule
import net.logiench.logienchlib.api.menu.InventoryMenu
import net.logiench.logienchlib.api.timer.Timer
import net.logiench.logienchlib.bukkit.menu.InventoryMenuManager
import net.logiench.logienchlib.bukkit.timer.PlayerLTaskPoolImpl
import net.logiench.logienchlib.core.LlCoreModule
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

// MockBukkitでテストを動かすにはopenにしないといけなかったのでとっても嫌だけどopenに
open class LogienchLibPlugin : JavaPlugin() {

	private var beanScope: BeanScope? = null

	override fun onLoad() {
		val beanScope = BeanScope.builder()
			.classLoader(this.classLoader)
			.bean(JavaPlugin::class.java, this) // DIに登録
			.modules(LlBukkitModule(), LlCoreModule(), LlApiModule())
			.build()

		setState(InstanceHolder.State.BEFORE_LOADED)
		@Suppress("DEPRECATION_ERROR")
		InstanceHolder.setLoad(beanScope.get(InstanceHolder.LoadContext::class.java))

		this.beanScope = beanScope
	}

	override fun onEnable() {
		val beanScope = this.beanScope ?: return

		// 各種イベント登録
		val pluginManager = server.pluginManager
		// タスクプール
		val playerLTaskPool = beanScope.get(PlayerLTaskPoolImpl::class.java)
		pluginManager.registerEvents(playerLTaskPool, this)
		// メニュー
		val menuManager = beanScope.get(InventoryMenuManager::class.java)
		pluginManager.registerEvents(menuManager, this)


		setState(InstanceHolder.State.BEFORE_ENABLED)
		@Suppress("DEPRECATION_ERROR")
		InstanceHolder.setEnabled(beanScope.get(InstanceHolder.EnableContext::class.java))

		//TEST START

		pluginManager.registerEvents(Listener, this)

		/*val factory = LConfigPath.createFactory(this)
		val path = factory.fromRelative("config.yml")
		path.saveResource()
		val config = path.loadWithUpdate()
		for (key in config.getKeys(true)) {
			println("$key: ${config.get(key)}")
		}
		config.set("key.value", "WORLD!")
		config.save()
*/
		// TEST END
	}

	private fun setState(state: InstanceHolder.State) {
		InstanceHolder.javaClass.getDeclaredField("state").apply {
			isAccessible = true
			set(InstanceHolder, state)
		}
	}

	override fun onDisable() {
		val beanScope = this.beanScope ?: return
		beanScope.close()
	}
}


object Listener : Listener {
	@EventHandler
	fun test(ev: PlayerInteractEvent) {
		val player = ev.player
		InventoryMenu.chest("TEST", 6).open(player) {
			setItem(1, 1, ItemStack(Material.DIAMOND_SWORD)) {
				player.sendMessage("CLICK!")
			}
			setItem(1, 2, ItemStack(Material.OAK_LOG, 64))
			onClick {
				println("CLICKED! top:${isTopInventory} bottom:${isBottomInventory} drop:${isDrop} hotbar:${isHotbar} pickup:${isPickup} place:${isPlace}")
			}

			Timer.on(20, 20, taskPool) {
				println("TIMER")
			}
		}
	}
}
