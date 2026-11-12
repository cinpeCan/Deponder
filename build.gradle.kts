buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-metadata-jvm:2.3.20")
        classpath("org.ow2.asm:asm:9.7.1")
    }
}

plugins {
    base
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.vanniktech.maven.publish) apply false
}

val deponderGroupId = "io.github.cinpecan"
val deponderVersion = "0.5.0"

subprojects {
    group = deponderGroupId
    version = deponderVersion

    plugins.withId("com.vanniktech.maven.publish") {
        extensions.configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
            publishToMavenCentral()

            val hasInMemoryKey =
                providers.gradleProperty("signingInMemoryKey").orNull?.isNotBlank() == true
            val hasKeyRingCredentials =
                providers.gradleProperty("signing.keyId").orNull?.isNotBlank() == true &&
                    providers.gradleProperty("signing.password").isPresent &&
                    providers.gradleProperty("signing.secretKeyRingFile").orNull?.isNotBlank() == true

            if (hasInMemoryKey || hasKeyRingCredentials) {
                signAllPublications()
            }

            pom {
                url.set("https://github.com/cinpeCan/Deponder")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("cinpeCan")
                        name.set("cinpe")
                        url.set("https://github.com/cinpeCan")
                    }
                }
                scm {
                    url.set("https://github.com/cinpeCan/Deponder")
                    connection.set("scm:git:https://github.com/cinpeCan/Deponder.git")
                    developerConnection.set("scm:git:https://github.com/cinpeCan/Deponder.git")
                }
            }
        }
    }
}
