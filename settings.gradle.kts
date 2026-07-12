pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

// Type-safe project accessors — reference sibling projects as `projects.ktsCore` etc.
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "kts"

// Each module directory name matches its Gradle project name (= the published Maven artifactId),
// so no `projectDir` remapping is needed. The `kts-` prefix keeps these artifacts distinct from the
// upstream `org.locationtech.jts` Java artifacts on Maven Central; the SOURCE packages are unchanged
// (`org.locationtech.jts.*`).
include(
    ":kts-core",
    ":kts-io-wkt",
    ":kts-io-gml",
    ":kts-io-kml",
    ":kts-io-files",
)
