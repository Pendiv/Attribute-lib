package net.logiench.logienchlib.bukkit

import be.seeseemelk.mockbukkit.MockBukkit
import be.seeseemelk.mockbukkit.ServerMock
import io.mockk.clearAllMocks
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Bukkit（Paper）モジュール用テストの共通ベースクラス。
 *
 * MockBukkit によって仮想のBukkitサーバー環境を起動し、
 * テスト終了後に確実にアンモックする。
 *
 * 使い方:
 * ```kotlin
 * class MyCommandTest : BukkitTestBase() {
 *
 *     @Test
 *     fun `プレイヤーがコマンドを実行できること`() {
 *         val player = server.addPlayer()
 *         // ... テストロジック
 *     }
 * }
 * ```
 *
 * @property server モックサーバーインスタンス。テスト内で自由に使用可能。
 */
abstract class BukkitTestBase {

	/** MockBukkitが提供するモックサーバー。プレイヤーの追加やワールド操作に使用する。 */
	protected lateinit var server: ServerMock
	protected lateinit var plugin: LogienchLibPlugin

	/**
	 * 各テスト開始前にモックサーバーを起動する。
	 * サブクラスでオーバーライドする場合は super.setUp() を呼ぶこと。
	 */
	@BeforeEach
	open fun setUp() {
		this.server = MockBukkit.mock()
		this.plugin = MockBukkit.load(LogienchLibPlugin::class.java)
	}

	// テスト環境の根幹のテスト
	@Test
	fun `テストインフラが正常に動作すること`() {
		// MockBukkit のサーバーが正しく起動しているか確認
		assertNotNull(server, "MockBukkitのサーバーが起動していること")
		assertNotNull(plugin, "プラグインがロードされていること")
	}

	/**
	 * 各テスト終了後にモックサーバーを停止し、MockKの状態もリセットする。
	 * テスト間でサーバー状態が引き継がれないようにするために必須。
	 */
	@AfterEach
	open fun tearDown() {
		MockBukkit.unmock()
		clearAllMocks()
	}
}
