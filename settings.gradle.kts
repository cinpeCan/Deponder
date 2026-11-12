pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Deponder"
include(":deponder-core")
include(":deponder")
include(":deponder-compose")
include(":deponder-bom")
if (file("app").exists()) {
    include(":app")
    if (file("app/shared").exists()) {
        include(":app:shared")
    }
}
