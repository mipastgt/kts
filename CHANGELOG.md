# Changelog

All notable changes to this Kotlin Multiplatform port of the JTS Topology Suite are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

Versioning is `<jtsBaseVersion>.<portRevision>` — e.g. `1.20.0.0` is port revision `0` of upstream
JTS `1.20.0`. The `portRevision` (4th component) is bumped for changes to the Kotlin port itself; the
`jtsBaseVersion` changes only when the port is re-based onto a newer upstream JTS release.

## [Unreleased]

These are build, CI, and tooling changes only — the published library API is unchanged. The
javadoc-jar fix below takes effect with the next release (`1.20.0.0` artifacts on Maven Central are
immutable).

### Added
- **Continuous integration.** A GitHub Actions workflow (`.github/workflows/ci.yml`) that builds and
  runs the test suite across the full target matrix on **Linux, macOS, and Windows** runners, so the
  union covers JVM, JS, Wasm/JS, and every Kotlin/Native target. It is manual-dispatch only
  (`workflow_dispatch`).
- **`verifyJavadocJar` build check** (wired into `check`) that fails if a module's published
  `-javadoc.jar` contains no HTML, guarding against the empty-javadoc-jar regression fixed below.
- README badges: CI status, Kotlin version, Kotlin Multiplatform targets, Java 8+, and JTS base
  version.

### Fixed
- The published `-javadoc.jar` now bundles the full **Dokka HTML API documentation** instead of a
  manifest-only stub. The jar had been wired to Dokka Gradle Plugin v2's empty `dokkaGenerate`
  lifecycle task; it now uses `dokkaGeneratePublicationHtml`, which owns the generated HTML.

### Changed
- Build: raised the Gradle daemon heap and Metaspace (`org.gradle.jvmargs`) so a full parallel
  Kotlin/Native build of all six modules no longer exhausts Metaspace.

## [1.20.0.0] — 2026-07-17

First public release: a faithful Kotlin Multiplatform port of
[JTS 1.20.0](https://github.com/locationtech/jts), published to Maven Central under group
`de.mpmediasoft.kts`. Source package names are unchanged (`org.locationtech.jts.*`).

### Added
- Multiplatform artifacts for **JVM** (Java 8+), **JS** (Node/browser), **Wasm/JS**, and
  **Kotlin/Native** (macOS, iOS, Linux, Windows).
- Modular, à-la-carte packaging:
  - `kts-core` — geometry model, spatial predicates, overlay, buffer, algorithms, and the WKT/WKB writers.
  - `kts-io-wkt` — `WKTReader`, `WKBReader`, `WKBWriter`.
  - `kts-io-gml` — `GMLReader`, `GMLWriter`.
  - `kts-io-kml` — `KMLReader`, `KMLWriter`.
  - `kts-io-geojson` — `GeoJsonReader`, `GeoJsonWriter` (kotlinx-serialization).
  - `kts-io-files` — `WKTFileReader`, `WKBHexFileReader` (kotlinx-io).
- An extension-property layer restoring the Kotlin `.property` idiom on top of JTS's Java getters.

### Changed (deviations from upstream JTS)
- IO readers and the GML/KML/file IO are split out of `kts-core` into the à-la-carte `kts-io-*`
  modules; the WKT/WKB writers remain in `kts-core`.
- GeoJSON is built on `kotlinx-serialization-json` instead of the JVM-only `org.json.simple`.
- Stream-based (`java.io`) IO is provided as JVM-only extension functions; the common API works on
  `String`/`ByteArray`.
- GML: `GMLHandler` is no longer a SAX `DefaultHandler`, and `GMLReader.read` throws `ParseException`
  instead of `SAXException`/`ParserConfigurationException`.

### Removed
- `java.io.Serializable` / `serialVersionUID` (use WKT/WKB for persistence).
- `Cloneable` / `clone()` from the multiplatform surface (use the runtime-type-preserving `copy()`).
- The JVM-desktop-only `org.locationtech.jts.awt` package and internal dev/debug helpers.
- The `jts.overlay` / `jts.relate` system-property switches.

See [`doc/kotlin/KOTLIN-MP-COMPATIBILITY.md`](doc/kotlin/KOTLIN-MP-COMPATIBILITY.md) for the complete,
itemised list of API deviations and the rationale for each.

[Unreleased]: https://github.com/mipastgt/kts/compare/v1.20.0.0...HEAD
[1.20.0.0]: https://github.com/mipastgt/kts/releases/tag/v1.20.0.0
