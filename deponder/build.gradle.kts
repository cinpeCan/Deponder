import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.binary.compatibility.validator)
}

if (file("publisher.gradle").exists()) {
    apply(from = "publisher.gradle")
}

android {
    namespace = "com.cinpe.deponder"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        encoding = "utf-8"
    }

    lint {
        targetSdk = libs.versions.targetSdk.get().toInt()
    }

    testOptions {
        targetSdk = libs.versions.targetSdk.get().toInt()
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = false
            all {
                it.maxHeapSize = "2g"
                it.systemProperty("robolectric.graphicsMode", "NATIVE")
            }
        }
    }
}

kotlin {
    explicitApi()
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

dependencies {
    api(libs.androidx.annotation)
    api(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
