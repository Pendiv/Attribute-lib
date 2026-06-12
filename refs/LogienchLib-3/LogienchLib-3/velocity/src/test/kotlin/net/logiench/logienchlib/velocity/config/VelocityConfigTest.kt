package net.logiench.logienchlib.velocity.config

import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.plugin.PluginManager
import io.mockk.every
import io.mockk.mockk
import net.logiench.logienchlib.velocity.VelocityTestBase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.util.*

class VelocityConfigTest : VelocityTestBase() {

	@BeforeEach
	override fun setUp() {
		super.setUp()

		val pluginManager = mockk<PluginManager>()
		val pluginContainer = mockk<PluginContainer>()
		every { pluginContainer.description.id } returns "logienchlib" // 将来的にデータ管理用のクラスを作成してそこから持ってくる
		every { pluginManager.fromInstance(any()) } returns Optional.empty()
		every { pluginManager.fromInstance(plugin) } returns Optional.of(pluginContainer)
		every { proxyServer.pluginManager } returns pluginManager
	}

	@Test
	fun `パスサービスがプラグインのデータフォルダを返すこと`() {
		val service = VelocityConfigPathService(proxyServer)

		assertDoesNotThrow("プラグインからのデータフォルダ取得はエラーを吐かないこと") { service.getDataFolder(plugin) }
		assertThrows<IllegalArgumentException>("プラグイン以外を入れたらエラーを吐くこと") { service.getDataFolder(Any()) }
	}
}