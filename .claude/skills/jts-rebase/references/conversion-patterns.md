# Java → Kotlin/KMP conversion patterns & the deviation catalog

This is the "how to actually convert a file" companion to `SKILL.md`. The **authoritative,
itemized** list of every deviation the port takes from upstream is
[`doc/kotlin/KOTLIN-MP-COMPATIBILITY.md`](../../../../doc/kotlin/KOTLIN-MP-COMPATIBILITY.md) — read the
relevant section before porting a file in that area. This file summarizes the recurring patterns so
you know what to watch for; the compatibility doc is the source of truth and must be updated when you
add a new deviation.

## Ground rules

- **Byte-faithful bodies.** Port method bodies as close to the upstream Java as Kotlin allows. The
  port deliberately keeps upstream's explicit Java getters (`getArea()`, not a Kotlin property) so the
  code stays diffable against upstream on the next re-base — do **not** "Kotlin-ify" getters into
  properties. (Ergonomic `.property` access is provided separately as an extension layer — see
  compatibility doc §21; don't touch the getter call sites.)
- **JVM ABI preserved (Phase 1).** On the JVM the port keeps the Java ABI via `@JvmStatic` /
  `@JvmField` / `-Xjvm-default`, and `object` companions expose `const val`/`@JvmField` so Java sees
  static finals. Preserve this when porting new statics/constants.
- **`commonMain` purity (Phase 2).** Anything in `commonMain` cannot reference `java.*`. When upstream
  adds code using JVM-only APIs, either replace with a common equivalent or push it behind an
  `expect`/`actual` seam (JVM `actual` can `typealias`/delegate to the Java type; a `nonJvmMain`
  `actual` provides the pure-Kotlin version).

## Recurring mechanical substitutions

These are the ones that come up again and again (all itemized with rationale in the compatibility doc
§4, §7–§13):

| Upstream Java | Port replacement | Notes / doc |
| --- | --- | --- |
| `implements Serializable`, `serialVersionUID` | removed entirely | §6 — also delete the serialization tests |
| `Cloneable` / `Object.clone()` | `copy()` (retained `clone()` delegates to it) | §8 |
| `java.util.TreeMap` / `TreeSet` | `org.locationtech.jts.util.TreeMap` / `TreeSet` | §7 |
| `java.util.PriorityQueue` | `org.locationtech.jts.util.PriorityQueue<E>` | §7 |
| `java.util.EnumSet<Ordinate>` | `Set<Ordinate>` / `MutableSet<Ordinate>` | §13 |
| `Collections.synchronizedList` | plain `ArrayList` (threading seam dropped) | §4, §11 |
| `@Synchronized`/`synchronized{}`/`@Volatile` | removed (lazy-build not thread-safe, matches JTS docs) | §11 |
| `System.arraycopy` | `copyInto` | §10 |
| `System.currentTimeMillis()` | `kotlin.time.TimeSource.Monotonic` | §2 (`Stopwatch`) |
| `System.getProperty(...)` config switches | removed (use the API setters) | §3 |
| `.javaClass` / `Class<*>` | `::class` / `kotlin.reflect.KClass` | §9 |
| `x.javaClass.name` in diagnostics | `x::class.simpleName` (JS has no `qualifiedName`) | §17 |
| `Double.longBitsToDouble`/`doubleToLongBits` | `Double.fromBits`/`.toBits()` (not `toRawBits`) | §13 |
| `java.text.DecimalFormat` | core `OrdinateFormat` / the `OrdinateFormatImpl` expect-actual | §11 |
| `StringBuffer` | `StringBuilder` | §13 |
| `java.util.regex` | `kotlin.text.Regex` | §15 |
| `java.io.StreamTokenizer` (WKT) | the hand-rolled `WktStreamTokenizer`/`WktCharStream` | §13 |
| `java.io.IOException` | the `expect`/`actual` `io.IOException` (JVM typealias) | §13 |
| StAX/SAX XML (GML/KML) | **xmlutil** | §15, §16 |
| `org.json.simple` (GeoJSON) | **kotlinx-serialization-json** | §19 |
| file IO | **kotlinx-io** (`kts-io-files`) | §14 |

## JVM-only `java.io` stream overloads

The port's common classes can't take `java.io.Reader`/`Writer`. Upstream member overloads like
`read(Reader)` / `write(Geometry, Writer)` become **JVM-only extension functions** in a
`*Extensions.kt` file (`@file:JvmName(...)`), so Java callers call
`XxxExtensions.method(receiver, …)`. Precedents: `WKTReaderExtensions.read` (§13),
`KMLWriterExtensions` (§15), `GMLExtensions` (§16), `GeoJsonExtensions` (§19). The `String`/`ByteArray`
entry points stay as common members. If upstream adds a new stream overload, follow this pattern.

## Visibility & openness

- **Kotlin `internal` name-mangles**, which breaks the same-package Java test suite. Where upstream is
  package-private but the Java tests need it, widen to `public` (additive, non-breaking — §5). Don't
  use `internal` for anything a test in the same package touches.
- **Kotlin classes are final by default**; upstream Java classes are open. The public geometry/
  builder/factory classes are marked `open` to preserve upstream's subclassing (§20). A new public
  extensible class should be `open` to match upstream semantics; leaf/utility classes stay final.

## KDoc (doc comments)

Port Javadoc to KDoc — Dokka `failOnWarning` is ON, so a bad link fails the build. Mechanical rules
(the port applied these tree-wide; the git history has the exact `sed` recipes):

- `{@link X}` / `{@code y}` / `<code>`/`<tt>` → `[X]` / `` `y` ``.
- `<pre>` → triple-backtick fences; convert Java sample code inside fences to idiomatic Kotlin.
- `<p>`/`<ul>`/`<li>`/`<b>`/`<i>` → Markdown; HTML entities → literal chars; `<sup>`/`<sub>` → caret
  notation / Unicode.
- Drop `@version`. Fix stale `@param` names (real drift, Dokka flags it).
- Cross-package links that won't resolve: use `[Short][fully.qualified.Name]` in prose, FQN targets in
  `@see`. Package docs live in `kts-core/packages.md` (Dokka `includes`), not `package-info.java`
  (which can't sit in a common source set).
- In-body (non-doc) `/** */` comments should be `/* */` — Kotlin attaches only the *last* KDoc block
  to a declaration, so a stray in-body `/**` silently drops the real doc. Watch stacked class-doc +
  constructor-doc: merge via `@constructor`, don't demote (demoting loses the class doc). Details and
  gotchas: the memory `dokka-conversion-status` and the commit history on `main`.

macOS `sed` gotcha: BSD `sed` has no `\b` word boundary (silently no-ops). Use `[[:space:]]`, `-E`,
`-i ''`.
