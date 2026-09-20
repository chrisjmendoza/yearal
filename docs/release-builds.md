# Release builds

Status: **current as of M2 T11** (2026-09-19). Owning doc for how a release build is produced and signed
day to day. The policy behind these choices — why the upload key stays offline, why CI never holds it —
is [security-and-privacy.md](security-and-privacy.md) §8.2 and [ARCHITECTURE.md](ARCHITECTURE.md) §7
("Signing"); this doc is the runbook.

## Why you'd want a release build at all

A debug APK (`:app:assembleDebug`) is `debuggable=true` and carries Compose's debug instrumentation
(extra recomposition tracking, no R8). ART runs unoptimized code paths for a debuggable app, so a debug
build's frame timing is not representative of what a user gets. A release build removes both, so it's the
build to judge real UI performance on — install it on a phone, not the debug build.

**What a release build does and does not tell you about performance:** `debuggable` is off and Compose's
debug instrumentation is gone, which is most of what makes debug builds feel slower than they need to.
It does **not** yet include R8 shrinking/optimization or a baseline profile — both are
[ROADMAP.md](ROADMAP.md) M8 T2, deliberately deferred (see "Why isMinifyEnabled is untouched" below) — so
startup time and first-scroll jank can still improve further once those land.

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
(`adb uninstall io.github.chrisjmendoza.yearal`) and install again.

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

## Why `isMinifyEnabled` is untouched

The `release` build type does **not** turn on R8 (`isMinifyEnabled`) here. R8 needs its own keep rules
for Hilt, Room 3, and kotlinx.serialization before it's safe to enable — that work, plus the baseline
profile, is [ROADMAP.md](ROADMAP.md) M8 T2. Enabling R8 without those keep rules risks a release-only
crash (reflection-based code Hilt/Room/serialization rely on gets stripped), which is exactly the kind of
surprise the owner must not hit while judging whether the app feels fast. `isProfileable = true` is on
instead (not `isDebuggable`), so Android Studio's CPU/memory profiler can attach to the exact APK being
judged.
