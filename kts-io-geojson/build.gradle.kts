import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// kts-io-geojson: GeoJSON reader/writer, Kotlin Multiplatform. The reader/writer are rewritten on
// kotlinx-serialization-json (JsonElement tree API) in commonMain, replacing the JVM-only
// `org.json.simple` dependency of upstream JTS. The parse path (Json.parseToJsonElement) and the
// write path (JsonObject tree + Json.encodeToString) are pure Kotlin and native-capable; only the
// java.io Reader/Writer adapters stay in `jvmMain`.
//
// NOTE on writer output: GeoJsonWriter.formatOrdinate relies on Double.toString to render
// ordinates, which on the JVM matches upstream JTS byte-for-byte (the jvmTest parity tests cover
// this). Kotlin's Double.toString on JS/Wasm/native can differ in edge cases; the writer compiles
// and runs on every target, but exact-string parity is only guaranteed on the JVM.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
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
        getByName("commonMain") {
            dependencies {
                api(projects.ktsCore)
                implementation(libs.kotlinx.serialization.json)
            }
        }
        getByName("commonTest") {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        getByName("jvmTest") {
            dependencies {
                implementation(libs.junit)
                implementation(project(path = ":kts-core", configuration = "testArtifacts"))
                // GeoJson tests round-trip through WKT via the shared GeometryTestCase harness.
                implementation(projects.ktsIoWkt)
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

// Wire the package-level documentation into Dokka.
dokka {
    moduleName.set("kts-io-geojson")
    dokkaSourceSets.configureEach {
        includes.from("packages.md")
    }
}
