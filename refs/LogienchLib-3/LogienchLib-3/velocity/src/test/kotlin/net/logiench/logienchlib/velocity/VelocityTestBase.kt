package net.logiench.logienchlib.velocity

import com.velocitypowered.api.proxy.ProxyServer
import io.mockk.clearAllMocks
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * Velocityモジュール用テストの共通ベースクラス。
 *
 * Velocityには MockBukkit 相当の専用モックライブラリが存在しないため、
 * MockK を使って ProxyServer などの依存をモックする。
 *
 * 使い方:
 * ```kotlin
 * class MyVelocityServiceTest : VelocityTestBase() {
 *
 *     @Test
 *     fun `プレイヤー数を取得できること`() {
 *         every { proxyServer.playerCount } returns 5
 *         assertEquals(5, proxyServer.playerCount)
 *     }
 * }
 * ```
 *
 * @property proxyServer Velocity の ProxyServer モック。テスト内で自由に使用可能。
 */
abstract class VelocityTestBase {

	/** Velocity の ProxyServer（MockK によるモック）。 */
	protected val proxyServer: ProxyServer = mockk(relaxed = true)
	protected val plugin = mockk<LogienchLibBootstrap>(relaxed = true)

	/**
	 * 各テスト開始前の初期化処理。
	 * サブクラスでオーバーライドする場合は super.setUp() を呼ぶこと。
	 */
	@BeforeEach
	open fun setUp() {
		// 必要に応じてサブクラスで追加の初期化を行う
	}

	/**
	 * 各テスト終了後のクリーンアップ処理。
	 * MockKが保持するすべてのモック状態をリセットする。
	 */
	@AfterEach
	open fun tearDown() {
		clearAllMocks()
	}
}
