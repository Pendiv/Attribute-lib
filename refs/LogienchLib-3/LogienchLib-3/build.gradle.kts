import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	java
	`java-library`
	// 全モジュールを1つにまとめた配布用JARを生成するプラグイン
	alias(libs.plugins.shadow)
	// 各サブモジュールで個別に有効化するプラグイン（ここでは宣言のみ）
	alias(libs.plugins.kotlin.jvm) apply false
	alias(libs.plugins.kotlin.kapt) apply false
}

val globalVersion = "3.0.0-SNAPSHOT"

// ─────────────────────────────────────────────
// 全サブモジュール共通の設定
// ─────────────────────────────────────────────
allprojects {
	plugins.apply(JavaPlugin::class.java)
	plugins.apply("java-library")
	plugins.apply("org.jetbrains.kotlin.jvm")
	plugins.apply("org.jetbrains.kotlin.kapt")

	group = "net.logiench"
	version = globalVersion

	repositories {
		mavenCentral()
		// PaperMC / Velocity
		maven("https://repo.papermc.io/repository/maven-public/")
		// GeyserMC / Floodgate
		maven("https://repo.opencollab.dev/main/")
		// NBT-API
		maven("https://repo.codemc.io/repository/maven-public/")
	}

	// Java ツールチェーン
	java {
		toolchain {
			languageVersion.set(JavaLanguageVersion.of(21))
		}
	}

	// Kotlin コンパイルターゲット
	tasks.withType<KotlinCompile>().configureEach {
		compilerOptions {
			jvmTarget.set(JvmTarget.JVM_21)
		}
	}

	// Java コンパイル文字コード
	tasks.withType<JavaCompile>().configureEach {
		options.encoding = "UTF-8"
	}

	// JUnit5プラットフォームを有効化（./gradlew :モジュール:test でも一括でも実行可能）
	tasks.withType<Test>().configureEach {
		useJUnitPlatform()
	}
}

// ─────────────────────────────────────────────
// 配布用JARの集約設定（ルートプロジェクト）
// api + core + bukkit + velocity を1つのJARにまとめる
// compileOnly宣言された Paper API / Velocity API は含まれない（サーバー側が提供）
// ─────────────────────────────────────────────
dependencies {
	implementation(project(":api"))
	implementation(project(":core"))
	implementation(project(":bukkit"))
	implementation(project(":velocity"))
}

tasks.shadowJar {
	// 依存ライブラリのリロケーション
	// （他プラグインとのクラス衝突を避けたい場合はコメント解除）
//	relocate("de.tr7zw.changeme.nbtapi", "de.tr7zw.logiench.nbtapi")

	// classifierなしで最終配布JARを生成（例: LogienchLib-3.0.0-SNAPSHOT.jar）
	archiveClassifier.set("")

	// shadowJar生成のときは他のモジュールのものも実行する
	dependsOn(":velocity:shadowJar")
	dependsOn(":bukkit:shadowJar")
}

// build タスクで shadowJar を必ず実行
tasks.build {
	dependsOn(tasks.shadowJar)
}
