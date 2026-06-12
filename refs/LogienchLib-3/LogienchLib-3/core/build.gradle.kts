// ─────────────────────────────────────────────
// coreモジュール
// 役割: プラットフォームに依存しない共通実装
// 依存: api, HikariCP, avaje-inject
// 内容: データベース制御 (LDatabase)、汎用Configラッパーなど
// ─────────────────────────────────────────────

dependencies {
	// APIモジュール（インターフェース群）
	api(project(":api"))

	// コネクションプール
	api(libs.hikaricp)

	// DI フレームワーク
	implementation(libs.avaje.inject)
	kapt(libs.avaje.inject.generator)

	// ─── テスト ───
	testImplementation(libs.junit.jupiter)
	testImplementation(libs.junit.platform.launcher) // JUnitバージョン統一層
	testImplementation(libs.mockk)
}
