package net.logiench.logienchlib.core.config

import io.mockk.every
import io.mockk.mockk
import net.logiench.logienchlib.api.config.LConfigPath
import net.logiench.logienchlib.api.internal.ConfigPathService
import net.logiench.logienchlib.core.CoreTestBase
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class CoreConfigTest : CoreTestBase() {

	private lateinit var configPathService: ConfigPathService
	private lateinit var configService: ConfigServiceImpl

	@BeforeEach
	override fun setUp() {
		super.setUp()

		this.configPathService = mockk<ConfigPathService>(relaxed = true)
		this.configService = ConfigServiceImpl(configPathService)
	}

	@Test
	fun `ディレクトリの読み込みでエラーが起きること`(@TempDir tempDir: Path) {
		val directory = tempDir.resolve("hogehoge").toFile()
		directory.mkdirs()
		val lConfigPath = configService.createPath(directory)

		assertTrue(lConfigPath.isDirectory())

		assertThrows<IllegalArgumentException>("ディレクトリを対象にするとロードに失敗すること") {
			lConfigPath.load()
		}
	}

	@Nested
	inner class `コンフィグの内部処理` {

		// @formatter:off  Contentのインデントをタブにするとエラーになるのでスペース維持のため
		private val configContent = """
				# Comment1
				yaml1:
				    test1: 1
				    # Comment2
				    test2: 4
				# Comment3
				yaml2: 5
				
				# Comment4
			""".trimIndent()
		// @formatter:on
		@TempDir
		lateinit var tempDir: Path
		lateinit var lConfigPath: LConfigPath

		@BeforeEach
		fun setUp() {
			every { configPathService.getResource(any(), any()) } returns configContent.byteInputStream()
			every { configPathService.getDataFolder(any()) } returns tempDir.toFile()

			val fileName = "config.yml"
			tempDir.resolve(fileName).toFile().writeText(configContent)
			this.lConfigPath = configService.createFactory(Any()).fromRelative(fileName)
		}

		@Test
		fun `ファイルの取得が行えること`() {
			assertDoesNotThrow("ファイルのロードに成功した") {
				val config = lConfigPath.load()
				assertNotNull(config, "ロードの返り値がnullではない")
			}
		}

		@Test
		fun `loadWithUpdateのテスト`() {
			// 基本的なconfig操作
			val config = lConfigPath.load()
			config.set("yaml2", 10)
			config.remove("yaml1.test1")
			config.save()
			assertEquals(10, config.getInt("yaml2"), "コンフィグの編集が行われている")
			assertFalse(config.contains("yaml1.test1"), "コンフィグのパスが削除されている")

			val reloadedConfig = lConfigPath.load()
			assertEquals(10, reloadedConfig.getInt("yaml2"), "コンフィグのファイル自体が更新されている(データ上書き)")
			assertFalse(reloadedConfig.contains("yaml1.test1"), "コンフィグのファイル自体が更新されている(データ削除)")

			val updateConfig = lConfigPath.loadWithUpdate()
			assertEquals(1, updateConfig.getInt("yaml1.test1"), "コンフィグの不足したデータが復元されている")
			assertEquals(10, updateConfig.getInt("yaml2"), "データが復元されているが、他の値は維持されている")
		}

		@AfterEach
		fun forceUnlock() {
			// gcのクリアを行わないと仮フォルダの削除に失敗する
			System.gc()
		}
	}
}
