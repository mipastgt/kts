# Release checklist (kts)

The end-to-end steps to ship a version. For a **re-base onto a new upstream JTS release**, bump
`jtsBaseVersion`; for a fix to the port itself, bump `portRevision`.

## 1. Version

In [`build.gradle.kts`](../../../../build.gradle.kts):

```kotlin
val jtsBaseVersion = "1.20.0"   // ← the upstream JTS release this port is derived from
val portRevision = 0            // ← revision of THIS port against that base
```

Version = `<jtsBaseVersion>.<portRevision>`, e.g. `1.20.0.0`. Both Maven and Gradle order the
4-component versions correctly (`1.20.0.1 > 1.20.0.0`).

- **Re-base (new JTS):** set `jtsBaseVersion` to the new upstream version, reset `portRevision = 0`.
  → e.g. `1.21.0.0`.
- **Port fix / follow-up:** bump `portRevision` only. → e.g. `1.20.0.1`.

Update the module title/description strings in `build.gradle.kts` and the version references in
`README.md` if the JTS base changed.

## 2. Publish to Maven Central

Publishing is via the **vanniktech maven-publish** plugin → Central Portal (`central.sonatype.com`).
Credentials live OUTSIDE the repo in `~/.gradle/gradle.properties` (never commit them):
`mavenCentralUsername`/`mavenCentralPassword` (Central Portal token), `signingInMemoryKey` (armored
GPG secret key as one `\n`-escaped line), `signingInMemoryKeyPassword`, `signingInMemoryKeyId`.

- Dry run first: `./gradlew clean publishToMavenLocal` (no signing/creds needed) — confirms every
  module produces jar + `.module` + `.pom` + `-sources.jar` + `-javadoc.jar`.
- Real upload: `./gradlew publishToMavenCentral`. This **stages** a deployment (USER_MANAGED) — it is
  **not public** until you review and click **Publish** at central.sonatype.com/publishing/deployments
  (or Drop to cancel). Once published it is permanent/immutable.
- **Do the publish on macOS** — a single `publishToMavenCentral` cross-compiles ALL targets incl.
  Apple native, which a Linux runner can't. (The maintainer has chosen manual local publish over CI
  for this reason — don't re-suggest CI unless asked.)
- **GPG keyserver gotcha:** Central validates the signing key by fingerprint. Having it on
  keyserver.ubuntu.com is NOT sufficient — you must also
  `gpg --keyserver keys.openpgp.org --send-keys <FPR>` (serves key material by fingerprint
  immediately). Verify: `curl -s "https://keys.openpgp.org/pks/lookup?op=get&search=0x<FPR>"`.

## 3. CHANGELOG & git tag

- Add a `CHANGELOG.md` section for the version (Keep a Changelog format: Added / Changed / Removed;
  for a re-base, summarize what upstream JTS changed + any new port deviations).
- Tag `vX.Y.Z.N` (matching the version) and push. **The tag push must come from an authenticated
  client** (GitHub Desktop or a terminal where `git`/`gh` is authed) — Claude's sandboxed shell has no
  GitHub credentials.

## 4. GitHub Release

Create a Release from the tag with the changelog as notes, and attach the aggregated Dokka docs.

```bash
# notes = the CHANGELOG section for this version
awk '/^## \[X\.Y\.Z\.N\]/{f=1} f&&/^## \[/&&!/X\.Y\.Z\.N/{exit} f' CHANGELOG.md > /tmp/notes.md
# docs asset
./gradlew :dokkaGenerateHtml
( cd build/dokka/html && zip -rq /tmp/kts-X.Y.Z.N-docs.zip . )

gh release create vX.Y.Z.N --repo mipastgt/kts \
  --title "vX.Y.Z.N — Kotlin Multiplatform port of JTS <jtsBaseVersion>" \
  --notes-file /tmp/notes.md --latest --verify-tag
gh release upload vX.Y.Z.N '/tmp/kts-X.Y.Z.N-docs.zip#API documentation (Dokka HTML)' --repo mipastgt/kts
```

**`gh` auth is NOT visible to Claude's spawned shells** (sandboxed; no keychain/`~/.config/gh`
access, even with the sandbox disabled). Claude can build the notes + docs zip and verify results via
the public API, but the `gh release create`/`upload` calls must be **run by the user**. Verify after:

```bash
curl -s https://api.github.com/repos/mipastgt/kts/releases/latest
```

## 5. Verify on Maven Central

Once indexed (can take a bit after the portal Publish), all modules should 200:

```bash
base="https://repo1.maven.org/maven2/de/mpmediasoft/kts"
for a in kts-core kts-io-wkt kts-io-gml kts-io-kml kts-io-geojson kts-io-files; do
  curl -s -o /dev/null -w "%{http_code}  $a\n" "$base/$a/X.Y.Z.N/$a-X.Y.Z.N.pom"
done
```

## 6. Announce (optional, for notable releases)

The port is announced to the JTS community in the upstream **Show and tell** discussion category
(NOT Announcements — that's maintainer-only): https://github.com/locationtech/jts/discussions. Keep the
tone deferential (the port stands on upstream's work). The kts repo itself is **Issues-only** by the
maintainer's choice — do not enable/suggest GitHub Discussions on it.
