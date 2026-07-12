# JTS 1.20.0 → Kotlin Multiplatform: API Compatibility Notes

**Living document.** Records every deviation from upstream JTS (Java) API or behavior
introduced by the Kotlin/Multiplatform conversion. Maintained for downstream users
migrating from Java `jts-core` to the Kotlin/KMP port.

**Context.** Upstream JTS development continues in **Java**; this port is a *faithful
language conversion* so the library can run on Kotlin Multiplatform targets. Phase 1
(Kotlin-on-JVM) preserves the Java ABI (via `@JvmStatic`/`@JvmField`/`-Xjvm-default`), so
Java callers are unaffected there. The deviations below are the deliberate exceptions —
mostly arising in Phase 2 (removing `java.*` for KMP) and from the io-module split.

**Rule going forward:** any change that affects the existing public API surface (removal,
move, signature/visibility change, or observable behavior change) MUST be recorded here.
Pure development/debug helpers may be dropped freely — record the removal, but no
replacement is owed.

**Status legend:** `DONE` · `PLANNED` (Phase 2 / decided, not yet implemented) · `PROPOSED`.

---

## 1. Module structure

- **[DONE — Stage 2B-2c] `org.locationtech.jts.io` readers split into à-la-carte io modules.**
  Class FQNs are unchanged. WKT *writing* (`WKTWriter`, `OrdinateFormat`, `WKTConstants`,
  `Ordinate`) **remains in core** (it backs `Geometry.toText()`/`toString()`). Everything else
  moved out of core into three optional Gradle modules that depend on core:
  - **`jts-io-wkt`** (`kts-io-wkt`): `WKTReader`, WKB (`WKBReader`/`WKBWriter`/`WKBConstants`
    + `InStream`/`OutStream`/`ByteOrder*` byte plumbing). (`ParseException` moved to core; the file
    readers `WKTFileReader`/`WKBHexFileReader` moved to `jts-io-files` — see §14.)
  - **`jts-io-files`** (`kts-io-files`): the file readers `WKTFileReader`/`WKBHexFileReader`
    (multiplatform, kotlinx-io backed). See §14.
  - **`jts-io-gml`** (`kts-io-gml`): the `gml2.*` package.
  - **`jts-io-kml`** (`kts-io-kml`): the `kml.*` package.
  - **`jts-io-geojson`** (`kts-io-geojson`): the `io.geojson.*` package. See §19.

  **Impact:** users of WKB / GML / KML / GeoJSON / WKT-*reading* must add the corresponding module
  dependency; a WKT-writing / geometry-only user depends on core alone. Their **production
  dependency is core only** — the shared `io.ParseException` lives in **core** (its natural home,
  matching upstream JTS, where the whole `org.locationtech.jts.io` package is in `jts-core`), so a
  GML/KML user does not transitively pull the WKT/WKB reader. `jts-io-wkt` is only a **test**
  dependency of gml/kml (the shared `GeometryTestCase` harness, and `KMLReaderTest`, parse WKT via
  `WKTReader`). **core has zero external dependencies and zero Java source.** The satellite modules
  do pull one external dependency each where they need multiplatform I/O: `jts-io-files` uses
  **kotlinx-io** (§14), `jts-io-gml`/`jts-io-kml` use **xmlutil** (§15, §16), and `jts-io-geojson`
  uses **kotlinx-serialization-json** (§19) — attached only to those satellites. **`jts-io-kml`
  (§15) and `jts-io-gml` (§16) are now Kotlin Multiplatform**, converted from the JVM-only Java (JDK
  StAX/SAX); **`jts-io-geojson` (§19)** replaces upstream's JVM-only `org.json.simple`. **All six
  modules build for the full target matrix (§17): `jvm`,
  `js`, `wasmJs`, and 7 native targets** (macosArm64, iosArm64/iosSimulatorArm64/iosX64,
  linuxX64/linuxArm64, mingwX64). (`macosX64` was dropped 2026-07-12 — the Kotlin toolchain is
  sunsetting the Intel-mac native target.)
  - **`jts-io-wkt` is now Kotlin Multiplatform** (`jvm()` + `macosArm64()`), converted from Java.
    The `String`/`ByteArray` WKT & WKB parse path is **`commonMain`, dep-free and native-capable**:
    `WKTReader` (with a hand-rolled `WktStreamTokenizer` replacing `java.io.StreamTokenizer` — see
    §13), `WKBReader`, `WKBWriter`, `ByteOrderValues`/`ByteOrderDataInStream`/`ByteArrayInStream`,
    the `InStream`/`OutStream` interfaces, `WKBConstants`, `ParseException`. Only the `java.io`
    stream adapters stay in `jvmMain`: `InputStreamInStream`, `OutputStreamOutStream`. (The file
    readers `WKTFileReader`/`WKBHexFileReader` have moved to `jts-io-files` — §14.)
  - *Test wiring:* core's shared test infrastructure (`test.jts.GeometryTestCase` / `WKTorBReader`)
    parses WKT/WKB, so **core's own `jvmTest` depends back on `:io-wkt`** (a DAG:
    core-main → io-wkt-main → core-test → io-wkt-test, no cycle). Core exposes its compiled
    `jvmTest` classes as a consumable `testArtifacts` jar (replacing the Maven `test-jar`) that the
    io modules' tests consume.
  - *Split package:* `org.locationtech.jts.io` now spans core (Kotlin writers) and `jts-io-wkt`
    (Kotlin readers). Legal on a flat JVM classpath (no `module-info`); revisit if JPMS modules are
    ever introduced.
- **[DONE] Package-level documentation migrated from `package-info.java` to Dokka
  `includes` markdown.** The 49 in-scope (Kotlin-converted) `package-info.java` files were
  removed and their javadoc consolidated into [`kts-core/packages.md`](../../kts-core/packages.md)
  (one `# Package <fqn>` section each, HTML→Markdown). `.java` files cannot live in a KMP
  common source set, and Dokka reads package docs only from `includes` markdown, not
  `package-info.java`. **[DONE — Stage 2B-2c] `packages.md` is now wired into Dokka** (core's
  `dokka { dokkaSourceSets.configureEach { includes.from("packages.md") } }`, task
  `:core:dokkaGenerate`). One adjustment was required: Dokka's `includes` AST parser rejects
  ATX **sub-headers** (`##`/`###`) inside a `# Package` section, so the in-body sub-headings
  (migrated from `<h2>` etc.) were demoted to bold text; the `# Module`/`# Package` headers and
  all prose are unchanged. The former `io`/`io.gml2` `package-info.java` moved with their
  packages into the io modules; the `awt` one was deleted with that package (§below).

## 2. Removed public API — dev/debug helpers

Rationale: development continues upstream in Java; these are dev-only helpers with no role
in the ported geometry engine.

- **[DONE] `org.locationtech.jts.util.Debug` — removed** (JVM-only tracing built on
  reflection + `System.out`/`PrintStream`). All live call sites were removed: the
  `if (Debug.isDebugging()) { … }` blocks in `CascadedPolygonUnion`, and the
  `Debug.println(…)` diagnostics in `OverlayResultValidator`, `BufferResultValidator`,
  `BufferDistanceValidator`, and `ConformingDelaunayTriangulator`.
- **[DONE] `print(java.io.PrintStream)` debug dump methods removed** from the internal
  graph classes that carried them (`geomgraph`: `NodeMap`, `Node`, `EdgeEndStar`,
  `DirectedEdgeStar`, `EdgeEnd`, `DirectedEdge`, `Edge` (incl. `printReverse`),
  `EdgeList`, `EdgeIntersection`, `EdgeIntersectionList`, `PlanarGraph.printEdges`;
  `operation.relate.EdgeEndBundle`; `planargraph.DirectedEdge`; `noding.SegmentNode`,
  `noding.SegmentNodeList`). These existed only to be invoked reflectively by `Debug`
  and had no product or test callers.
- **[DONE] `org.locationtech.jts.util.TestBuilderProxy` — removed**
  (reflective hook into the JTS TestBuilder GUI app, using `java.awt.Color` +
  `java.lang.reflect`; not geometry functionality; no callers).
- **[DONE] `org.locationtech.jts.util.Stopwatch` — retained, reimplemented on
  `kotlin.time.TimeSource`.** The public API (`start`/`stop`/`reset`/`split`/`getTime`/
  `getTimeString`, all `Long` milliseconds) is unchanged; only the internal clock moved
  from `System.currentTimeMillis()` to `TimeSource.Monotonic` marks (behavior-equivalent,
  monotonic — no wall-clock/`java.lang.System` dependency). Used by the JVM-only perf test
  suite.
- **[DONE, JVM-only] `org.locationtech.jts.util.Memory` — kept JVM-only.** Reports JVM heap
  usage via `java.lang.Runtime.getRuntime()`, which has no KMP-common equivalent. It is used
  only by the JVM-only performance tests (`test.jts.perf.*`), so it belongs to the JVM
  source set (alongside that test infrastructure) at the Stage-2B target split; it is **not**
  a common-source blocker and is left unchanged.

## 3. Removed configuration mechanisms

- **[DONE] System-property overrides dropped.** `GeometryOverlay` and `GeometryRelate`
  no longer read `-D` system properties (`jts.overlay` / `jts.relate`) at startup to select
  the overlay/relate implementation (the `init` block calling
  `setOverlayImpl(System.getProperty(...))` was removed from each). The shipped default is
  used; the same selection remains reachable through the normal API
  (`setOverlayImpl`/`setRelateImpl`, still present, and the `*_PROPERTY_VALUE_*` constants
  are retained for those callers). Removes the last `java.lang.System.getProperty` use. The
  `Debug` enable property is likewise gone.
- **[DONE] `ConformingDelaunayTriangulator` always throws on non-convergence.** The
  constraint-enforcement loop previously guarded its `ConstraintEnforcementException`
  with `if (!Debug.isDebugging())`, i.e. the exception was suppressed only when the
  (JVM-only, `-D`-toggled) debug flag was on. With `Debug` removed, the shipped default
  (debug off → exception thrown) is now the sole behavior; the throw is unconditional.

## 4. Behavioral changes (no signature change)

- **[DONE] `StringUtil.NEWLINE` is always `"\n"`** (was the platform `line.separator`
  via `System.getProperty`, i.e. `\r\n` on Windows), and is now a compile-time
  `const val`. Cosmetic; affects only formatted/diagnostic string output. (`StringUtil`
  IO helpers were also de-`java.io`-ed: `getStackTrace(Throwable)` now delegates to
  `Throwable.stackTraceToString()` instead of `ByteArrayOutputStream`+`PrintStream`.)
- **[DONE] `PackedCoordinateSequence` coordinate cache no longer uses `SoftReference`.**
  The `coordRef` cache of the expanded `Coordinate[]` is now a plain strong reference
  (`Array<Coordinate>?`); it is still cleared by the ordinate setters, but is no longer
  GC-droppable under memory pressure. Memory-pressure behavior change only.
- **[DONE] `CoordinateArrays.enforceConsistency` result array component type is
  `Coordinate[]`** (was the input's exact runtime subtype, e.g. `CoordinateXY[]`, created
  via `java.lang.reflect.Array.newInstance`; now `arrayOfNulls<Coordinate>`). Observable
  only to callers inspecting the array's runtime component type — none in JTS do. Both
  `enforceConsistency` overloads also now compare coordinate runtime types with Kotlin
  `::class` (`KClass`) instead of `java.lang.Class`/`.javaClass`.
- **[DONE] `index.quadtree.NodeBase.items` is a plain `ArrayList`** (was
  `Collections.synchronizedList(...)`). The per-node item list is no longer internally
  synchronized. No effect on single-threaded use; `Quadtree` was already documented as not
  supporting concurrent modification. (`Collections.synchronizedList` has no KMP-common
  equivalent.)

## 5. Visibility wideners (additive, non-breaking)

The conversion widens some upstream package-private classes/members to `public` because
Kotlin `internal` name-mangles and would block the same-package Java test suite. These
**add** to the API surface and do **not** break Java callers, so they are not itemized
individually. Examples: the entire `operation.overlayng` package, `OverlapUnion`
(incl. `isUnionOptimized()`), and several `coverage` classes. See the conversion notes for
the per-package list.

## 6. Java serialization removed

- **[DONE] `java.io.Serializable` support fully removed** (Phase 2, JVM build). The
  `implements Serializable` marker and all `serialVersionUID` fields are dropped from every
  core type that carried them (31 classes: `Coordinate`(+`XY`/`XYM`/`XYZM`), `Envelope`,
  `PrecisionModel`(+`Type`), `LineSegment`, `CoordinateList`, the whole `Geometry`
  hierarchy, `GeometryFactory`, the `CoordinateSequence` impls/factories, `math.DD`,
  `triangulate.ConstraintEnforcementException`, and the `index.strtree`/`index.quadtree`
  node/tree classes). **Impact:** JTS objects can no longer be serialized via Java's
  `ObjectOutputStream`. Rationale: JVM-only mechanism with no KMP equivalent; upstream
  development continues in Java for anyone needing it. The corresponding serialization
  tests were removed (`SerializabilityTest`, `CoordinateSequenceTestBase.testSerializable`,
  `STRtreeTest`/`QuadtreeTest.testSerialization`, plus the `SerializationUtil` /
  `TestSerializable` helpers).

## 7. Collection-backing changes (sorted/heap seam)

The `java.util` sorted/heap collections have no `kotlin` common-stdlib equivalent, so
internal uses are being migrated off them.

- **[DONE] `java.util.PriorityQueue` uses replaced by `org.locationtech.jts.util.PriorityQueue`.**
  The seven internal min-heap sites (`STRtree` k-NN + nearest-neighbour, `BoundablePair`,
  `algorithm.hull.ConcaveHull`, `algorithm.construct.LargestEmptyCircle` /
  `MaximumInscribedCircle`, `simplify.RingHull`, `coverage.TPVWSimplifier`) now use JTS's
  own binary-heap. To support this, **`util.PriorityQueue` was made generic
  (`PriorityQueue<E>`, `add(E)`/`poll(): E?`/`peek(): E?`) and is no longer `@Deprecated`**
  — it is now the KMP-common heap backing. Source-compatible: the class was already public,
  and raw Java usage still compiles. (`java.util.PriorityQueue.remove()` call sites map to
  `poll()!!`, guarded by `isEmpty()`; `.size` property → `size()`.)
- **[DONE] `java.util.TreeMap` / `java.util.TreeSet` uses replaced by new internal
  `org.locationtech.jts.util.TreeMap` / `TreeSet`.** These are minimal sorted collections
  (natural key/element ordering; identity by `compareTo == 0`, matching `java.util`) that
  implement `MutableMap` / `MutableSet`, backed by a sorted array with binary search
  (get/contains O(log n); iteration of `keys`/`values`/`entries` and of the set is in
  ascending order). `TreeSet` also provides the navigable `higher(e)` / `lower(e)` queries
  (used by `triangulate.polygon.PolygonHoleJoiner`). All 17 consumer files switched from the
  `java.util` imports (11 map sites: `geomgraph` `NodeMap`/`EdgeIntersectionList`/
  `EdgeEndStar`/`EdgeList`, `operation` `BoundaryOp`/`IsSimpleOp`, `planargraph.NodeMap`,
  `triangulate` `VertexTaggedGeometryDataMapper`/`ConformingDelaunayTriangulationBuilder`,
  `noding` `SegmentNodeList`/`SegmentStringDissolver`; 6 set sites: `IsSimpleOp`,
  `union.PointGeometryUnion`, `linemerge.LineSequencer`, `overlay.snap.GeometrySnapper`,
  `geom.GeometryCollection`, `PolygonHoleJoiner`). `PolygonHoleJoiner.findJoinableVertex`
  gained `!!` assertions because `higher`/`lower` now have an explicit nullable (`E?`)
  return where `java.util`'s were platform types (its own `== null` guard is preserved).

## 8. `Cloneable` / `clone()` removed from the KMP surface

`java.lang.Cloneable` and the native `Object.clone()` shallow copy have no `kotlin`
common-stdlib equivalent, so they are removed. Every affected type already provides a
`copy()` method (or copy constructor), which is the recommended, runtime-type-preserving
replacement — and `clone()` was already `@deprecated` throughout.

- **[DONE] These types no longer implement `Cloneable`:** `Coordinate`, `Geometry`,
  `CoordinateSequence`, `math.DD`, `geom.util.AffineTransformation`, `IntersectionMatrix`.
  Code testing `x is Cloneable` (or Java `instanceof Cloneable`) will no longer match them.
- **[DONE] `clone()` methods retained but reimplemented without `Object.clone()`:**
  `Coordinate.clone()` / `Geometry.clone()` delegate to `copy()`; `DD.clone()` →
  `DD(this)`; `AffineTransformation.clone()` → `AffineTransformation(this)`;
  `CoordinateList.clone()` builds the copy explicitly (preserving its exact upstream
  element layout). The concrete `Geometry` / `CoordinateSequence` subclasses already
  delegated `clone()` to `copy()`, so their behaviour is unchanged. **Behaviour note:** the
  (previously dead) base `Geometry.clone()` now returns a deep `copy()` rather than a
  shallow `Object.clone()`; every concrete geometry already overrode `clone()` to return a
  deep copy, so there is no observable change.
- **[DONE] Array `.clone()` → `.copyOf()`** in `VertexSequencePackedRtree` and `RingHull`.

## 9. `java.lang.Class` reflection removed (`.javaClass` / `Class<*>`)

`java.lang.Class` and `.javaClass` (JVM reflection) are replaced by `kotlin.reflect.KClass`
(`::class`), whose `simpleName`/`qualifiedName`/equality are common-stdlib (no `kotlin-reflect`
artifact required — verified against `kotlin.jvm.internal.ClassReference`).

- **[DONE] `Geometry.isEquivalentClass`** now compares `this::class == other::class`
  (was `this.javaClass.name == other.javaClass.name`). Behaviour-identical (same-class test);
  drives `equalsExact`.
- **[DONE] Runtime-type comparisons** in `GeometryFactory.buildGeometry` (heterogeneity check,
  `Class<*>` locals → `KClass<*>`, `geom::class`) and `GeometryEditor.editGeometryCollection`
  (`collectionForType::class == MultiPoint::class`, etc.) use `KClass` equality.
- **[DONE] Diagnostic messages** that embedded the runtime class name now use
  `x::class.qualifiedName` instead of `x.javaClass.name` (`GeometryGraph`, `IsValidOp`,
  `RepeatedPointTester`, `BufferCurveSetBuilder`, `GeometryFixer`, `GeometryTransformer`,
  `GeometryFactory`, `GeometryEditor`). On the JVM `qualifiedName` equals the old FQN for these
  top-level classes; unhappy-path text only.
- **[DONE] `GeometryExtracter` `Class<*>` API → `KClass<*>`.** The constructor and the
  `extract(Geometry, Class, …)` overloads now take `kotlin.reflect.KClass<*>?` (Java callers
  would pass `Foo::class` / `JvmClassMappingKt.getKotlinClass`). The internal `toGeometryType`
  mapping switched from `clz.isAssignableFrom(Foo::class.java)` to `clz == Foo::class`
  (KMP-common has no `isAssignableFrom`); exact-class behaviour is preserved. **This API path
  is unused in JTS** (all callers use the `String` geometry-type overloads), so no internal
  or test call site changed.

## 10. Residual `java.lang.System` uses eliminated

Bare (unqualified) `System.*` calls resolve to `java.lang.System` and were not caught by the
earlier `java.*`-prefixed grep gate. All live uses are now gone:

- **[DONE] `System.arraycopy` → `copyInto`** in `util.IntArrayList` (2) and
  `geom.CoordinateArrays.scroll` (3). Behaviour-identical bulk array copy.
- **[DONE] `System.currentTimeMillis` → `kotlin.time`** (see §2, `Stopwatch`).
- **[DONE] `System.getProperty` removed** (see §3, `GeometryOverlay`/`GeometryRelate`).

## 11. Multiplatform split (Stage 2B-2b): concurrency, `CoordinateList`, formatting

Introducing the Gradle Kotlin Multiplatform build with a `commonMain` compiled for a native
target (`macosArm64`) surfaced JVM-only constructs that had to change for `commonMain` purity.

- **[DONE] Internal synchronization removed (concurrency seam).** `kotlin.synchronized` /
  `@Synchronized` / `@Volatile` do not exist in `commonMain`, so the lazy-build / prepared-geometry
  guards were dropped: `@Synchronized` on `algorithm.locate.IndexedPointInAreaLocator.createIndex`,
  `geom.prep.PreparedLineString.getIntersectionFinder`, `geom.prep.PreparedPolygon`
  (`getIntersectionFinder`/`getPointLocator`), `index.strtree.AbstractSTRtree.build`,
  `index.intervalrtree.SortedPackedIntervalRTree.init`; the `synchronized(this){}` /
  `synchronized(items){}` blocks in `index.hprtree.HPRtree.build` and
  `index.quadtree.NodeBase.visitItems`; and the `@Volatile` markers on the corresponding
  built/root fields. **Impact:** these types are no longer safe for concurrent *first use*
  (lazy build). This matches JTS's documented threading model (most classes are not thread-safe)
  and the earlier `NodeBase.items` change (§4). Single-threaded behavior is unchanged.
- **[DONE] `CoordinateList` no longer extends `java.util.ArrayList`.** `kotlin.collections.ArrayList`
  is `final` on non-JVM targets, so `CoordinateList` now **implements** `MutableList<Coordinate>`
  (by delegating to an internal `ArrayList`) instead of extending `ArrayList`. It is still a
  `List<Coordinate>` (on the JVM, a `java.util.List`) with all the same list operations and the
  JTS-specific `add(...)`/`closeRing`/`toCoordinateArray` methods. **Impact:** it is no longer an
  `instanceof ArrayList`, and `ArrayList`-specific methods (`ensureCapacity`, `trimToSize`, `clone`)
  are gone. No JTS code relied on these.
- **[DONE] `OrdinateFormat` finite-value formatting is an `expect`/`actual` seam
  (`OrdinateFormatImpl`).** JVM `actual` = `java.text.DecimalFormat` (unchanged reference
  behavior). Native/other `actual` = a pure-Kotlin shortest-digit formatter. **Known limitation
  (native, Stage 2B follow-up):** on the rounding path (`create(n)` with a small `n`) the native
  formatter rounds the shortest decimal rather than the exact binary value, so it can differ from
  `DecimalFormat` by one ulp for half-way doubles; the default formatter (`MAX_FRACTION_DIGITS`)
  never rounds and is exact. To be pinned down by a native test.
- **[NOTE] Build system.** With `expect`/`actual` in `commonMain`, the module builds with **Gradle
  Kotlin Multiplatform only**; the legacy Maven build no longer applies. The Gradle `jvmTest` task
  runs the identical Java suite (same Surefire selection) and is the reference guardrail — now
  **2127** (was 2129) after the Stage 2B-2c `awt` drop removed `PolygonShapeTest` (§12).

## 12. Stage 2B-2c: io module split, `awt` drop, Dokka, toolchain

- **[DONE] `awt` package dropped.** The `org.locationtech.jts.awt` package (9 files: `ShapeWriter`,
  `ShapeReader`, `PointTransformation`/`IdentityPointTransformation`, `PointShapeFactory`,
  `PolygonShape`, `GeometryCollectionShape`, `ShapeCollectionPathIterator`, `FontGlyphReader`) was
  Swing/AWT `java.awt.Shape` rendering — not geometry, and referenced by nothing in core. Deleted
  along with its `PolygonShapeTest` (2 tests: `testFlatness`, `testEmptyHole`) → guardrail 2129 → 2127.
  **Impact:** callers that converted JTS geometries to `java.awt.Shape` must vendor these classes;
  they have no replacement in the KMP core (AWT is inherently JVM-desktop-only).
- **[DONE] Multi-project Gradle build.** The root is a Gradle build (`settings.gradle.kts` +
  `build.gradle.kts` at the repo top) including `:kts-core`, `:kts-io-wkt`, `:kts-io-gml`,
  `:kts-io-kml`, `:kts-io-geojson`, `:kts-io-files` (directory names match project names). The io
  modules and their tests moved out of core (§1).
- **[DONE] io reader split** — see §1.
- **[DONE] Toolchain updated to latest stable:** Gradle **9.6.1** (via the wrapper, `./gradlew`),
  Kotlin **2.4.0**, Dokka **2.1.0**, on JDK **25**. Dokka 2.1.0 (K2 analysis) is required — Dokka
  2.0.0's K1 analysis crashed parsing the JDK-25 version string. The Dokka Gradle Plugin **V2** is
  enabled via `org.jetbrains.dokka.experimental.gradle.pluginMode=V2Enabled` in `gradle.properties`.
  Guardrail re-validated green at 2127 and the `macosArm64` native compile stays green under 2.4.0.

## 13. `jts-io-wkt` common conversion (Stage 2B-2c)

`jts-io-wkt` was converted from JVM-only Java to Kotlin Multiplatform (`commonMain` + `jvmMain`);
the WKT/WKB `String`/`ByteArray` parse path is now dep-free common (compiles for `macosArm64`).
JVM ABI and the untouched Java io test suite (110 tests) are preserved.

- **[DONE] `IOException` expect/actual seam.** `org.locationtech.jts.io.IOException` is an
  `expect open class`; the JVM `actual` is a **`typealias` to `java.io.IOException`** (so the JVM
  ABI, `throws` clauses, and Java `catch (IOException)` are byte-identical), and the native `actual`
  is a plain `Exception` subclass. Only the `jvmMain` stream/file adapters ever throw it; the common
  `ByteArray`/`String` path does not. (Same pattern okio/kotlinx-io use.)
- **[DONE] `WKTReader` tokenizer replaced.** `java.io.StreamTokenizer` → a hand-rolled common
  `WktStreamTokenizer` over a `WktCharStream` abstraction, reproducing the reader's exact
  `StreamTokenizer` configuration (word chars `[A-Za-z0-9.+-]` + code points ≥ 160, whitespace
  `0..' '`, `#` line comments, numbers returned as words and parsed via `String.toDouble`). Parse
  results are identical on the JVM (validated by the full `WKTReaderTest` suite).
- **[BREAKING, minor] `WKTReader.read(java.io.Reader)` is now a JVM-only Kotlin extension**
  (`WKTReaderExtensions.read`), not a member — because `WKTReader` is common and `java.io.Reader`
  is JVM-only. The streaming semantics (consume one geometry, leave the reader positioned for the
  next) are unchanged. **Impact:** Java callers change `wktReader.read(reader)` →
  `WKTReaderExtensions.read(wktReader, reader)`; Kotlin JVM callers are unaffected (extension
  resolves as `wktReader.read(reader)`). `read(String)` is unchanged and is the common API. The one
  in-tree caller, `WKTFileReader`, was updated.
- **[DONE] `EnumSet<Ordinate>` fully eliminated.** `WKBReader`/`WKBWriter` used `java.util.EnumSet`
  internally and (for `WKBWriter`) in the public `get/setOutputOrdinates` signatures. These are now
  `MutableSet<Ordinate>` / `Set<Ordinate>` (JVM `java.util.Set`), matching core's `Ordinate`
  (already narrowed from `EnumSet`). `setOutputOrdinates(Set)` still accepts a Java `EnumSet.of(...)`
  argument. No test assigned the getter to an `EnumSet` variable.
- **[DONE] `java.io.ByteArrayOutputStream` → common `ByteArrayOutStream`.** `WKBWriter.write(Geometry)`
  (the `ByteArray`-returning path) now accumulates into a small growable common `OutStream` instead
  of `ByteArrayOutputStream` + `OutputStreamOutStream`.
- Mechanical common substitutions: `Double.longBitsToDouble`/`doubleToLongBits` → `Double.fromBits`/
  `.toBits()` (`.toBits()`, not `.toRawBits()`, to match NaN normalization); `Character.digit(c,16)`
  → `c.digitToIntOrNull(16) ?: -1`; `System.arraycopy` → `copyInto`; `StringBuffer` → `StringBuilder`;
  `toUpperCase(Locale.ROOT)` → `uppercase()`.

## 14. `jts-io-files` module (kotlinx-io file readers)

**[DONE]** The file readers `WKTFileReader` and `WKBHexFileReader` — formerly JVM-only Java in
`jts-io-wkt`'s `jvmMain` — are now a dedicated **multiplatform** module **`jts-io-files`**
(`kts-io-files`, `kotlin("multiplatform")`, `jvm()` + `macosArm64()`), backed by
**kotlinx-io** (`org.jetbrains.kotlinx:kotlinx-io-core`) for file access. This is the first
core/io dependency on an external library; it attaches only to this satellite (core and the WKT/WKB
parse path stay dependency-free).

- **Class FQNs unchanged** (`org.locationtech.jts.io.WKTFileReader` /
  `org.locationtech.jts.io.WKBHexFileReader`) — the package is unchanged; only the owning module
  (and therefore the Maven artifact) changed. **Impact:** a consumer that reads geometry *files*
  must add the `jts-io-files` dependency (previously these classes came with `jts-io-wkt`). The
  public method surface (`setLimit`/`setOffset`/`setStrictParsing`/`read()`) is unchanged.
- **Constructors:** both readers are `expect`/`actual` classes. The common `expect` declares the
  by-name constructor `(filename: String, reader)`. The **JVM `actual` additionally keeps the
  historical `java.io.File` and `java.io.Reader` constructors** (the `Reader` form still serves
  classpath-resource streams, e.g. `InteriorPointTest`), so existing JVM call sites compile
  unchanged. Native has only the by-name constructor.
- **Charset behaviour change (JVM):** the by-name `(filename, reader)` constructor now reads the
  file via kotlinx-io `SystemFileSystem`, which decodes **UTF-8** on every platform. The former Java
  `WKTFileReader(String)` used a `FileReader` (platform-default charset). For the ASCII WKT / WKB-hex
  content these readers handle this is not observable; it also makes reads deterministic across
  platforms. The `File`/`Reader` JVM constructors are unchanged (java.io, default charset).
- **Streaming seam:** to parse a *sequence* of WKT geometries from one file, `jts-io-wkt` now exposes
  a public streaming entry point — the `WktCharStream` interface and `WKTReader.read(source:
  WktCharStream)` (formerly the `internal readFromSource`). `jts-io-files` feeds file content through
  a shared `WktCharStream`, reproducing the former `WKTFileReader` geometry-by-geometry streaming
  (each geometry ends on a single-character `)` token, so the shared source is left correctly
  positioned between geometries).
- The `read()` behaviour (offset/limit, non-strict partial results, blank-line skipping for WKB-hex)
  is preserved; a new `jts-io-files` `jvmTest` (`FileReaderTest`) validates it end-to-end, including
  the kotlinx-io read path on the JVM.

## 15. `jts-io-kml` common conversion (xmlutil)

**[DONE]** `jts-io-kml` is converted from JVM-only Java to **Kotlin Multiplatform** (`jvm()` +
`macosArm64()`). Both `KMLReader` and `KMLWriter` are now `commonMain` Kotlin; class FQNs and the
public method surface are unchanged.

- **Reader — StAX → xmlutil.** `KMLReader` no longer uses the JDK StAX pull parser
  (`javax.xml.stream.XMLStreamReader` / `XMLInputFactory`); it is rewritten on **xmlutil**
  (`io.github.pdvrieze.xmlutil:core`, an `implementation` dependency of this module only). The
  parsing logic is a faithful port: an internal `StaxLikeReader` adapter over xmlutil's `XmlReader`
  preserves the original "current event" semantics (`isStartElement`/`isEndElement`/`getLocalName`/
  `getElementText`), so the geometry handling and the **exact `ParseException` messages** (e.g.
  "No element coordinates found in Point", "Empty coordinates", "Invalid coordinate format",
  "Unknown KML geometry type ...") are unchanged. `read(String)` behaviour is identical
  (validated by the unchanged `KMLReaderTest`). `java.util.regex` → `kotlin.text.Regex`;
  `java.lang.reflect.Array.newInstance` → typed `Array(size){ … }` builders; `String.format` → a
  small `%s` substitution helper.
- **Writer — pure common.** `KMLWriter` used no XML library (string building), so it is common
  as-is with two substitutions: `StringBuffer` → `StringBuilder`, and its private
  `java.text.DecimalFormat` precision formatter is replaced by core's **`OrdinateFormat`**
  (`OrdinateFormat.create(precision)`), whose output — locale-independent `.` separator, `HALF_EVEN`
  rounding, no scientific notation — matches the former `DecimalFormat("0." + "#" × precision)`
  exactly (validated by `KMLWriterTest`, including `testPrecision`). The default (no-precision) path
  still emits `Double.toString`.
- **Minor API change — `write(Geometry, java.io.Writer)`.** The `KMLWriter.write(Geometry, Writer)`
  member cannot live in a common class. It is preserved as a **JVM-only extension** (in
  `KMLWriterExtensions`, `@file:JvmName`), mirroring the `WKTReader.read(Reader)` precedent (§13):
  from Java call `KMLWriterExtensions.write(kmlWriter, geometry, writer)` instead of
  `kmlWriter.write(geometry, writer)`. The string-returning `write(Geometry)` is unchanged.
- **`ALTITUDE_MODE_*` constants** are unchanged public static `String` fields (`@JvmField var` in the
  companion), preserving their exact values (including the historical trailing spaces on
  `clampToGround `/`relativeToGround  `).

## 16. `jts-io-gml` common conversion (xmlutil) — **breaking SAX API change**

**[DONE]** `jts-io-gml` is converted from JVM-only Java to **Kotlin Multiplatform** (`jvm()` +
`macosArm64()`). The reader is rewritten on **xmlutil**; the writer is pure string building. Unlike
the WKT/KML conversions, this **breaks the public SAX API** (an intentional, approved trade for a
multiplatform reader — GML's reader was built directly on `org.xml.sax`).

- **`GMLHandler` is no longer a SAX `org.xml.sax.helpers.DefaultHandler`.** It is a plain Kotlin
  class fed element/character events by `GMLReader` (which drives an xmlutil pull parser). Its
  `getGeometry()` / `isGeometryComplete()` remain; the SAX callback methods
  (`startElement`/`characters`/`endElement` with `org.xml.sax` signatures, and
  `error`/`warning`/`fatalError`/`setDocumentLocator`) are **removed**. Consequently GMLHandler can
  no longer be registered as a handler on an external SAX parser. The `ErrorHandler delegate`
  constructor parameter is gone; the constructor is now `GMLHandler(GeometryFactory)`.
- **`GMLReader.read` no longer throws `org.xml.sax.SAXException` or
  `javax.xml.parsers.ParserConfigurationException`.** It throws `org.locationtech.jts.io.ParseException`
  (wrapping any XML well-formedness error). The internal parse strategies that formerly threw
  `SAXException("…")` now throw `ParseException("…")` with the **same messages**.
- **`GMLReader.read(java.io.Reader, GeometryFactory)`** (a common class cannot take a
  `java.io.Reader`) is preserved as a **JVM-only extension** (`GMLExtensions.read(gmlReader, reader,
  gf)`); the common entry point is `read(String, GeometryFactory)`. Likewise
  **`GMLWriter.write(Geometry, java.io.Writer)`** is a JVM-only extension
  (`GMLExtensions.write(gmlWriter, geometry, writer)`); the common `write(Geometry): String` is
  unchanged. (Same precedent as `WKTReader.read(Reader)` / `KMLWriter.write(Writer)`.)
- **Undeclared namespace prefixes.** GML fragments routinely use an undeclared `gml:` prefix
  (`<gml:Point>…`), which the original reader accepted because it set
  `SAXParserFactory.setNamespaceAware(false)`. xmlutil is namespace-aware and rejects undeclared
  prefixes, so the reader uses xmlutil's `KtXmlReader` in **`relaxed = true`** mode, which tolerates
  them (matching the old namespace-disabled behaviour). Element lookup uses the prefix-stripped
  local name; attributes are matched by qualified name (the namespace-qualified attribute lookups,
  which never matched under the old namespaces-disabled parser, still never match). The relaxed
  `KtXmlReader` constructor and the multiplatform `StringReader` are marked `@XmlUtilInternal`
  (opt-in required), so `GMLReader.read` carries `@OptIn(XmlUtilInternal::class)`.
- **`GeometryStrategies` / `ParseStrategy`** are now internal implementation details (the former
  `public static ParseStrategy findStrategy(...)` and the `ParseStrategy` interface are `internal`).
- **`GMLConstants`** is unchanged as public constants (a Kotlin `object` of `const val` — Java still
  sees `GMLConstants.GML_POINT` etc. as static final fields).
- Behaviour is validated by the unchanged `GMLReaderTest` (parsing) and the `Static*Test`
  write→read round-trips; the only test edits are the removed `SAXException`/`ParserConfigurationException`
  catches/`throws` and the two java.io call sites now routed through `GMLExtensions`.

## 17. Full KMP target matrix

**[DONE]** All six modules (core + the five io satellites) build for the same target set:

- **`jvm`** — the reference target; the untouched Java test suite runs here (guardrail: 2131 green).
- **`js`** (IR, `nodejs()` + `browser()`).
- **`wasmJs`** (`nodejs()` + `browser()`).
- **7 native targets:** `macosArm64`, `iosArm64`, `iosSimulatorArm64`, `iosX64`,
  `linuxX64`, `linuxArm64`, `mingwX64`. (`macosX64` was removed 2026-07-12: the Kotlin/Native
  toolchain is sunsetting the Intel-mac target — `fun macosX64()` is deprecated for removal in a
  future Kotlin release. Apple-silicon Macs are covered by `macosArm64`.)

All three external dependencies publish for this whole matrix (kotlinx-io `0.9.1`, xmlutil `1.0.1`,
kotlinx-serialization-json `1.9.0`), so the satellites resolve on every target.

**Source-set layout.** The platform `actual`s that are pure Kotlin (not JVM-specific) are shared
across native + js + wasmJs via an intermediate **`nonJvmMain`** source set (`dependsOn(commonMain)`,
with `nativeMain`/`jsMain`/`wasmJsMain` depending on it): core's `OrdinateFormatImpl`, io-wkt's
`IOException`, io-files's `WKTFileReader`/`WKBHexFileReader`. Only the JVM keeps a distinct `actual`
(`DecimalFormat`, `java.io.IOException` typealias, the `java.io.File`/`Reader` constructors). The
build calls `applyDefaultHierarchyTemplate()` before wiring `nonJvmMain` so the intermediate
`nativeMain`/`jsMain`/`wasmJsMain` source sets exist to be re-parented.

**Behaviour change — diagnostic messages use the simple class name.** Kotlin/JS does not support
`KClass.qualifiedName`. The 12 internal "should never reach here" diagnostics that formerly embedded
the fully-qualified class name (`::class.qualifiedName`, itself the KMP replacement for the original
`getClass().getName()`) now use `::class.simpleName` (supported on every platform). This affects only
the text of these internal error messages (e.g. `Point` instead of `org.locationtech.jts.geom.Point`);
no test asserts them and no control flow depends on it. `Geometry.isEquivalentClass` and the other
`::class` *equality* comparisons are unchanged (`KClass` equality is supported on all targets).

**io-files on the browser.** `jts-io-files` compiles for `js`/`wasmJs`, but kotlinx-io's
`SystemFileSystem` is backed by Node's `fs` / WASI only — so the by-name file readers function under
the JVM, native, Node, and WASI, and are non-functional in a browser (there is no filesystem to read).

## 18. Native runtime validation (`commonTest`)

Until now only the JVM target ran tests (the ported Java suite). A **`commonTest`** (kotlin.test)
suite now validates *runtime* behaviour on the non-JVM targets — the same assertions run on every
target's test task, so passing on the JVM (the reference) and on native/js/wasm proves parity.

- **core `commonTest`** (19 tests, `org.locationtech.jts.kmptest`): the pure-Kotlin reimplementations
  and `expect`/`actual` seams that carry the real off-JVM risk — `OrdinateFormat` (the hand-rolled
  shortest-digit formatter vs `DecimalFormat`; DEFAULT path asserted byte-exact, the documented
  half-way rounding divergence deliberately not asserted), `util.Random` (checked against an inline
  re-derivation of `java.util.Random`'s LCG), `TreeMap`/`TreeSet`/`PriorityQueue`, and
  `DoubleBits`/`CommonBits` — plus representative geometry ops (area/length/centroid, predicates,
  overlay, convex hull, buffer, distance, `WKTWriter`). The buffer area is asserted within tolerance
  to absorb any 1-ulp platform-`libm` trig difference.
- **io-wkt `commonTest`** (4 tests, `org.locationtech.jts.io.kmptest`): WKT string roundtrips (the
  hand-rolled `WktStreamTokenizer` + parser + format path) and WKB binary/hex roundtrips in both byte
  orders.
- **io-geojson `commonTest`** (5 tests, `org.locationtech.jts.io.geojson`): read→write→read
  roundtrips over all GeoJSON geometry types plus empty geometries (compared structurally via
  `equalsExact`, since whole-number ordinate rendering differs off-JVM — see §19), CRS/SRID parsing,
  a factory-built write path, and the two parse-failure cases.

**Where it actually runs.** On the macOS/arm64 dev host the suite executes on **jvm + macosArm64
(real native) + jsNode + wasmJsNode** — all green. Kotlin auto-disables the test tasks for targets
that cannot run on the host (`linuxX64`/`linuxArm64`/`mingwX64`/`iosArm64`/`iosX64`), so those are
validated by cross-compilation of the test binary only. The JVM guardrail is now **2159** (2131 Java
+ 19 core-common + 4 io-wkt-common + 5 io-geojson-common). The io-geojson JVM target additionally
runs the 41 ported Java tests (`GeoJsonReaderTest`, `GeoJsonWriterTest`, `GeoJsonTest`), which cover
exact-string writer parity with upstream JTS.

Run: `./gradlew :kts-core:macosArm64Test :kts-core:jsNodeTest :kts-core:wasmJsNodeTest
:kts-io-wkt:macosArm64Test :kts-io-wkt:jsNodeTest :kts-io-wkt:wasmJsNodeTest` (add `:*:jvmTest`).

## 19. `jts-io-geojson` common conversion (kotlinx-serialization)

**[DONE]** `jts-io-geojson` is the GeoJSON reader/writer, converted from upstream JTS's JVM-only
`org.locationtech.jts.io.geojson` (in the `jts-io-common` Java module) to **Kotlin Multiplatform**
(`jvm` + `js` + `wasmJs` + 7 native). Class FQNs and the **public API are unchanged**
(`GeoJsonReader`, `GeoJsonWriter`, `GeoJsonConstants`, `OrientationTransformer`); Java callers use
them exactly as before.

- **JSON backend — `org.json.simple` → kotlinx-serialization-json.** Upstream's only external
  dependency here was the JVM-only `json.simple`. The reader now parses with
  `Json.parseToJsonElement` and walks the `JsonElement` tree (`JsonObject`/`JsonArray`); the writer
  builds a `JsonObject` tree and serialises it with `Json.encodeToString`. The upstream reader
  performed unchecked casts on `java.util.Map`/`List`, so the tree-walk is a near-mechanical
  translation; the null-handling (absent key or JSON `null` → empty geometry) is preserved by
  typed accessors (`GeoJsonElements.kt`) that read both as Kotlin `null`.
- **Exception semantics preserved.** The `create*` helpers keep upstream's catch-only-
  `RuntimeException` behaviour (so a nested `ParseException` propagates rather than being
  re-wrapped), and all `ParseException` messages are byte-identical. `read(String)` is annotated
  `@Throws(ParseException::class)` so Java still sees the checked exception.
- **Exact writer output on the JVM.** `GeoJsonWriter.getJsonString`/`formatOrdinate` are ported
  verbatim (the custom scale-rounding + "emit `long` when integral" rendering), and the pre-formatted
  coordinate arrays are injected as raw JSON via **`JsonUnquotedLiteral`** — the kotlinx analog of
  json.simple's `JSONAware`. With compact, insertion-ordered `Json` output this reproduces upstream's
  `JSONObject.writeJSONString` byte layout, validated by the unchanged `GeoJsonWriterTest` (18 exact-
  string assertions). **Caveat:** `formatOrdinate` falls back to `Double.toString`, whose whole-number
  rendering differs on JS/native (e.g. `0` vs `0.0`), so byte-exact output parity is guaranteed on the
  JVM only; the `commonTest` roundtrips therefore compare geometries structurally, not by string.
- **`isEncodeCRS` / `isForceCCW`.** The two boolean setters become Kotlin `var`s; the `is`-prefixed
  names make Kotlin emit the original Java setter names (`setEncodeCRS`/`setForceCCW`), so the ported
  Java tests call them unchanged.
- **Stream (`java.io`) overloads are JVM-only extensions.** `GeoJsonReader.read(java.io.Reader)` and
  `GeoJsonWriter.write(Geometry, java.io.Writer)` live in `GeoJsonExtensions` (`@file:JvmName`), same
  precedent as `WKTReader.read(Reader)` (§13) and the GML/KML writers (§15, §16); the common entry
  points `read(String)` / `write(Geometry): String` are unchanged.
- Behaviour is validated by the unchanged `GeoJsonReaderTest`/`GeoJsonWriterTest`/`GeoJsonTest` on the
  JVM plus the multiplatform `GeoJsonRoundtripTest` (§18) on native/js/wasm.

## 20. Public geometry / builder / factory classes made `open`

**[DONE]** Additive, non-breaking. In upstream Java JTS these classes are not `final` (Java classes
are open by default), so they can be subclassed — a supported extension pattern (e.g. deriving a
custom builder or a specialised geometry type). The Kotlin conversion made them `final` only by
accident of Kotlin's `class`-is-final-by-default rule: the pass added `open` **only where the port's
own hierarchy required it** (e.g. `LineString` ← `LinearRing`, `GeometryCollection` ← the `Multi*`
types). Leaf classes with no in-tree subclass were left final. This restores upstream openness for
the public geometry model and the public builder/factory surface, so downstream code can subclass
them as it could against Java JTS.

- **Classes marked `open` (43):**
  - `geom`: `Point`, `Polygon`, `LinearRing`, `MultiPoint`, `MultiLineString`, `MultiPolygon`,
    `Envelope`, `PrecisionModel`, `Triangle`, `IntersectionMatrix`, `OctagonalEnvelope`.
  - `geom.impl`: `CoordinateArraySequence`, `PackedCoordinateSequenceFactory`.
  - `geom.util`: `GeometryEditor`, `GeometryFixer`, `GeometryCombiner`, `GeometryExtracter`,
    `LineStringExtracter`, `PointExtracter`, `PolygonExtracter`, `LinearComponentExtracter`,
    `ComponentCoordinateExtracter`, `PolygonalExtracter`, `AffineTransformation`,
    `AffineTransformationBuilder`, `AffineTransformationFactory`, `GeometryMapper`,
    `GeometryCollectionMapper`, `SineStarFactory`. (`GeometryTransformer` was already `open`.)
  - `geom.prep`: `PreparedGeometryFactory`, `PreparedPoint`, `PreparedLineString`, `PreparedPolygon`.
  - `shape`: `CubicBezierCurve`; `shape.fractal`: `HilbertCurveBuilder`, `KochSnowflakeBuilder`,
    `MortonCurveBuilder`, `SierpinskiCarpetBuilder`; `shape.random`: `RandomPointsInGridBuilder`
    (`RandomPointsBuilder` was already `open`).
  - `linearref`: `LinearGeometryBuilder`.
  - `triangulate`: `VoronoiDiagramBuilder`, `ConformingDelaunayTriangulationBuilder`,
    `DelaunayTriangulationBuilder`.
- **Deliberately *not* opened (this step):** internal engine builders widened to `public` only for
  the same-package Java test suite (§5) — the `operation.overlay*` / `geomgraph` / `edgegraph` /
  `noding` / `index` / buffer / polygonize builders; static-utility holders with only companion
  members (`Quadrant`, `Dimension`, `Position`, `Location`, `Coordinates`, `CoordinateSequences`,
  `CoordinateArrays`, `GeometryOverlay`); private-constructor singletons/builders
  (`CoordinateArraySequenceFactory`, `TriangulationBuilder`); the `geom.prep` predicate-evaluator
  helpers; and the exception types. These are not part of the geometry/builder/factory extension
  surface; they can be opened later if a concrete need arises.
- **Scope of "open".** Opening a class enables subclassing and adding members. *Overriding* existing
  behaviour additionally requires the specific member to be `open`; members that already `override` a
  base declaration are open by Kotlin's rules, but non-overriding leaf methods remain final. No
  members were newly opened in this step.
- **Compatibility.** Source- and binary-compatible (only widens what is permitted); the unmodified
  Java guardrail suite (`:kts-core:jvmTest`) and the JVM + `macosArm64` compiles remain green.

## 21. Kotlin property-style accessors (additive extension layer)

**[DONE]** Additive, non-breaking, opt-in. The port keeps upstream JTS's explicit Java getters
(`getArea()`, `getCentroid()`, …) — a faithful-conversion decision, so the ported body stays
byte-identical to upstream Java (which matters for re-porting from upstream). The cost was
ergonomic: when Kotlin calls the *Java* JTS artifacts, the compiler synthesizes `geom.area` from
`getArea()`; but Kotlin does **not** synthesize properties from *Kotlin*-declared `getX()` functions,
so against this port callers had to write `geom.getArea()`.

`geom/PropertyAccessors.kt` restores the `.property` idiom as a thin layer of **extension
properties** that delegate to the underlying (virtual) getters — e.g. `val Geometry.area get() =
getArea()`. Chosen over converting the getters to real Kotlin properties because that would have
forced hundreds of internal getter→property call-site edits and permanently diverged the ported code
from upstream's getter-call style; the extension layer keeps the port faithful and touches nothing
existing.

- **Surface covered:** the public no-arg accessors of `Geometry` (and thus every subtype),
  `Point`, `LineString`, `Polygon`, `MultiLineString`, `Coordinate` (`m`, `isValid`), `Envelope`,
  `LineSegment`, `PrecisionModel`, `Triangle`. Names match what Kotlin-on-Java-JTS synthesizes
  (`area`, `centroid`, `numGeometries`, `isEmpty`, `SRID`, …). Parameterized getters
  (`getGeometryN(i)`, `getCoordinateN(i)`, …) are unaffected — they stay functions, as in Java.
- **Opt-in by import** (`import org.locationtech.jts.geom.*`), and **invisible to Java** (extension
  properties compile to static helpers) — the Java getter API is untouched.
- **Deliberate omissions:** `Coordinate.x/y/z` (already public fields); `Geometry.envelope` (the
  protected `envelope: Envelope?` field would shadow it inside a consumer subclass and denote a
  different type than the bounding-box `Geometry` the getter returns — use `envelopeInternal`, or
  `getEnvelope()`). `factory`/`SRID` are provided: the extension resolves for ordinary callers and,
  inside a consumer subclass, the identical protected field resolves instead — same value and type.
- **Validated** by `commonTest` `PropertyAccessorTest` (asserts the accessors equal their getters,
  from a different package so field/member shadowing would surface), green on `jvm` + `macosArm64`.
