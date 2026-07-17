# Build, test & docs commands (kts KMP)

kts is Kotlin Multiplatform: 6 modules — `kts-core`, `kts-io-wkt`, `kts-io-gml`, `kts-io-kml`,
`kts-io-geojson`, `kts-io-files`. Toolchain moves forward over time; check
[`gradle/libs.versions.toml`](../../../../gradle/libs.versions.toml) and
[`build.gradle.kts`](../../../../build.gradle.kts) for the current Gradle/Kotlin/Dokka versions rather
than trusting a number written here.

## Compile

- Core JVM: `./gradlew :kts-core:compileKotlinJvm`
- A native target (proves `commonMain` purity): `./gradlew :kts-core:compileKotlinMacosArm64`

## Test — the guardrail

**There is NO `test` task** (that's a JVM-plugin thing; this is KMP). Use:

- `./gradlew jvmTest` — runs the full ported Java suite + `commonTest` on the JVM across all modules.
  Fast (~seconds). **This is the primary guardrail** during a re-base: green = faithful to upstream.
- `./gradlew allTests` — adds native/iOS test tasks; slow, needs simulators. Use before release.

### Multiplatform runtime validation (`commonTest`)

The `commonTest` suites prove the pure-Kotlin reimplementations and `expect`/`actual` seams behave the
same off-JVM. On a macOS/arm64 host, the runnable targets are:

```
./gradlew :kts-core:macosArm64Test :kts-core:jsNodeTest :kts-core:wasmJsNodeTest \
          :kts-io-wkt:macosArm64Test :kts-io-wkt:jsNodeTest :kts-io-wkt:wasmJsNodeTest \
          :kts-io-geojson:macosArm64Test :kts-io-geojson:jsNodeTest :kts-io-geojson:wasmJsNodeTest
```

Targets the host can't run (`linuxX64`/`linuxArm64`/`mingwX64`/`iosArm64`/`iosX64`) are validated by
cross-compilation of the test binary only. **Any new pure-Kotlin reimplementation or `expect`/`actual`
seam you add during a re-base needs a `commonTest` assertion** — that's the only thing proving parity
on the platforms where you replaced `java.*`.

## Dokka docs

- Aggregated multi-module site (root): `./gradlew dokkaGenerate` → `build/dokka/html/index.html`.
- Per-module: `./gradlew :kts-core:dokkaGenerateHtml` → `kts-core/build/dokka/html/index.html`.
- **`failOnWarning` is ENABLED** — an unresolvable KDoc link fails the build. Note the Gradle build
  cache can MASK Dokka warnings on re-runs; force a real run with
  `./gradlew dokkaGenerate --rerun-tasks --no-build-cache`.
- View over HTTP, not `file://` (search/nav break on `file://`):
  `python3 -m http.server 8012 --directory build/dokka/html`.
- Note: a broken link in a *detached*/in-body doc does NOT warn (Dokka ignores unattached KDoc); test
  the guard with an *attached* KDoc.

## Gotchas

- macOS BSD `sed`: no `\b` word boundary (silently fails to match). Use `[[:space:]]`, `-E`, `-i ''`.
- The root project needs `repositories { mavenCentral() }` (it runs a Dokka task).
