package net.logiench.logienchlib.bukkit.config

import net.logiench.logienchlib.bukkit.BukkitTestBase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class BukkitConfigTest : BukkitTestBase() {

	@Test
	fun `パスサービスがプラグインのデータフォルダを返すこと`() {
		val service = BukkitConfigPathService()

		assertDoesNotThrow("プラグインからのデータフォルダ取得はエラーを吐かないこと") { service.getDataFolder(plugin) }
		assertThrows<IllegalArgumentException>("プラグイン以外を入れたらエラーを吐くこと") { service.getDataFolder(Any()) }
	}
}
