import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// kts-io-gml: GML 2 reader/writer, Kotlin Multiplatform. The reader is rewritten on xmlutil (pull
// parser, relaxed mode to accept the undeclared GML namespace prefixes) in commonMain, replacing
// the JDK SAX (javax.xml.parsers / org.xml.sax); the writer is pure string building.
//
// NOTE: this BREAKS the historical SAX API — GMLHandler is no longer an org.xml.sax DefaultHandler
// and GMLReader.read no longer throws SAXException/ParserConfigurationException (see the compat doc).
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
                // GMLReaderTest and the shared GeometryTestCase harness parse WKT via WKTReader.
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

// Wire the package-level documentation (migrated from package-info.java) into Dokka.
// `packages.md` holds one `# Module` + one `# Package <fqn>` section.
dokka {
    moduleName.set("kts-io-gml")
    dokkaSourceSets.configureEach {
        includes.from("packages.md")
    }
}
