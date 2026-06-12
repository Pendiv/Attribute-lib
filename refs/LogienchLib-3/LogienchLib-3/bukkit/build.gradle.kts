// ─────────────────────────────────────────────
// bukkitモジュール
// 役割: Paper(Bukkit)用の実装とメインクラス
// 依存: api, core, Paper API
// 内容: InventoryMenuManager、Bukkitスケジューラを使った TimerServiceImpl など
// ─────────────────────────────────────────────

plugins {
	// ShadowJar（依存ライブラリを1つのJARに同梱）
	alias(libs.plugins.shadow)
	// デバッグ用Paperサーバー起動
	alias(libs.plugins.run.paper)
}

dependencies {
	// APIモジュール
	api(project(":api"))
	// coreモジュール（共通実装）
	api(project(":core"))

	// ─── Paper / Bukkit API ───
	compileOnly(libs.paper.api)

	// Floodgate（統合版プレイヤー対応API）
	compileOnlyApi(libs.floodgate.api)

	// ProtocolLib（パケット操作）
	compileOnlyApi(libs.protocollib)

	// NBT-API（アイテムNBTデータ操作）
	api(libs.nbt.api)

	// LevelDB（KVストア）
	api(libs.leveldb.api)
	api(libs.leveldb)

	// NBT Utilities（Cloudburstmc NBTシリアライズ）
	api(libs.cloudburstmc.nbt)

	// DI フレームワーク
	implementation(libs.avaje.inject)
	kapt(libs.avaje.inject.generator)

	// ─── テスト ───
	// testCompileOnly: コンパイル時の型解決（ServerMock の親クラス org.bukkit.Server 等）に必要
	// 実行時は MockBukkit が 1.21 系の Paper API を内包して提供するため runtime は不要
	// ※ 1.20.6版paper-apiと1.21版MockBukkitのバージョン差があるため、
	//   実行時は testImplementation ではなく MockBukkitに任せることでクラス競合を回避
	testCompileOnly(libs.paper.api)
	testImplementation(libs.junit.jupiter)
	testImplementation(libs.junit.platform.launcher) // JUnitバージョン統一層（mockbukkitが引き込むバージョンと履揺を強制一致）
	testImplementation(libs.mockbukkit)  // BukkitサーバーAPIのモック（ServerMock, PlayerMock等）
	testImplementation(libs.mockk)
}

// plugin.yml / paper-plugin.yml へのバージョン埋め込み
tasks.processResources {
	filteringCharset = "UTF-8"
	val props = mapOf("version" to project.version)
	filesMatching("plugin.yml") { expand(props) }
	filesMatching("paper-plugin.yml") { expand(props) }
}

// kaptスタブ生成時、kapt自身が生成するクラス（LlBukkitModule等）への未解決参照を
// エラーにせず「エラー型」として処理を続行させる設定
// これにより「鶏と卵」の循環コンパイル問題を解消する
kapt {
	correctErrorTypes = true
}

// ShadowJar 設定（classifierなしで最終JARを生成）
tasks.shadowJar {
	archiveClassifier.set("")
}

// build タスクが shadowJar を必ず実行するように設定
tasks.build {
	dependsOn(tasks.shadowJar)
}

// ─── デバッグ用サーバー起動設定 ───
tasks.runServer {
	minecraftVersion("1.21.8")
	// Windows環境の文字化け対策
	jvmArgs("-Dfile.encoding=UTF-8", "-Dsun.stdout.encoding=UTF-8", "-Dsun.stderr.encoding=UTF-8")
}
