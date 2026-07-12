import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// jts-io-files (deferred Stage 2B-2c follow-up): à-la-carte multiplatform file readers for WKT and
// WKB-hex geometry files. The common file access is backed by kotlinx-io (SystemFileSystem); the
// legacy java.io constructors (File / Reader, incl. classpath-resource streams) are preserved in
// the jvm target. Depends on :kts-core and :kts-io-wkt (the WKT/WKB parsers + the WktCharStream
// streaming seam).
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    `maven-publish`
    alias(libs.plugins.dokka)
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
                api(projects.ktsIoWkt)
                implementation(libs.kotlinx.io.core)
            }
        }
        // The non-JVM file-reader actuals are pure Kotlin (file access via kotlinx-io), shared
        // across native + js + wasmJs. NOTE: on browser JS / Wasm-JS there is no filesystem
        // (kotlinx-io SystemFileSystem is backed by Node's fs / WASI only), so the by-name readers
        // compile everywhere but only function where a filesystem exists (JVM, native, Node, WASI).
        val nonJvmMain = create("nonJvmMain") {
            dependsOn(commonMain)
        }
        getByName("nativeMain") { dependsOn(nonJvmMain) }
        getByName("jsMain") { dependsOn(nonJvmMain) }
        getByName("wasmJsMain") { dependsOn(nonJvmMain) }
        getByName("jvmTest") {
            dependencies {
                implementation(libs.junit)
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
