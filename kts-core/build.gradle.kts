import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
            // NOTE: Kotlin interfaces with body-bearing methods (e.g. CoordinateSequence.getMeasures)
            // must emit REAL JVM `default` methods so Java implementors inherit them. This used to
            // require `-Xjvm-default=all`, but on Kotlin 2.2+ the default `-jvm-default=enable` mode
            // already generates real default methods, so no explicit flag is needed. Do NOT set
            // `-jvm-default=disable` (that reverts to the DefaultImpls behavior and breaks Java
            // implementors).
        }
    }

    // Non-JVM targets. The non-JVM `actual`s (OrdinateFormatImpl, …) are pure Kotlin and shared by
    // all of them via the `nonJvmMain` source set (see below). core has no external dependencies,
    // so it can target the full feasible set.
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

    // Materialize the default hierarchy's intermediate source sets (nativeMain/jsMain/wasmJsMain)
    // so they can be referenced by name below (and re-parented under nonJvmMain).
    applyDefaultHierarchyTemplate()

    sourceSets {
        // Canonical KMP layout: commonMain = src/commonMain/kotlin, the JVM-only actuals
        // (OrdinateFormatImpl, Memory) = src/jvmMain/kotlin. The non-JVM actuals are pure Kotlin,
        // shared across native + js + wasmJs via the intermediate `nonJvmMain` source set.
        val commonMain = getByName("commonMain")
        val nonJvmMain = create("nonJvmMain") {
            dependsOn(commonMain)
        }
        getByName("nativeMain") { dependsOn(nonJvmMain) }
        getByName("jsMain") { dependsOn(nonJvmMain) }
        getByName("wasmJsMain") { dependsOn(nonJvmMain) }
        // Native runtime validation: a kotlin.test suite in commonTest runs on EVERY target's test
        // task (jvmTest, macosArm64Test, jsNodeTest, wasmJsNodeTest), so the same assertions that
        // pass on the JVM (the reference) also prove parity on native/js/wasm. It targets the
        // pure-Kotlin reimplementations and expect/actual seams (OrdinateFormat, util.Random,
        // TreeMap/TreeSet/PriorityQueue, Double bit ops) plus representative geometry operations.
        getByName("commonTest") {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        getByName("jvmTest") {
            dependencies {
                implementation(libs.junit)
                // The shared test infrastructure (test.jts.GeometryTestCase / WKTorBReader) parses
                // WKT/WKB via the readers, which live in :kts-io-wkt. Core's own tests therefore
                // depend back on it (main → :kts-io-wkt → core is still a DAG).
                implementation(projects.ktsIoWkt)
                // Some tests (IOUtil, InteriorPointTest, CascadedPolygonUnionTester, perf) read
                // geometry files via WKTFileReader / WKBHexFileReader, which live in :kts-io-files.
                implementation(projects.ktsIoFiles)
            }
        }
    }
}

// Expose the compiled JVM test sources (test.jts.* shared infrastructure — GeometryTestCase,
// WKTorBReader, IOUtil, …) as a consumable artifact for the io modules' tests. This replaces
// the Maven `test-jar` that the io modules consumed via `<type>test-jar</type>`.
val jvmTestJar = tasks.register<Jar>("jvmTestJar") {
    archiveClassifier.set("tests")
    val testCompilation = kotlin.jvm().compilations.getByName("test")
    from(testCompilation.output.allOutputs)
}

val testArtifacts = configurations.consumable("testArtifacts").get()

artifacts {
    add(testArtifacts.name, jvmTestJar)
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
}

tasks.withType<Test>().configureEach {
    useJUnit()
    maxHeapSize = "1g"
    // Run only *Test classes, excluding perf/stress tests and the jts.perf package — this keeps the
    // Gradle test set identical to the reference (upstream Surefire) suite (guardrail parity).
    include("**/*Test.class")
    exclude(
        "**/*PerfTest.class",
        "**/*StressTest.class",
        "**/test/jts/perf/**",
    )
}

// Per-package documentation for Dokka. `packages.md` holds one `# Module` + one `# Package <fqn>`
// section per core package; Dokka renders them as the package/module overview text.
dokka {
    moduleName.set("kts-core")
    dokkaSourceSets.configureEach {
        includes.from("packages.md")
    }
    // The precision package doc (packages.md) embeds images/minClearance.png. Dokka does not
    // copy images referenced from `includes` markdown, so register it as a custom asset — Dokka
    // copies custom assets into the generated site's `images/` directory, which the doc references
    // via the page's pathToRoot prefix (`../../images/minClearance.png`).
    pluginsConfiguration.html {
        customAssets.from("images/minClearance.png")
    }
}
