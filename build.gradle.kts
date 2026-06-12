plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.69-stable")

    testImplementation("io.papermc.paper:paper-api:26.1.2.build.69-stable")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    withType<Javadoc>().configureEach {
        options.encoding = "UTF-8"
    }

    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("26.1.2")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    test {
        useJUnitPlatform()
    }

    processResources {
        // expand はこの charset でファイルを読む。未指定だとプラットフォーム既定(Windows では
        // Shift-JIS)になり、UTF-8 の日本語コメントが壊れて plugin.yml が読めなくなる
        filteringCharset = "UTF-8"
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
