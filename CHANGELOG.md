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
- **Java 8 API guardrail.** Every module's JVM compilation now passes `-Xjdk-release=1.8`
  alongside `jvmTarget = JVM_1_8`. `jvmTarget` fixes only the class-file version; the API surface
  came from whichever JDK ran Gradle, so a call to a method added after Java 8 would have compiled
  into class-file 52 and then failed with `NoSuchMethodError` on a real Java 8 JVM. This is the
  Kotlin counterpart of the `options.release = 8` the Java test sources already had, and it makes
  the advertised Java 8 floor enforced rather than asserted.

### Fixed
- The published `-javadoc.jar` now bundles the full **Dokka HTML API documentation** instead of a
  manifest-only stub. The jar had been wired to Dokka Gradle Plugin v2's empty `dokkaGenerate`
  lifecycle task; it now uses `dokkaGeneratePublicationHtml`, which owns the generated HTML.

### Changed
- Build: raised the Gradle daemon heap and Metaspace (`org.gradle.jvmargs`) so a full parallel
  Kotlin/Native build of all six modules no longer exhausts Metaspace.
- CI: build and test on **JDK 25** (Temurin) instead of 21, matching the JDK the port is developed
  on. The published artifacts are unaffected — they are pinned to Java 8 by `jvmTarget` and
  `-Xjdk-release` — so this only changes the JVM the `jvmTest` suite runs on.
- Kotlin **2.4.20** (was 2.4.10); the README badge follows. The full JVM suite (2207 tests) is
  green, and the JS, Wasm/JS and Kotlin/Native compilations are unaffected, as are the
  `kotlin-js-store` lock files.
- `com.vanniktech.maven.publish` **0.37.0** (was 0.36.0). Its one behavioural change is on the
  Maven Central publish path: redundant checksums (`.asc` signature checksums and `sha256`/`sha512`)
  are excluded by default, configurable through `checksums(...)` or `mavenCentralChecksums`.
- xmlutil **1.0.2** (was 1.0.1), behind the GML and KML readers/writers.
- Dokka (2.2.0), JUnit (4.13.2), kotlinx-io (0.9.1) and kotlinx-serialization (1.11.0) are already
  current; the newer Dokka and kotlinx-serialization releases are a Beta and an RC, so they were
  left alone.
- Gradle **9.7.1** (was 9.6.1, via the wrapper).

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
