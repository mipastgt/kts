import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// kts-io-wkt: WKT / WKB readers + writer + byte-stream plumbing. The String/ByteArray parse path
// is `commonMain` (dep-free, native-capable); only the java.io stream/file adapters stay in
// `jvmMain/java`. Depends on :kts-core only.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.dokka)
    alias(libs.plugins.vanniktech.publish)
}

// group and version are set for all modules in the root build.gradle.kts (allprojects).

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }
    js {
        nodejs()
        browser()
    }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
        browser()
    }
    macosArm64()
    iosArm64()
    iosSimulatorArm64()
    iosX64()
    linuxX64()
    linuxArm64()
    mingwX64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain = getByName("commonMain") {
            dependencies {
                implementation(projects.ktsCore)
            }
        }
        // The non-JVM IOException actual is pure Kotlin, shared across native + js + wasmJs.
        val nonJvmMain = create("nonJvmMain") {
            dependsOn(commonMain)
        }
        getByName("nativeMain") { dependsOn(nonJvmMain) }
        getByName("jsMain") { dependsOn(nonJvmMain) }
        getByName("wasmJsMain") { dependsOn(nonJvmMain) }
        // Native runtime validation: WKT/WKB read→write roundtrips in commonTest exercise the
        // hand-rolled tokenizer + parser + OrdinateFormat on every target (jvm/native/js/wasm).
        getByName("commonTest") {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        getByName("jvmTest") {
            dependencies {
                implementation(libs.junit)
                // core's shared JVM test infrastructure (test.jts.*), as the Maven test-jar was.
                // Type-safe accessors can't select a configuration, so this keeps the path form.
                implementation(project(path = ":kts-core", configuration = "testArtifacts"))
            }
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
}

tasks.withType<Test>().configureEach {
    useJUnit()
    maxHeapSize = "1g"
    include("**/*Test.class")
    exclude(
        "**/*PerfTest.class",
        "**/*StressTest.class",
        "**/test/jts/perf/**",
    )
}
