package net.logiench.logienchlib.core

import io.mockk.clearAllMocks
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * coreモジュール用テストの共通ベースクラス。
 *
 * MockKのモックを @BeforeEach でセットアップし、
 * @AfterEach で確実にクリアすることで、テスト間の状態汚染を防ぐ。
 *
 * 使い方:
 * ```kotlin
 * class MyServiceTest : CoreTestBase() {
 *     private val mockDependency = mockk<SomeDependency>()
 *
 *     @Test
 *     fun `何かのテスト`() { ... }
 * }
 * ```
 */
abstract class CoreTestBase {

	/**
	 * 各テスト開始前の初期化処理。
	 * サブクラスでオーバーライドする場合は super.setUp() を呼ぶこと。
	 */
	@BeforeEach
	open fun setUp() {
		// 必要に応じてサブクラスで拡張する
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
