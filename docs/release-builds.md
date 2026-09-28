# Release builds

Status: **current as of M8 T2** (2026-09-26: R8 on, git-derived build numbers). Owning doc for how a
release build is produced, numbered, shrunk and signed day to day. The policy behind these choices — why the upload key stays offline, why CI never holds it —
is [security-and-privacy.md](security-and-privacy.md) §8.2 and [ARCHITECTURE.md](ARCHITECTURE.md) §7
("Signing"); this doc is the runbook.

## Why you'd want a release build at all

A debug APK (`:app:assembleDebug`) is `debuggable=true` and carries Compose's debug instrumentation
(extra recomposition tracking, no R8). ART runs unoptimized code paths for a debuggable app, so a debug
build's frame timing is not representative of what a user gets. A release build removes both and is
shrunk and optimized by R8, so it's the build to judge real UI performance on — install it on a phone, not
the debug build.

**What a release build does and does not tell you about performance:** `debuggable` is off, Compose's
debug instrumentation is gone and R8 has shrunk and optimized the code (see "R8 and resource shrinking"
below). It does **not** yet include a baseline profile — the other half of [ROADMAP.md](ROADMAP.md) M8 T2,
which needs a managed device to generate — so startup time and first-scroll jank can still improve once
that lands.

## Building and installing a release APK

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat :app:assembleRelease
adb install -r app\build\outputs\apk\release\app-release.apk
```

The debug and release APKs share the same `applicationId`
(`io.github.chrisjmendoza.yearal`), but they are normally signed with different keys (the debug key vs.
your upload key, or the fallback debug key — see below). Android refuses to install an APK over an
existing install with a different signing certificate. If `adb install -r` fails with
`INSTALL_FAILED_UPDATE_INCOMPATIBLE` or a signature-mismatch error, uninstall the existing app first
(`adb uninstall io.github.chrisjmendoza.yearal`) and install again. `INSTALL_FAILED_VERSION_DOWNGRADE`
means the installed build has a higher `versionCode` — see "Version numbers" below.

## Version numbers

Every build is stamped from git by `build-logic/convention/src/main/kotlin/GitBuildVersion.kt`
([ARCHITECTURE.md](ARCHITECTURE.md) §7 "Versioning" owns the rule):

| | Value | Example |
|---|---|---|
| `versionCode` | `git rev-list --count HEAD` | `45` |
| `versionName`, release | `VERSION_NAME` from `gradle.properties`, plain — what the Play listing shows | `0.1.0` |
| `versionName`, debug | the same, plus SemVer build metadata: `+` count `.` 7-character commit hash, plus `.dirty` if the worktree had uncommitted changes | `0.1.0+45.72dbfa1`, `0.1.0+45.72dbfa1.dirty` |

The `+` is SemVer's build-metadata marker, not arithmetic: it labels a build without changing the
version (`0.1.0+45` and `0.1.0+46` are both 0.1.0). The More screen's About row reads `Version 0.1.0
(45)` — the name up to any `+`, then the `versionCode` — in both build types, the usual Android
convention. The feedback email's subject and body carry the full `versionName`, so a report from a
debug install names the exact commit it was built from. Only builds of `main` are ever uploaded; `main` moves
only by fast-forward ([WORKFLOW.md](WORKFLOW.md) §1), so its commit count only goes up and is a valid
Play `versionCode`. A `local/*` branch build carries its own count, which can equal a different `main`
commit's — the hash in the name tells them apart. The build fails if the count ever passes 2,000,000,000
(Play's hard cap is 2,100,000,000).

**When git can't answer** — a source archive with no `.git`, no `git` on `PATH`, or a shallow clone (whose
count would be wrong) — the build still succeeds with a warning: the `versionCode` is the `VERSION_BUILD`
Gradle property if one is passed (`-PVERSION_BUILD=123`), else `1`, and the debug name ends in `.unknown`
(`0.1.0+0.unknown`). CI checks out with `fetch-depth: 0` so its builds are numbered properly.

**One-time downgrade (2026-09-26).** Builds made before this scheme were `0.1.0` with `versionCode`
`10000` (the old `major*1_000_000 + minor*10_000 + …` formula). The commit count is far lower, so Android
refuses to install a new build over an old one (`INSTALL_FAILED_VERSION_DOWNGRADE`; Android Studio offers to
uninstall). Uninstall the old build once — this loses that install's events and settings — or, while the
installed build is a debug build, `adb install -r -d` keeps the data. Nothing was ever uploaded to Play under
the old numbers, so no store listing is affected.

### Confirming which key signed an APK

`apksigner` ships in the Android SDK's `build-tools`:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\<version>\apksigner.bat" verify --print-certs `
    app\build\outputs\apk\release\app-release.apk
```

The certificate DN tells you which key signed it: `CN=Android Debug` is the shared Android debug key
(what you get with no release keystore configured — see below); anything else is whatever key
`keystore.properties` or the `YEARAL_RELEASE_*` environment variables point at.

## No keystore configured? The build still works

`:app:assembleRelease` **never fails** for lack of a keystore — a fork, a CI run, or a machine that hasn't
generated an upload key yet all still get an installable APK. When neither `keystore.properties` nor the
environment variables below are complete, the `release` build type falls back to the **debug** signing
config, and the build prints one line at configuration time:

```
No release keystore configured (neither keystore.properties nor YEARAL_RELEASE_* env vars are complete) —
:app:assembleRelease will be signed with the DEBUG key. This build must NEVER be uploaded to Play.
```

That fallback build is fine for judging performance on your own phone (`isDebuggable` is still off,
`isProfileable` is still on — see below) — it is **not** fine to upload anywhere. If you see that
warning, you're getting a debug-signed release build, not your own release build.

## Configuring your own release key

### 1. Generate the upload keystore (once)

Play App Signing means Google holds the app signing key; you generate and hold only an **upload key**,
which Play Console can reset if it's ever lost or leaked
([Play App Signing](https://support.google.com/googleplay/android-developer/answer/9842756?hl=en)).
Generate it with `keytool` (ships with the JDK):

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
keytool -genkeypair -v `
    -keystore "C:\path\outside\the\repo\yearal-upload.jks" `
    -alias yearal-upload `
    -keyalg RSA -keysize 2048 -validity 10000 `
    -storepass "<put a strong unique password here>" `
    -keypass "<put a strong unique password here>"
```

Use two independently strong, unique passwords (store and key can differ). **Immediately:**

- Save both passwords in your password manager. Never write them in a file inside this repo, a commit
  message, an issue, or a chat log.
- Keep the `.jks` file **outside** the repo and outside any cloud-synced project folder
  (security-and-privacy.md §8.2).
- Make one **offline encrypted backup** of the keystore file (e.g. an encrypted USB drive or your
  password manager's file-attachment vault). If you lose this file *and* haven't set up Play App Signing
  key recovery, you cannot ship an update under the same app identity.

### 2. Point the build at it: `keystore.properties`

Create `keystore.properties` at the **repo root** (already covered by `.gitignore` — do not remove that
line). It is never committed and never referenced from anywhere but this build:

```properties
# keystore.properties — repo root, gitignored. Never commit this file.
storeFile=C:\\path\\outside\\the\\repo\\yearal-upload.jks
storePassword=<the store password, from your password manager>
keyAlias=yearal-upload
keyPassword=<the key password, from your password manager>
```

`storeFile` may be an absolute path (recommended) or a path relative to the repo root. All four keys are
required; if any is missing, the build falls back to the debug key (above) rather than failing.

### Alternative: environment variables

If you'd rather not keep a properties file (e.g. a temporary shell for one build), set these instead —
they're only used when `keystore.properties` is absent:

| Variable | Meaning |
|---|---|
| `YEARAL_RELEASE_STORE_FILE` | Path to the `.jks`/`.keystore` file |
| `YEARAL_RELEASE_STORE_PASSWORD` | Store password |
| `YEARAL_RELEASE_KEY_ALIAS` | Key alias (e.g. `yearal-upload`) |
| `YEARAL_RELEASE_KEY_PASSWORD` | Key password |

```powershell
$env:YEARAL_RELEASE_STORE_FILE = "C:\path\outside\the\repo\yearal-upload.jks"
$env:YEARAL_RELEASE_STORE_PASSWORD = "<store password>"
$env:YEARAL_RELEASE_KEY_ALIAS = "yearal-upload"
$env:YEARAL_RELEASE_KEY_PASSWORD = "<key password>"
```

Passwords set this way live only in that shell's process environment; they are never printed by the
build and never written to a Gradle log.

### 3. Build and verify

```powershell
.\gradlew.bat :app:assembleRelease
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\<version>\apksigner.bat" verify --print-certs `
    app\build\outputs\apk\release\app-release.apk
```

The certificate DN should now be the one you gave `keytool` (your name/org for the `-dname`, or the
default `CN=<your name>` prompt answers), not `CN=Android Debug`.

## R8 and resource shrinking

**What is on.** The `release` build type (`ifc.android.application`) enables AGP 9's
`optimization { enable = true }`, which runs R8 in full mode for code shrinking, optimization and
obfuscation, and also shrinks unused resources — the new-DSL replacement for `isMinifyEnabled` +
`isShrinkResources`. The default `proguard-android-optimize.txt` rules are included
(`keepRules.includeDefault`, on by default). Debug builds are untouched. The release APK went from
16.9 MB to 8.2 MB when R8 was turned on (2026-09-26).

**Where the rules live.** Almost all of them come from the libraries themselves: every AAR/JAR ships its
own consumer rules (Hilt and Dagger, Room 3, kotlinx.serialization, Navigation 3, Glance, Compose,
DataStore, AndroidX Startup), Hilt generates keep rules for every `@HiltViewModel`, and R8 merges them all.
The app adds only verified gaps, in [`app/src/main/keepRules/yearal.keep`](../app/src/main/keepRules/yearal.keep)
— AGP 9's `keepRules` source directory (`src/<sourceSet>/keepRules/*.keep`), which replaces
`proguardFiles(...)`. Each rule there names the symbol it protects and how the gap was found. Today it
has two, both for WorkManager 2.7.1, which Glance pulls in transitively along with an old Room 2.2.5:
without them the release build crashed at launch (WorkManager's database) and the widgets would never
have rendered (WorkManager's input merger). **No `-dontwarn` rules:** R8 reports zero warnings and no
`missing_rules.txt` is produced. If a dependency bump brings a warning or a missing class, fix it with a
targeted keep rule or a dependency change, never a blanket `-dontwarn`.

**What was checked, and how** (2026-09-26). Statically: `app/build/outputs/mapping/release/mapping.txt`
and `usage.txt` show the Hilt components, `YearalDatabase_Impl` (kept by name with its constructor), every
`@Serializable` Navigation 3 key with its `Companion`/`INSTANCE` and `serializer()`, and the settings DTO
all surviving. Navigation 3 restores the back stack with `Class.forName(<saved class name>)` plus the
key's `serializer()`, and the settings file stores enums by name; both rely on the kotlinx.serialization
library rules, which cover them. lib-recur uses no reflection. At runtime: the release APK was installed on
an API 36 emulator and driven through every tab, the intro, the day card, the Year view, creating an IFC
monthly event with a reminder, the settings (changed, then read back after a force-stop), Holidays, Learn
and Privacy; the activity was recreated (night mode) and the process killed and restored from the back
stack (Month over Year came back); both widgets were placed and tapped; then 7,000 `monkey` events —
with no crash, `ClassNotFoundException`, `NoSuchMethodError` or `SerializationException`. A physical-device
pass is still the owner's step ([device-test-matrix.md](device-test-matrix.md) RB1).

**CI** builds `:app:assembleRelease` on every push ([ci.yml](../.github/workflows/ci.yml)), so an R8
error — a missing class, a malformed rule — fails the push that caused it. R8 warnings do not fail a build,
so read the `minifyReleaseWithR8` output after a dependency bump. Neither can see a class that is reached
only by reflection (both WorkManager gaps built cleanly and failed at runtime): after a dependency or R8
change, repeat the smoke test ([device-test-matrix.md](device-test-matrix.md) RB1). CI needs no secret:
with no keystore the release build is debug-signed (above).

**Diagnosing a release-only crash.** A release stack trace has obfuscated names (`qd.q`, `ms3.<init>`)
and an `r8-map-id-…` source-file marker. Decode it with the mapping file of **that exact build**, using
Android Studio's *Code → Analyze Stack Trace* (point it at the mapping file) or the `retrace` tool from the
SDK's command-line tools:

```powershell
retrace app\build\outputs\mapping\release\mapping.txt stacktrace.txt
```

Then look the symbol up in `usage.txt` (what R8 removed) and `seeds.txt` (what the rules kept); a
reflective lookup of something listed in `usage.txt` is the classic cause. `configuration.txt` shows every
merged rule and which library it came from. The mapping changes with every build, so **archive
`mapping.txt` with every build that leaves your machine** — an AAB uploaded to Play carries it
automatically ([security-and-privacy.md](security-and-privacy.md) "Crash visibility"), and
[ARCHITECTURE.md](ARCHITECTURE.md) §7's `release.yml` attaches it to the GitHub Release. To rule R8 in or
out quickly, compare with a debug build of the same commit.

`isProfileable = true` stays on (not `isDebuggable`), so Android Studio's CPU/memory profiler can attach to
the exact APK being judged.
