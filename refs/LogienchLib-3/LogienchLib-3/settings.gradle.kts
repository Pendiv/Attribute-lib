plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}
rootProject.name = "LogienchLib"

include("api")
include("core")
include("bukkit")
include("velocity")