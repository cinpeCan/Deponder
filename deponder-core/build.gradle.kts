import java.io.ByteArrayOutputStream
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.dokka)
    alias(libs.plugins.vanniktech.maven.publish)
}

kotlin {
    explicitApi()

    @OptIn(ExperimentalAbiValidation::class)
    abiValidation()

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    iosArm64()
    iosSimulatorArm64()
    macosArm64()

    @OptIn(ExperimentalSwiftExportDsl::class)
    swiftExport {
        moduleName.set("DeponderCore")
        flattenPackage.set("com.cinpe.deponder.core")
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

tasks.withType<KotlinNativeCompile>().configureEach {
    if (name.startsWith("compileSwiftExportMainKotlin")) {
        compilerOptions.suppressWarnings.set(true)
    }
}

@DisableCachingByDefault(because = "Assembles Swift Export XCFramework and updates generated SPM sources")
abstract class AssembleDeponderAppleSwiftPackageTask : DefaultTask() {

    @get:Inject
    abstract val execOperations: ExecOperations

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceInputs: ConfigurableFileCollection

    @get:Internal
    abstract val rootProjectDir: DirectoryProperty

    @get:Internal
    abstract val projectBuildDir: DirectoryProperty

    @get:OutputDirectory
    abstract val appleGeneratedDir: DirectoryProperty

    @get:Internal
    abstract val packageSwiftFile: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val updatePackageSwiftChecksum: Property<Boolean>

    @get:Input
    abstract val projectVersion: Property<String>

    @get:Input
    @get:Optional
    abstract val javaHomePath: Property<String>

    @get:Input
    @get:Optional
    abstract val developerDirPath: Property<String>

    @get:OutputDirectory
    abstract val xcframeworkOutputDir: DirectoryProperty

    @get:OutputFile
    abstract val xcframeworkZipOutputFile: RegularFileProperty

    @get:OutputFile
    abstract val checksumOutputFile: RegularFileProperty

    private data class AppleSlice(
        val targetName: String,
        val targetPascal: String,
        val sdkName: String,
        val archs: String,
    )

    @TaskAction
    fun assemble() {
        val rootDir = rootProjectDir.get().asFile
        val buildDir = projectBuildDir.get().asFile
        val gradlewFile = File(rootDir, "gradlew")
        val gradleCacheDir = File(buildDir, "tmp/swift-export-gradle-cache")
        val javaHome = javaHomePath.orNull?.takeIf { it.isNotBlank() }
        val developerDir = developerDirPath.orNull?.takeIf { it.isNotBlank() }

        val slices = listOf(
            AppleSlice("macosArm64", "MacosArm64", "macosx", "arm64"),
            AppleSlice("iosArm64", "IosArm64", "iphoneos", "arm64"),
            AppleSlice("iosSimulatorArm64", "IosSimulatorArm64", "iphonesimulator", "arm64"),
        )

        val libsStageDir = File(buildDir, "tmp/xcframework-libs")
        libsStageDir.deleteRecursively()
        libsStageDir.mkdirs()

        val libraryArgs = mutableListOf<String>()
        for (slice in slices) {
            val spmStageDir = File(buildDir, "swift-export-stage/${slice.targetName}")
            spmStageDir.mkdirs()

            execOperations.exec {
                workingDir(rootDir)
                if (javaHome != null) {
                    environment("JAVA_HOME", javaHome)
                }
                if (developerDir != null) {
                    environment("DEVELOPER_DIR", developerDir)
                }
                commandLine(
                    gradlewFile.absolutePath,
                    "-p",
                    rootDir.absolutePath,
                    "--no-daemon",
                    "--no-configuration-cache",
                    "--project-cache-dir",
                    gradleCacheDir.absolutePath,
                    ":deponder-core:${slice.targetName}ReleaseGenerateSPMPackage",
                    ":deponder-core:linkSwiftExportBinaryReleaseStatic${slice.targetPascal}",
                    "-Pkotlin.envOverride.CONFIGURATION=Release",
                    "-Pkotlin.envOverride.SDK_NAME=${slice.sdkName}",
                    "-Pkotlin.envOverride.ARCHS=${slice.archs}",
                    "-Pkotlin.envOverride.BUILT_PRODUCTS_DIR=${spmStageDir.absolutePath}",
                    "-Pkotlin.envOverride.TARGET_BUILD_DIR=${spmStageDir.absolutePath}",
                    "-Pkotlin.envOverride.FRAMEWORKS_FOLDER_PATH=Frameworks",
                )
            }

            val sourceStaticLib = File(
                buildDir,
                "bin/${slice.targetName}/SwiftExportBinaryReleaseStatic/libSwiftExportBinary.a",
            )
            check(sourceStaticLib.isFile) {
                "Missing Swift Export static library at ${sourceStaticLib.absolutePath}"
            }

            val stagedLibDir = File(libsStageDir, slice.targetName)
            stagedLibDir.mkdirs()
            val renamedStaticLib = File(stagedLibDir, "libDeponderCoreBridge.a")
            sourceStaticLib.copyTo(renamedStaticLib, overwrite = true)

            libraryArgs.add("-library")
            libraryArgs.add(renamedStaticLib.absolutePath)
        }

        val xcframeworkDir = xcframeworkOutputDir.get().asFile
        xcframeworkDir.deleteRecursively()
        xcframeworkDir.parentFile.mkdirs()

        execOperations.exec {
            workingDir(rootDir)
            if (developerDir != null) {
                environment("DEVELOPER_DIR", developerDir)
            }
            commandLine(
                buildList {
                    add("/usr/bin/xcrun")
                    add("xcodebuild")
                    add("-create-xcframework")
                    addAll(libraryArgs)
                    add("-output")
                    add(xcframeworkDir.absolutePath)
                },
            )
        }

        val xcframeworkZip = xcframeworkZipOutputFile.get().asFile
        xcframeworkZip.delete()
        xcframeworkZip.parentFile.mkdirs()

        execOperations.exec {
            workingDir(xcframeworkDir.parentFile)
            commandLine(
                "/usr/bin/ditto",
                "-c",
                "-k",
                "--sequesterRsrc",
                "--keepParent",
                xcframeworkDir.name,
                xcframeworkZip.absolutePath,
            )
        }

        val checksumOut = ByteArrayOutputStream()
        execOperations.exec {
            workingDir(rootDir)
            if (developerDir != null) {
                environment("DEVELOPER_DIR", developerDir)
            }
            standardOutput = checksumOut
            commandLine(
                "/usr/bin/xcrun",
                "swift",
                "package",
                "compute-checksum",
                xcframeworkZip.absolutePath,
            )
        }
        val checksum = checksumOut.toString(Charsets.UTF_8).trim()
        check(checksum.matches(Regex("^[0-9a-fA-F]{64}$"))) {
            "Unexpected SHA-256 checksum from swift package compute-checksum: '$checksum'"
        }

        val checksumFile = checksumOutputFile.get().asFile
        checksumFile.parentFile.mkdirs()
        checksumFile.writeText("$checksum\n", Charsets.UTF_8)

        val generatedSourcesSrc = File(buildDir, "SPMPackage/macosArm64/Release/Sources")
        check(generatedSourcesSrc.isDirectory) {
            "Missing generated SPM Sources directory at ${generatedSourcesSrc.absolutePath}"
        }
        val generatedTargetDir = appleGeneratedDir.get().asFile
        generatedTargetDir.deleteRecursively()
        generatedSourcesSrc.copyRecursively(generatedTargetDir, overwrite = true)

        val linkDirectiveRegex = Regex("""^\s*link\s+".*"\s*$""")
        generatedTargetDir.walkTopDown()
            .filter { it.isFile && it.name == "module.modulemap" }
            .forEach { moduleMapFile ->
                val filteredLines = moduleMapFile.readLines(Charsets.UTF_8)
                    .filterNot { line -> linkDirectiveRegex.matches(line) }
                moduleMapFile.writeText(
                    filteredLines.joinToString(separator = "\n", postfix = "\n"),
                    Charsets.UTF_8,
                )
            }

        sanitizeGeneratedSwiftSources(generatedTargetDir)

        if (updatePackageSwiftChecksum.getOrElse(false)) {
            val packageSwift = packageSwiftFile.get().asFile
            check(packageSwift.isFile) {
                "Package.swift not found at ${packageSwift.absolutePath}"
            }
            val version = projectVersion.get()
            val original = packageSwift.readText(Charsets.UTF_8)
            val urlPattern = Regex("""(releases/download/v)[^/]+(/DeponderCoreBridge\.xcframework\.zip)""")
            val checksumPattern = Regex("""(checksum:\s*")[^"]*(")""")
            check(urlPattern.containsMatchIn(original)) {
                "Could not find release download URL in ${packageSwift.absolutePath}"
            }
            check(checksumPattern.containsMatchIn(original)) {
                "Could not find checksum field in ${packageSwift.absolutePath}"
            }
            val updated = original
                .let { urlPattern.replaceFirst(it, "$1$version$2") }
                .let { checksumPattern.replaceFirst(it, "$1$checksum$2") }
            packageSwift.writeText(updated, Charsets.UTF_8)
        }
    }

    private fun sanitizeGeneratedSwiftSources(generatedTargetDir: File) {
        val unreachableEnumDefaultRegex = Regex("""([ \t]*case \.[^:\n]+:[^\n]*\r?\n)[ \t]*default: fatalError\(\)[ \t]*\r?\n""")
        val voidResultClosureRegex = Regex("""let _result = (originalBlock\(_arg0\))\r?\n([ \t]*)return \{ _result; return true \}\(\)""")
        generatedTargetDir.walkTopDown()
            .filter { it.isFile && it.extension == "swift" }
            .forEach { swiftFile ->
                var content = swiftFile.readText(Charsets.UTF_8)
                content = content
                    .replace("@_implementationOnly import ", "internal import ")
                    .replace("KotlinBridgeable_Array_unbox(ref) as! [Any]", "KotlinBridgeable_Array_unbox(ref)")
                    .replace(
                        "return __createProtocolWrapper(externalRCRef: ref) as! any _KotlinBridgeable",
                        "return __createProtocolWrapper(externalRCRef: ref)!",
                    )
                    .replace("} catch let error as CancellationError {", "} catch is CancellationError {")
                content = unreachableEnumDefaultRegex.replace(content, "$1")
                content = voidResultClosureRegex.replace(content, "$1\n$2return true")
                swiftFile.writeText(content, Charsets.UTF_8)
            }
    }
}

tasks.register<AssembleDeponderAppleSwiftPackageTask>("assembleDeponderAppleSwiftPackage") {
    group = "build"
    description = "Assembles DeponderCoreBridge.xcframework and syncs Swift Export SPM sources into apple/Generated."
    onlyIf { System.getProperty("os.name").contains("mac", ignoreCase = true) }

    sourceInputs.from(
        layout.projectDirectory.dir("src/commonMain"),
        layout.projectDirectory.file("build.gradle.kts"),
        rootProject.layout.projectDirectory.file("gradle/libs.versions.toml"),
    )
    rootProjectDir.set(rootProject.layout.projectDirectory)
    projectBuildDir.set(layout.buildDirectory)
    appleGeneratedDir.set(rootProject.layout.projectDirectory.dir("apple/Generated"))
    packageSwiftFile.set(rootProject.layout.projectDirectory.file("Package.swift"))
    projectVersion.set(providers.provider { project.version.toString() })
    updatePackageSwiftChecksum.set(
        providers.gradleProperty("updatePackageSwiftChecksum")
            .map { it.equals("true", ignoreCase = true) }
            .orElse(false),
    )
    javaHomePath.set(
        providers.environmentVariable("JAVA_HOME")
            .orElse(providers.systemProperty("java.home")),
    )
    developerDirPath.set(
        providers.environmentVariable("DEVELOPER_DIR"),
    )

    val xcframeworkReleaseDir = layout.buildDirectory.dir("XCFrameworks/release")
    xcframeworkOutputDir.set(xcframeworkReleaseDir.map { it.dir("DeponderCoreBridge.xcframework") })
    xcframeworkZipOutputFile.set(xcframeworkReleaseDir.map { it.file("DeponderCoreBridge.xcframework.zip") })
    checksumOutputFile.set(xcframeworkReleaseDir.map { it.file("DeponderCoreBridge.xcframework.zip.sha256") })
}

mavenPublishing {
    coordinates(
        groupId = project.group.toString(),
        artifactId = "deponder-core",
        version = project.version.toString(),
    )
    pom {
        name.set("Deponder Core")
        description.set("Pure Kotlin Multiplatform force-directed graph physics kernel and hybrid quadtree engine.")
    }
}
