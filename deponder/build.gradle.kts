import java.io.File
import kotlin.metadata.Visibility
import kotlin.metadata.visibility
import kotlin.metadata.jvm.KotlinClassMetadata
import kotlin.metadata.jvm.Metadata as createKotlinMetadata
import kotlin.metadata.jvm.fieldSignature
import kotlin.metadata.jvm.getterSignature
import kotlin.metadata.jvm.setterSignature
import kotlin.metadata.jvm.signature
import kotlin.metadata.jvm.syntheticMethodForAnnotations
import kotlin.metadata.jvm.syntheticMethodForDelegate
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.SetProperty
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.dokka)
    alias(libs.plugins.vanniktech.maven.publish)
}

val testJava21Launcher = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(21))
}

android {
    namespace = "com.cinpe.deponder"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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
                it.javaLauncher.set(testJava21Launcher)
            }
        }
    }
}

kotlin {
    explicitApi()
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        jvmDefault.set(JvmDefaultMode.NO_COMPATIBILITY)
    }
}

/**
 * Public binary ABI dump task powered by `kotlin.metadata.jvm` and ASM for Android library release artifacts.
 */
@CacheableTask
abstract class DumpJvmPublicApiTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputClassesDirs: ConfigurableFileCollection

    @get:Input
    abstract val ignoredClasses: SetProperty<String>

    @get:OutputFile
    abstract val outputApiFile: RegularFileProperty

    private data class FieldInfo(
        val access: Int,
        val name: String,
        val desc: String,
    )

    private data class MethodInfo(
        val access: Int,
        val name: String,
        val desc: String,
    )

    private class ParsedClass(
        val internalName: String,
        val access: Int,
        val superName: String?,
        val interfaces: List<String>,
        val innerClassAccess: Map<String, Int>,
        val fields: List<FieldInfo>,
        val methods: List<MethodInfo>,
        val metadata: kotlin.Metadata?,
    )

    @TaskAction
    fun generate() {
        val ignoredInternalNames = ignoredClasses.get().mapTo(HashSet()) { it.replace('.', '/') }
        val classFiles = ArrayList<File>()
        for (dir in inputClassesDirs.files) {
            if (!dir.exists()) continue
            dir.walkTopDown()
                .filter { it.isFile && it.extension == "class" && it.name != "module-info.class" }
                .forEach { classFiles.add(it) }
        }

        val parsedByName = LinkedHashMap<String, ParsedClass>()
        for (file in classFiles.sortedBy { it.invariantSeparatorsPath }) {
            val parsed = parseClassFile(file.readBytes())
            if (parsed.internalName in ignoredInternalNames) continue
            parsedByName[parsed.internalName] = parsed
        }

        val visibilityCache = HashMap<String, Boolean>()
        fun isEffectivelyPublicClass(name: String): Boolean {
            visibilityCache[name]?.let { return it }
            val cls = parsedByName[name]
            if (cls == null) {
                visibilityCache[name] = false
                return false
            }
            val result = computeClassPublicity(cls, parsedByName, ::isEffectivelyPublicClass)
            visibilityCache[name] = result
            return result
        }

        val renderedClasses = ArrayList<String>()
        for (name in parsedByName.keys.sorted()) {
            if (!isEffectivelyPublicClass(name)) continue
            val rendered = renderPublicClass(parsedByName.getValue(name))
            if (rendered != null) {
                renderedClasses.add(rendered)
            }
        }

        val outFile = outputApiFile.get().asFile
        outFile.parentFile.mkdirs()
        val body = if (renderedClasses.isEmpty()) {
            ""
        } else {
            renderedClasses.joinToString(separator = "\n\n", postfix = "\n\n")
        }
        outFile.writeText(body, Charsets.UTF_8)
    }

    private fun parseClassFile(bytes: ByteArray): ParsedClass {
        var internalName = ""
        var classAccess = 0
        var superName: String? = null
        val interfaces = ArrayList<String>()
        val innerAccess = HashMap<String, Int>()
        val fields = ArrayList<FieldInfo>()
        val methods = ArrayList<MethodInfo>()

        var hasKotlinMetadata = false
        var mdKind: Int? = null
        var mdVersion: IntArray? = null
        val mdData1 = ArrayList<String>()
        val mdData2 = ArrayList<String>()
        var mdExtraString: String? = null
        var mdPackageName: String? = null
        var mdExtraInt: Int? = null

        val reader = ClassReader(bytes)
        reader.accept(
            object : ClassVisitor(Opcodes.ASM9) {
                override fun visit(
                    version: Int,
                    access: Int,
                    name: String,
                    signature: String?,
                    superClassName: String?,
                    ifaces: Array<out String>?,
                ) {
                    internalName = name
                    classAccess = access
                    superName = superClassName
                    if (ifaces != null) {
                        interfaces.addAll(ifaces)
                    }
                }

                override fun visitInnerClass(
                    name: String,
                    outerName: String?,
                    innerName: String?,
                    access: Int,
                ) {
                    innerAccess[name] = access
                }

                override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
                    if (descriptor != "Lkotlin/Metadata;") return null
                    hasKotlinMetadata = true
                    return object : AnnotationVisitor(Opcodes.ASM9) {
                        override fun visit(name: String?, value: Any?) {
                            when (name) {
                                "k" -> mdKind = value as? Int
                                "mv" -> mdVersion = value as? IntArray
                                "xs" -> mdExtraString = value as? String
                                "pn" -> mdPackageName = value as? String
                                "xi" -> mdExtraInt = value as? Int
                            }
                        }

                        override fun visitArray(name: String?): AnnotationVisitor? {
                            val target = when (name) {
                                "d1" -> mdData1
                                "d2" -> mdData2
                                else -> return null
                            }
                            return object : AnnotationVisitor(Opcodes.ASM9) {
                                override fun visit(itemName: String?, value: Any?) {
                                    if (value is String) target.add(value)
                                }
                            }
                        }
                    }
                }

                override fun visitField(
                    access: Int,
                    name: String,
                    descriptor: String,
                    signature: String?,
                    value: Any?,
                ): FieldVisitor? {
                    fields.add(FieldInfo(access, name, descriptor))
                    return null
                }

                override fun visitMethod(
                    access: Int,
                    name: String,
                    descriptor: String,
                    signature: String?,
                    exceptions: Array<out String>?,
                ): MethodVisitor? {
                    methods.add(MethodInfo(access, name, descriptor))
                    return null
                }
            },
            ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES,
        )

        val metadata = if (hasKotlinMetadata) {
            createKotlinMetadata(
                kind = mdKind,
                metadataVersion = mdVersion,
                data1 = mdData1.toTypedArray(),
                data2 = mdData2.toTypedArray(),
                extraString = mdExtraString,
                packageName = mdPackageName,
                extraInt = mdExtraInt,
            )
        } else {
            null
        }

        return ParsedClass(
            internalName = internalName,
            access = classAccess,
            superName = superName,
            interfaces = interfaces,
            innerClassAccess = innerAccess,
            fields = fields,
            methods = methods,
            metadata = metadata,
        )
    }

    private fun isKotlinPublic(v: Visibility): Boolean =
        v == Visibility.PUBLIC || v == Visibility.PROTECTED

    private fun computeClassPublicity(
        cls: ParsedClass,
        allClasses: Map<String, ParsedClass>,
        checkParent: (String) -> Boolean,
    ): Boolean {
        if ((cls.access and Opcodes.ACC_SYNTHETIC) != 0) return false
        val ownInnerFlags = cls.innerClassAccess[cls.internalName]
        val effectiveAccess = ownInnerFlags ?: cls.access
        if ((effectiveAccess and (Opcodes.ACC_PUBLIC or Opcodes.ACC_PROTECTED)) == 0) return false
        if ((effectiveAccess and (Opcodes.ACC_PRIVATE or Opcodes.ACC_SYNTHETIC)) != 0) return false

        val dollarIdx = cls.internalName.lastIndexOf('$')
        if (dollarIdx > 0) {
            val outerName = cls.internalName.substring(0, dollarIdx)
            if (outerName in allClasses && !checkParent(outerName)) return false
        }

        val md = cls.metadata ?: return true
        return when (val kcm = KotlinClassMetadata.readLenient(md)) {
            is KotlinClassMetadata.Class -> isKotlinPublic(kcm.kmClass.visibility)
            is KotlinClassMetadata.FileFacade -> {
                val pkg = kcm.kmPackage
                pkg.functions.any { isKotlinPublic(it.visibility) } ||
                    pkg.properties.any { isKotlinPublic(it.visibility) }
            }
            is KotlinClassMetadata.MultiFileClassFacade -> true
            is KotlinClassMetadata.SyntheticClass,
            is KotlinClassMetadata.MultiFileClassPart,
            is KotlinClassMetadata.Unknown,
            -> false
        }
    }

    private fun renderPublicClass(cls: ParsedClass): String? {
        val nonPublicMethods = HashSet<String>()
        val nonPublicFields = HashSet<String>()
        val publicKotlinCtorDescs = HashSet<String>()
        val publicKotlinFunNames = HashSet<String>()
        var hasKotlinMetadata = false

        cls.metadata?.let { md ->
            when (val kcm = KotlinClassMetadata.readLenient(md)) {
                is KotlinClassMetadata.Class -> {
                    hasKotlinMetadata = true
                    val km = kcm.kmClass
                    for (ctor in km.constructors) {
                        val sig = ctor.signature
                        if (isKotlinPublic(ctor.visibility)) {
                            if (sig != null) publicKotlinCtorDescs.add(sig.descriptor)
                        } else if (sig != null) {
                            nonPublicMethods.add("${sig.name}${sig.descriptor}")
                        }
                    }
                    for (fn in km.functions) {
                        val sig = fn.signature
                        if (isKotlinPublic(fn.visibility)) {
                            if (sig != null) publicKotlinFunNames.add(sig.name)
                        } else if (sig != null) {
                            nonPublicMethods.add("${sig.name}${sig.descriptor}")
                        }
                    }
                    for (prop in km.properties) {
                        if (!isKotlinPublic(prop.visibility)) {
                            prop.getterSignature?.let { nonPublicMethods.add("${it.name}${it.descriptor}") }
                            prop.setterSignature?.let { nonPublicMethods.add("${it.name}${it.descriptor}") }
                            prop.fieldSignature?.let { nonPublicFields.add("${it.name}:${it.descriptor}") }
                            prop.syntheticMethodForAnnotations?.let { nonPublicMethods.add("${it.name}${it.descriptor}") }
                            prop.syntheticMethodForDelegate?.let { nonPublicMethods.add("${it.name}${it.descriptor}") }
                        } else {
                            val setterAttr = prop.setter
                            if (setterAttr != null && !isKotlinPublic(setterAttr.visibility)) {
                                prop.setterSignature?.let { nonPublicMethods.add("${it.name}${it.descriptor}") }
                            }
                            prop.fieldSignature?.let { fieldSig ->
                                // Backing fields without @JvmField / const stay private in bytecode, while Companion/const fields stay public.
                                if (fieldSig.name.endsWith("\$delegate")) {
                                    nonPublicFields.add("${fieldSig.name}:${fieldSig.descriptor}")
                                }
                            }
                        }
                    }
                }

                is KotlinClassMetadata.FileFacade -> {
                    hasKotlinMetadata = true
                    val pkg = kcm.kmPackage
                    for (fn in pkg.functions) {
                        val sig = fn.signature
                        if (isKotlinPublic(fn.visibility)) {
                            if (sig != null) publicKotlinFunNames.add(sig.name)
                        } else if (sig != null) {
                            nonPublicMethods.add("${sig.name}${sig.descriptor}")
                        }
                    }
                    for (prop in pkg.properties) {
                        if (!isKotlinPublic(prop.visibility)) {
                            prop.getterSignature?.let { nonPublicMethods.add("${it.name}${it.descriptor}") }
                            prop.setterSignature?.let { nonPublicMethods.add("${it.name}${it.descriptor}") }
                            prop.fieldSignature?.let { nonPublicFields.add("${it.name}:${it.descriptor}") }
                            prop.syntheticMethodForAnnotations?.let { nonPublicMethods.add("${it.name}${it.descriptor}") }
                            prop.syntheticMethodForDelegate?.let { nonPublicMethods.add("${it.name}${it.descriptor}") }
                        } else {
                            val setterAttr = prop.setter
                            if (setterAttr != null && !isKotlinPublic(setterAttr.visibility)) {
                                prop.setterSignature?.let { nonPublicMethods.add("${it.name}${it.descriptor}") }
                            }
                        }
                    }
                }

                else -> Unit
            }
        }

        val visibleFields = cls.fields
            .filter { f ->
                (f.access and (Opcodes.ACC_PUBLIC or Opcodes.ACC_PROTECTED)) != 0 &&
                    (f.access and Opcodes.ACC_SYNTHETIC) == 0 &&
                    "${f.name}:${f.desc}" !in nonPublicFields
            }
            .sortedWith(compareBy<FieldInfo> { it.name }.thenBy { it.desc })

        val visibleMethods = cls.methods
            .filter { m ->
                if (m.name == "<clinit>") return@filter false
                if ((m.access and (Opcodes.ACC_PUBLIC or Opcodes.ACC_PROTECTED)) == 0) return@filter false
                if ((m.access and Opcodes.ACC_BRIDGE) != 0) return@filter false
                if ("${m.name}${m.desc}" in nonPublicMethods) return@filter false

                val isSynthetic = (m.access and Opcodes.ACC_SYNTHETIC) != 0
                if (!isSynthetic) {
                    return@filter true
                }

                // Keep only public Kotlin default-argument bridges ($default and <init>(...ILDefaultConstructorMarker;)V).
                if (m.name.endsWith("\$default")) {
                    val baseName = m.name.removeSuffix("\$default")
                    return@filter !hasKotlinMetadata || baseName in publicKotlinFunNames
                }
                if (m.name == "<init>" && m.desc.contains("ILkotlin/jvm/internal/DefaultConstructorMarker;)V")) {
                    return@filter !hasKotlinMetadata || publicKotlinCtorDescs.isNotEmpty()
                }
                false
            }
            .sortedWith(compareBy<MethodInfo> { it.name }.thenBy { it.desc })

        val effectiveClassAccess = cls.innerClassAccess[cls.internalName] ?: cls.access
        val header = buildString {
            append(if ((effectiveClassAccess and Opcodes.ACC_PUBLIC) != 0) "public" else "protected")
            val isInterface = (cls.access and Opcodes.ACC_INTERFACE) != 0
            val isAnnotation = (cls.access and Opcodes.ACC_ANNOTATION) != 0
            if (isInterface) {
                if (isAnnotation) {
                    append(" abstract annotation class ")
                } else {
                    append(" abstract interface ")
                }
            } else {
                if ((effectiveClassAccess and Opcodes.ACC_FINAL) != 0) append(" final")
                if ((effectiveClassAccess and Opcodes.ACC_ABSTRACT) != 0) append(" abstract")
                append(" class ")
            }
            append(cls.internalName)

            val supertypes = ArrayList<String>()
            if (!isInterface && cls.superName != null && cls.superName != "java/lang/Object") {
                supertypes.add(cls.superName)
            }
            val sortedInterfaces = if (isAnnotation) {
                cls.interfaces.filter { it != "java/lang/annotation/Annotation" }.sorted()
            } else {
                cls.interfaces.sorted()
            }
            supertypes.addAll(sortedInterfaces)
            if (supertypes.isNotEmpty()) {
                append(" : ")
                append(supertypes.joinToString(", "))
            }
            append(" {")
        }

        return buildString {
            appendLine(header)
            for (f in visibleFields) {
                append('\t')
                append(if ((f.access and Opcodes.ACC_PUBLIC) != 0) "public" else "protected")
                if ((f.access and Opcodes.ACC_STATIC) != 0) append(" static")
                if ((f.access and Opcodes.ACC_FINAL) != 0) append(" final")
                append(" field ")
                append(f.name)
                append(' ')
                appendLine(f.desc)
            }
            for (m in visibleMethods) {
                append('\t')
                append(if ((m.access and Opcodes.ACC_PUBLIC) != 0) "public" else "protected")
                if ((m.access and Opcodes.ACC_STATIC) != 0) append(" static")
                if ((m.access and Opcodes.ACC_FINAL) != 0 && (cls.access and Opcodes.ACC_INTERFACE) == 0) {
                    append(" final")
                }
                if ((m.access and Opcodes.ACC_ABSTRACT) != 0 && (cls.access and Opcodes.ACC_INTERFACE) == 0) {
                    append(" abstract")
                }
                if ((m.access and Opcodes.ACC_SYNTHETIC) != 0) append(" synthetic")
                append(" fun ")
                append(m.name)
                append(' ')
                appendLine(m.desc)
            }
            append('}')
        }
    }
}

@CacheableTask
abstract class CompareJvmPublicApiTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val projectApiFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val generatedApiFile: RegularFileProperty

    @TaskAction
    fun verify() {
        val expectedFile = projectApiFile.get().asFile
        val actualFile = generatedApiFile.get().asFile
        if (!expectedFile.exists()) {
            throw GradleException(
                "Missing committed ABI dump at ${expectedFile.absolutePath}. Run ':deponder:apiDump' to generate it.",
            )
        }
        val expected = expectedFile.readText(Charsets.UTF_8).replace("\r\n", "\n").trimEnd()
        val actual = actualFile.readText(Charsets.UTF_8).replace("\r\n", "\n").trimEnd()
        if (expected != actual) {
            throw GradleException(
                "Public ABI check failed for project ':deponder'.\n" +
                    "Committed file: ${expectedFile.absolutePath}\n" +
                    "Generated file: ${actualFile.absolutePath}\n" +
                    "Run './gradlew :deponder:apiDump' to update if this binary API change is intentional.",
            )
        }
    }
}

val generatedReleaseApiFile = layout.buildDirectory.file("api/${project.name}.api")
val committedReleaseApiFile = layout.projectDirectory.file("api/${project.name}.api")

val apiBuild = tasks.register<DumpJvmPublicApiTask>("apiBuild") {
    group = "other"
    description = "Builds the public binary ABI dump for the release variant."
    inputClassesDirs.from(
        tasks.named<KotlinCompile>("compileReleaseKotlin").flatMap { it.destinationDirectory },
        tasks.named<JavaCompile>("compileReleaseJavaWithJavac").flatMap { it.destinationDirectory },
    )
    outputApiFile.set(generatedReleaseApiFile)
    ignoredClasses.addAll(
        listOf(
            "com.cinpe.deponder.BuildConfig",
            "com.cinpe.deponder.R",
            "com.cinpe.deponder.R\$attr",
            "com.cinpe.deponder.R\$id",
            "com.cinpe.deponder.R\$string",
            "com.cinpe.deponder.R\$styleable",
        ),
    )
}

tasks.register<Sync>("apiDump") {
    group = "other"
    description = "Writes the public binary ABI dump to api/${project.name}.api."
    from(apiBuild.flatMap { it.outputApiFile })
    into(layout.projectDirectory.dir("api"))
}

val apiCheck = tasks.register<CompareJvmPublicApiTask>("apiCheck") {
    group = "verification"
    description = "Checks the compiled release ABI against the committed api/${project.name}.api dump."
    projectApiFile.set(committedReleaseApiFile)
    generatedApiFile.set(apiBuild.flatMap { it.outputApiFile })
}

tasks.named("check") {
    dependsOn(apiCheck)
}

dependencies {
    api(project(":deponder-core"))
    api(libs.androidx.lifecycle.common)

    compileOnly(libs.androidx.annotation)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    testCompileOnly(libs.androidx.annotation)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
}

mavenPublishing {
    coordinates(
        groupId = project.group.toString(),
        artifactId = "deponder",
        version = project.version.toString(),
    )
    pom {
        name.set("Deponder")
        description.set("Pure non-intrusive Android View physics operators and DeponderRubberView.")
    }
}
