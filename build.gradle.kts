import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.jetbrains.dokka.gradle.DokkaExtension

// Root build for the JTS Kotlin Multiplatform Gradle build.
// Plugin versions come from the version catalog (gradle/libs.versions.toml); subprojects apply
// the plugins without repeating a version.
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    // Declared `apply false` so the plugin's classes are on the root build-script classpath — the
    // `subprojects {}` block below configures its `MavenPublishBaseExtension`, but the root project
    // itself publishes nothing.
    alias(libs.plugins.vanniktech.publish) apply false
    // Applied (not `apply false`) at the root so the root project can aggregate the subprojects'
    // Dokka output into a single multi-module site. See the `dokka`/`dependencies` block below.
    alias(libs.plugins.dokka)
}

// Maven coordinates for all modules. The group is `de.mpmediasoft.kts` so these Kotlin artifacts
// don't collide with the upstream `org.locationtech.jts` Java artifacts on Maven Central. The
// source packages remain `org.locationtech.jts.*` — this changes only the published coordinates.
//
// Version scheme = <upstream JTS version>.<port revision>, e.g. 1.20.0.0, 1.20.0.1, …
//   - jtsBaseVersion (1.20.0) is the upstream JTS release this Kotlin port was derived from.
//     Only change it when re-basing the port onto a newer JTS release.
//   - portRevision is the revision of THIS language port against that JTS base. Bump it (0 -> 1 ->
//     2 …) for any correction or change to the Kotlin port itself. Both Maven and Gradle order the
//     4-component versions correctly (e.g. 1.20.0.1 > 1.20.0.0 == 1.20.0).
val jtsBaseVersion = "1.20.0"
val portRevision = 0

allprojects {
    group = "de.mpmediasoft.kts"
    version = "$jtsBaseVersion.$portRevision"
}

// ---------------------------------------------------------------------------------------------
// Publication metadata (POM) + a Dokka-HTML `-javadoc.jar` + GPG signing + Central Portal upload.
//
// Publishing is handled by the `com.vanniktech.maven.publish` plugin (applied per module). It wires
// the Central Portal endpoint, signs all publications, and — via `configure(KotlinMultiplatform(…))`
// — attaches a sources jar and a Dokka-HTML javadoc jar to every KMP publication (Central requires
// both). Coordinates default to `group:<module dir>:version`, which is exactly the scheme we want,
// so no explicit `coordinates(...)` call is needed.
//
// POM name/description/url/licenses/developers/scm are configured centrally for all modules here;
// the only per-module values are the human-readable title and description maps below.
//
// Credentials live OUTSIDE the repo (~/.gradle/gradle.properties or env), never here:
//   mavenCentralUsername / mavenCentralPassword         — Central Portal user token
//   signingInMemoryKey / signingInMemoryKeyPassword     — ASCII-armored GPG secret key + passphrase
// `publishToMavenLocal` needs none of these — vanniktech does not sign local installs.
// ---------------------------------------------------------------------------------------------
val projectUrl = "https://github.com/mipastgt/kts"

val moduleTitles = mapOf(
    "kts-core" to "JTS Kotlin Multiplatform — Core",
    "kts-io-wkt" to "JTS Kotlin Multiplatform — WKT/WKB IO",
    "kts-io-gml" to "JTS Kotlin Multiplatform — GML IO",
    "kts-io-kml" to "JTS Kotlin Multiplatform — KML IO",
    "kts-io-geojson" to "JTS Kotlin Multiplatform — GeoJSON IO",
    "kts-io-files" to "JTS Kotlin Multiplatform — File IO",
)
val moduleDescriptions = mapOf(
    "kts-core" to "Kotlin Multiplatform port of the JTS Topology Suite 1.20.0 core: geometry model, spatial predicates, overlay, buffer and related algorithms.",
    "kts-io-wkt" to "WKT and WKB readers and writers for the JTS Kotlin Multiplatform port.",
    "kts-io-gml" to "GML 2 reader and writer for the JTS Kotlin Multiplatform port.",
    "kts-io-kml" to "KML reader and writer for the JTS Kotlin Multiplatform port.",
    "kts-io-geojson" to "GeoJSON reader and writer (kotlinx-serialization) for the JTS Kotlin Multiplatform port.",
    "kts-io-files" to "File-based WKT/WKB geometry readers (kotlinx-io) for the JTS Kotlin Multiplatform port.",
)

// ---------------------------------------------------------------------------------------------
// Aggregated multi-module Dokka site.
//
// The root applies the Dokka plugin and depends on each documented module through the `dokka`
// configuration. Running `./gradlew dokkaGenerate` (or `dokkaGenerateHtml`) at the root produces a
// single combined HTML site — one navigation tree and one search across all modules — under
// `build/dokka/html`. The per-module sites and the `-javadoc.jar` publications are unaffected.
// ---------------------------------------------------------------------------------------------
// The root now runs a Dokka task, so it needs a repository to resolve Dokka's own plugin
// artifacts (dokka-base, templating-plugin). Subprojects declare their own repositories.
repositories {
    mavenCentral()
}

dokka {
    moduleName.set("JTS Kotlin Multiplatform")
    // Treat any Dokka warning (e.g. an unresolvable KDoc link) as a build failure for the
    // aggregated site, so doc-reference regressions can't slip in unnoticed.
    dokkaPublications.configureEach {
        failOnWarning.set(true)
    }
}

dependencies {
    dokka(project(":kts-core"))
    dokka(project(":kts-io-wkt"))
    dokka(project(":kts-io-gml"))
    dokka(project(":kts-io-kml"))
    dokka(project(":kts-io-geojson"))
    dokka(project(":kts-io-files"))
}

subprojects {
    // Fail each module's Dokka generation on any warning (unresolvable KDoc links, etc.), matching
    // the aggregated site's setting above. Warnings surface at the per-module stage too.
    pluginManager.withPlugin("org.jetbrains.dokka") {
        extensions.configure<DokkaExtension> {
            dokkaPublications.configureEach {
                failOnWarning.set(true)
            }
        }
    }

    pluginManager.withPlugin("com.vanniktech.maven.publish") {
        extensions.configure<MavenPublishBaseExtension> {
            // Upload to the Central Portal (central.sonatype.com).
            publishToMavenCentral()

            // Sign every artifact — but only when a GPG key is actually configured (via the
            // `signingInMemoryKey` Gradle property / `ORG_GRADLE_PROJECT_signingInMemoryKey` env
            // var, e.g. in ~/.gradle/gradle.properties or CI). Without this guard the `sign…`
            // tasks run for `publishToMavenLocal` too and fail with "no configured signatory",
            // breaking key-free local testing. The Central upload always has the key, so it signs.
            if (providers.gradleProperty("signingInMemoryKey").isPresent) {
                signAllPublications()
            }

            // KMP publication layout: a Dokka-HTML javadoc jar (built from each module's Dokka
            // HTML output) on every target + the root metadata publication. Sources jars are
            // published by default. Dokka is applied before this plugin (see each module's
            // `plugins {}` order) so the Dokka task exists when this javadoc jar is wired.
            //
            // NOTE: use `dokkaGeneratePublicationHtml`, NOT the `dokkaGenerate` lifecycle task.
            // Under Dokka Gradle Plugin v2, `dokkaGenerate` is an empty aggregator whose own
            // outputs are the HTML directory of NOTHING, so wiring the jar to it produces an empty
            // (manifest-only) javadoc jar. `dokkaGeneratePublicationHtml` is the task that actually
            // owns the generated `build/dokka/html` directory.
            configure(KotlinMultiplatform(javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml")))

            // POM metadata Central requires (name/description/url/licenses/developers/scm).
            pom {
                name.set(moduleTitles[project.name] ?: project.name)
                description.set(
                    moduleDescriptions[project.name]
                        ?: "Kotlin Multiplatform port of the JTS Topology Suite.",
                )
                url.set(projectUrl)
                licenses {
                    license {
                        name.set("Eclipse Public License 2.0")
                        url.set("https://www.eclipse.org/legal/epl-2.0/")
                        distribution.set("repo")
                    }
                    license {
                        name.set("Eclipse Distribution License 1.0")
                        url.set("https://www.eclipse.org/org/documents/edl-v10.php")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("mipastgt")
                        name.set("Michael Paus")
                        email.set("michael.paus@mpmediasoft.de")
                        organization.set("mpMediaSoft")
                        organizationUrl.set("https://www.mpmediasoft.de")
                    }
                }
                scm {
                    connection.set("scm:git:$projectUrl.git")
                    developerConnection.set("scm:git:ssh://git@github.com/mipastgt/kts.git")
                    url.set(projectUrl)
                }
            }
        }

        // Regression guard for the empty-javadoc-jar bug (see the JavadocJar.Dokka note above):
        // build the primary KMP `-javadoc.jar` (the one published as the module's main artifact and
        // read by javadoc.io) and assert it actually contains rendered Dokka HTML, not just a
        // manifest. Wired into `check`, so `./gradlew check` (and CI) fails if the jar ever reverts
        // to a stub. Cheap relative to the Dokka generation it depends on — it only reads the zip.
        val verifyJavadocJar = tasks.register("verifyJavadocJar") {
            group = "verification"
            description = "Fails if the published Dokka -javadoc.jar contains no HTML."
            val javadocJar = tasks.named(
                "kotlinMultiplatformDokkaJavadocJar",
                org.gradle.api.tasks.bundling.AbstractArchiveTask::class.java,
            )
            val jarFile = javadocJar.flatMap { it.archiveFile }
            inputs.file(jarFile)
            doLast {
                val file = jarFile.get().asFile
                val htmlCount = java.util.zip.ZipFile(file).use { zip ->
                    zip.entries().asSequence().count { it.name.endsWith(".html") }
                }
                require(htmlCount > 0) {
                    "javadoc jar ${file.name} contains no HTML — Dokka output was not packaged " +
                        "(the jar is a manifest-only stub). Check the JavadocJar.Dokka(\"…\") task " +
                        "wiring in the root build.gradle.kts."
                }
                logger.lifecycle("verifyJavadocJar: ${file.name} contains $htmlCount HTML file(s).")
            }
        }
        tasks.named("check") { dependsOn(verifyJavadocJar) }
    }
}
