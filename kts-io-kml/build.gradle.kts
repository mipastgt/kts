import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// kts-io-kml: KML reader/writer, Kotlin Multiplatform. The reader is rewritten on xmlutil (pull
// parser) in commonMain, replacing the JDK StAX (javax.xml.stream); the writer is pure string
// building.
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

    sourceSets {
        getByName("commonMain") {
            dependencies {
                api(projects.ktsCore)
                implementation(libs.xmlutil.core)
            }
        }
        getByName("jvmTest") {
            dependencies {
                implementation(libs.junit)
                implementation(project(path = ":kts-core", configuration = "testArtifacts"))
                // KMLReaderTest and the shared GeometryTestCase harness parse WKT via WKTReader.
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
