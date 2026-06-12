plugins {
	`maven-publish`
	alias(libs.plugins.shadow)
}

dependencies {
	// ─── プラットフォーム API（コンパイル時のみ、JARには含めない）───
	// APIモジュールとしてBukkit/Velocityの型を参照できるようにする
	compileOnly(libs.paper.api)
	compileOnly(libs.velocity.api)

	// BoostedYAML (API経由で外部にも型を公開)
	api(libs.boosted.yaml)

	// DI フレームワーク
	implementation(libs.avaje.inject)
	kapt(libs.avaje.inject.generator)
}

kapt {
	correctErrorTypes = true
}

publishing {
	publications {
		create<MavenPublication>("shadow") {
			artifactId = "logienchlib"
			// ShadowJarで生成された成果物を公開対象にする
			project.shadow.component(this)
		}
	}

	repositories {
		maven {
			name = "GitHubPackages"
			url = uri("https://maven.pkg.github.com/Logiench/LogienchLib")
			credentials {
				// 環境変数、またはローカルの gradle.properties からトークンを取得
				username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("GITHUB_ACTOR") as String?
				password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("GITHUB_TOKEN") as String?
			}
		}
	}
}
