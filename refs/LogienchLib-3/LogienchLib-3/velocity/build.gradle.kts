// ─────────────────────────────────────────────
// velocityモジュール
// 役割: Velocity用の実装とメインクラス
// 依存: api, core, Velocity API
// 内容: Velocityスケジューラを使った TimerServiceImpl など（GUI機能なし）
// ─────────────────────────────────────────────

plugins {
	// 個別ビルド用のfat JAR生成（配布はルートのshadowJarが担当）
	alias(libs.plugins.shadow)

	alias(libs.plugins.run.velocity)
}

dependencies {
	// APIモジュール
	api(project(":api"))
	// coreモジュール（共通実装）
	api(project(":core"))

	// ─── Velocity API ───
	// annotationProcessor でプラグイン情報（@Plugin）を自動処理
	compileOnly(libs.velocity.api)
	annotationProcessor(libs.velocity.api)

	// DI フレームワーク
	implementation(libs.avaje.inject)
	kapt(libs.avaje.inject.generator)

	// ─── テスト ───
	// Velocity専用モックライブラリは存在しないため MockK のみで対応
	// testImplementation: テスト実行時にも ProxyServer 等の型が必要なため compileOnly ではなく implementation
	testImplementation(libs.velocity.api)
	testImplementation(libs.junit.jupiter)
	testImplementation(libs.junit.platform.launcher) // JUnitバージョン統一層
	testImplementation(libs.mockk)
}

// ─────────────────────────────────────────────
// テンプレートからKotlinソースを生成するタスク
// src/main/templates/ 内の *.kt ファイルの ${version} を
// Gradleのプロジェクトバージョンに置換して build/generated/ に出力する
// ─────────────────────────────────────────────
val generateTemplates by tasks.registering(Copy::class) {
	description = "テンプレートファイル内の \${version} をGradleバージョンに置換してソースを生成する"

	from("src/main/templates")
	into(layout.buildDirectory.dir("generated/sources/templates/kotlin/main"))

	// 各行の ${version} をプロジェクトバージョンで置換
	filter { line: String ->
		line.replace("\${version}", project.version.toString())
	}
	filteringCharset = "UTF-8"
}

// 生成されたソースディレクトリをKotlinのソースセットに追加
// srcDir にタスクを渡すことで、コンパイル前に generateTemplates が自動実行される
kotlin.sourceSets.main {
	kotlin.srcDir(generateTemplates)
}

// kaptスタブ生成時、kapt自身が生成するクラス（LlVelocityModule等）への未解決参照を
// エラーにせず「エラー型」として処理を続行させる設定
// これにより「鶏と卵」の循環コンパイル問題を解消する
kapt {
	correctErrorTypes = true
}

// 個別ビルド用 shadowJar 設定
tasks.shadowJar {
	archiveClassifier.set("")
}

tasks.build {
	dependsOn(tasks.shadowJar)
}

// ─── デバッグ用サーバー起動設定 ───
tasks.runVelocity {
	// Velocityは頻繁なアップデートがあるため自動で切り替わるように
	velocityVersion("3.5.0-SNAPSHOT")
	// Windows環境の文字化け対策
	jvmArgs("-Dfile.encoding=UTF-8", "-Dsun.stdout.encoding=UTF-8", "-Dsun.stderr.encoding=UTF-8")
}
