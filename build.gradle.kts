buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-metadata-jvm:2.4.21")
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

import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.EnvSpec
import org.jetbrains.kotlin.gradle.targets.js.npm.LockStoreTask
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec

val deponderGroupId = "io.github.cinpecan"
val deponderVersion = "0.7.0"

fun <P : Plugin<*>, S : EnvSpec<*>> Project.disableToolDownloadUrl(
    pluginClass: Class<P>,
    specClass: Class<S>,
) {
    plugins.withType(pluginClass) {
        extensions.configure(specClass) {
            downloadBaseUrl.unset()
            downloadBaseUrl.unsetConvention()
        }
    }
}

allprojects {
    disableToolDownloadUrl(WasmNodeJsPlugin::class.java, WasmNodeJsEnvSpec::class.java)
    disableToolDownloadUrl(WasmYarnPlugin::class.java, WasmYarnRootEnvSpec::class.java)
    @OptIn(ExperimentalWasmDsl::class)
    disableToolDownloadUrl(BinaryenPlugin::class.java, BinaryenEnvSpec::class.java)
}

// KGP registers npm LockStoreTask alongside Yarn; running both in Gradle 9.6 fails without package-lock.json.
plugins.withType(WasmYarnPlugin::class.java) {
    tasks.withType(LockStoreTask::class.java)
        .matching { it.name == "kotlinWasmUpgradePackageLock" || it.name == "kotlinWasmStorePackageLock" }
        .configureEach {
            enabled = false
            setDependsOn(emptyList<Any>())
            inputFile.unset()
            inputFile.unsetConvention()
            additionalInputFiles.setFrom()
        }
}

subprojects {
    group = deponderGroupId
    version = deponderVersion

    tasks.withType<AbstractTestTask>().configureEach {
        testLogging {
            events("failed")
            exceptionFormat = TestExceptionFormat.FULL
            showExceptions = true
            showCauses = true
            showStackTraces = true
        }
    }

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
