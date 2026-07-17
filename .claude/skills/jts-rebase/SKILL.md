---
name: jts-rebase
description: >-
  Re-base this Kotlin Multiplatform port (kts) onto a newer upstream JTS release — i.e. port a new
  version of the original Java JTS Topology Suite over to Kotlin/KMP without starting from zero. Use
  this whenever the task involves upgrading kts to a new JTS version, porting upstream JTS changes,
  "re-basing"/"rebasing" the port, bumping jtsBaseVersion, or reconciling this port with a newer
  locationtech/jts release. Assume you will have access to this repo plus the OLD and NEW upstream
  JTS Java sources. Read this before touching source when the trigger is a JTS version upgrade.
---

# Re-basing the kts port onto a newer JTS release

## What this is

`kts` is a **faithful language port** of the Java [JTS Topology Suite](https://github.com/locationtech/jts)
to Kotlin Multiplatform. Upstream development continues in Java; periodically a new JTS version ships
(e.g. `1.20.0` → `1.21.0`), and this port must be brought up to that version.

The whole point of this skill: **re-basing is a delta operation, not a fresh port.** The port was
built once, painstakingly, from JTS `1.20.0`. You do **not** re-convert the whole tree. You find what
changed between the old and new upstream Java, and port only that delta — re-applying the same
deliberate deviations the port already documents. Two things make this tractable and safe:

1. **The compatibility doc is the map.** [`doc/kotlin/KOTLIN-MP-COMPATIBILITY.md`](../../../doc/kotlin/KOTLIN-MP-COMPATIBILITY.md)
   itemizes *every* deliberate deviation from upstream (module split, removed `java.*`, `expect`/`actual`
   seams, visibility/openness widenings, the property-accessor layer, …). It is a living document. Any
   new deviation you introduce MUST be recorded there.
2. **The test suite is the oracle.** The port's guardrail is JTS's *own unmodified Java test suite*,
   compiled against the Kotlin and run as `jvmTest`. When green, the port behaves like upstream. The
   new JTS release ships new/changed Java tests too — those become the new guardrail. **Green
   `jvmTest` on the new upstream test suite is the definition of a correct re-base.**

## Prerequisites — get these in front of you first

- **This repo** (the port), on a clean branch off `main`.
- **The OLD upstream JTS Java sources** — the exact release the port currently sits on. Read
  `jtsBaseVersion` in [`build.gradle.kts`](../../../build.gradle.kts) to know which (e.g. `1.20.0`).
- **The NEW upstream JTS Java sources** — the target release.

If you don't have the upstream sources, get them (git tags on `locationtech/jts`, or the release
source zips). You cannot re-base without diffing old→new.

## The workflow

Work in phases. Keep `jvmTest` green at every step you can — a re-base that compiles but regresses the
guardrail is not done.

### 1. Compute the upstream delta

Diff the OLD vs NEW upstream Java trees. This is the entire scope of work. Bucket every change:

- **Changed** existing files (method bodies, new methods, signature changes).
- **Added** files (new classes/algorithms/packages).
- **Removed** files.
- **Changed/added/removed tests** — track these separately; they redefine the guardrail count.

Also note upstream's own version-to-version release notes / CHANGES — they call out behavioral changes
and new features, which tells you where to look hardest.

### 2. Map each upstream change onto the port

For every non-test source change, decide **which port module** it lands in and **which deviations
apply**. See [`references/conversion-patterns.md`](references/conversion-patterns.md) for the mechanical
Java→Kotlin substitutions and the deviation catalog. In short:

- **Module routing.** Core geometry/algorithms → `kts-core`. IO readers → the matching `kts-io-*`
  satellite (`wkt`/`gml`/`kml`/`geojson`/`files`). A brand-new upstream package needs a routing
  decision consistent with §1 of the compatibility doc. A new `awt`-like JVM-desktop-only package is
  probably dropped (record it), not ported.
- **Faithful body, deviations re-applied.** Port the Java to Kotlin keeping the body byte-faithful to
  upstream *except* where a documented deviation applies (no `java.io.Serializable`, no `Cloneable`,
  `java.util.TreeMap`→the port's `util.TreeMap`, `System.arraycopy`→`copyInto`, `EnumSet`→`Set`,
  `expect`/`actual` for anything JVM-only that must live in `commonMain`, etc.). The compatibility doc
  §2–§21 is the checklist of what to watch for.
- **Existing seams may already cover it.** If upstream changed a file the port already rewrote (e.g. a
  reader converted to xmlutil, or a class behind an `expect`/`actual`), you re-apply the change *inside
  the port's existing structure*, not by reverting to upstream's Java shape.

### 3. Update the ported test suite

Drop the NEW upstream Java tests into the correct module `jvmTest` source sets (mirroring how the
current ones are laid out). Re-apply the small, documented test-side edits the port needs — e.g.
removed `SAXException`/`Serializable` tests, the `GMLExtensions`/`KMLWriterExtensions`/
`GeoJsonExtensions` routing for the `java.io` overloads, WKT `read(Reader)` → `WKTReaderExtensions`.
The new guardrail count is whatever the new suite totals (the current baseline is recorded in the
compatibility doc §17/§18 — update it).

### 4. Make the guardrail green

`./gradlew jvmTest` (there is **no** `test` task — this is KMP). Iterate until the full ported Java
suite passes. A failing assertion means the port diverged from upstream behavior — fix the Kotlin, not
the test (the test is the oracle). See [`references/build-and-test.md`](references/build-and-test.md).

### 5. Validate multiplatform

Compile the whole target matrix and run the `commonTest` runtime suite on the non-JVM targets the host
can run (`macosArm64`, `jsNode`, `wasmJsNode`). Any new pure-Kotlin reimplementation or `expect`/
`actual` seam you added needs `commonTest` coverage (that's what proves off-JVM parity). Commands in
[`references/build-and-test.md`](references/build-and-test.md).

### 6. Docs: KDoc, compatibility doc, Dokka

- Port doc comments as KDoc, not Javadoc — the mechanical rules are in
  [`references/conversion-patterns.md`](references/conversion-patterns.md). `failOnWarning` is ENABLED,
  so any unresolvable KDoc link fails the Dokka build; run the aggregated site to catch it.
- Record every new deviation in [`doc/kotlin/KOTLIN-MP-COMPATIBILITY.md`](../../../doc/kotlin/KOTLIN-MP-COMPATIBILITY.md)
  and bump the guardrail test counts it cites.

### 7. Release

Re-basing onto a new upstream release bumps **`jtsBaseVersion`** (e.g. `1.20.0` → `1.21.0`) and resets
`portRevision` to `0` in [`build.gradle.kts`](../../../build.gradle.kts). Then follow the full release
checklist — Maven Central publish, git tag, GitHub Release with the Dokka docs asset, README/CHANGELOG,
and the upstream "Show and tell" announcement — in [`references/release-checklist.md`](references/release-checklist.md).

## Guiding principles

- **Faithfulness over cleverness.** The port's value is that it behaves exactly like JTS. Keep ported
  bodies as close to upstream Java as the language allows; deviate only where documented and necessary.
- **The delta is the scope.** Don't rewrite what upstream didn't touch. Don't re-litigate the port's
  existing deviations — they were hard-won decisions, all recorded.
- **The Java test suite is ground truth.** Keep it unmodified except for the documented mechanical
  edits, and make it green. That is the whole safety story.
