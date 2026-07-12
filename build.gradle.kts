import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

// Root build for the JTS Kotlin Multiplatform Gradle build.
// Plugin versions come from the version catalog (gradle/libs.versions.toml); subprojects apply
// the plugins without repeating a version.
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.dokka) apply false
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
// Publication metadata (POM) + a Dokka-HTML `-javadoc.jar` for every publication.
//
// Maven Central requires each published artifact to carry POM name/description/url/licenses/
// developers/scm and a javadoc jar. These are configured centrally for all modules here; the
// only per-module values are the human-readable title and description maps below.
//
// NOT configured yet (intentionally, until the user finishes local testing): artifact SIGNING
// (the `signing` plugin + GPG key) and the Central Portal / Sonatype publishing endpoint. Local
// `publishToMavenLocal` needs neither.
// ---------------------------------------------------------------------------------------------
val projectUrl = "https://github.com/mipastgt/kts"

val moduleTitles = mapOf(
    "kts-core" to "JTS Kotlin Multiplatform — Core",
    "kts-io-wkt" to "JTS Kotlin Multiplatform — WKT/WKB IO",
    "kts-io-gml" to "JTS Kotlin Multiplatform — GML IO",
    "kts-io-kml" to "JTS Kotlin Multiplatform — KML IO",
    "kts-io-files" to "JTS Kotlin Multiplatform — File IO",
)
val moduleDescriptions = mapOf(
    "kts-core" to "Kotlin Multiplatform port of the JTS Topology Suite 1.20.0 core: geometry model, spatial predicates, overlay, buffer and related algorithms.",
    "kts-io-wkt" to "WKT and WKB readers and writers for the JTS Kotlin Multiplatform port.",
    "kts-io-gml" to "GML 2 reader and writer for the JTS Kotlin Multiplatform port.",
    "kts-io-kml" to "KML reader and writer for the JTS Kotlin Multiplatform port.",
    "kts-io-files" to "File-based WKT/WKB geometry readers (kotlinx-io) for the JTS Kotlin Multiplatform port.",
)

subprojects {
    // POM metadata for every Maven publication the module produces (root/metadata + per-target).
    pluginManager.withPlugin("maven-publish") {
        extensions.configure<PublishingExtension> {
            publications.withType<MavenPublication>().configureEach {
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
        }
    }

    // A Dokka-HTML `-javadoc.jar`, attached to every publication (Central requires a javadoc jar).
    // Every module applies the Dokka plugin, so its `dokkaGenerate` output (build/dokka/html) is
    // packaged with the conventional `javadoc` classifier.
    pluginManager.withPlugin("org.jetbrains.dokka") {
        val dokkaHtmlJar = tasks.register<Jar>("dokkaHtmlJar") {
            dependsOn("dokkaGenerate")
            from(layout.buildDirectory.dir("dokka/html"))
            archiveClassifier.set("javadoc")
        }
        pluginManager.withPlugin("maven-publish") {
            extensions.configure<PublishingExtension> {
                publications.withType<MavenPublication>().configureEach {
                    artifact(dokkaHtmlJar)
                }
            }
        }
    }
}
