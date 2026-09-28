import com.android.build.api.dsl.ApkSigningConfig
import com.android.build.api.dsl.ApplicationExtension
import java.util.Properties

// Convention for the :app module. Shared Android configuration lives in IfcAndroid.kt; this adds the
// target SDK, the git-derived versionCode/versionName (GitBuildVersion.kt, docs/ARCHITECTURE.md §7
// "Versioning"), the JUnit4 + Robolectric test stack, release signing (docs/release-builds.md,
// docs/security-and-privacy.md §8.2) and R8 on the release build (docs/release-builds.md "R8 and resource
// shrinking").

plugins {
    id("com.android.application")
    id("com.diffplug.spotless")
}

val buildVersion = resolveBuildVersion()

extensions.configure<ApplicationExtension> {
    configureIfcAndroid(this)

    defaultConfig {
        targetSdk = libs.findVersion("targetSdk").get().requiredVersion.toInt()

        // versionCode = commit count of HEAD; versionName = VERSION_NAME, the plain SemVer in
        // gradle.properties (what the Play listing shows). The count is a valid Play versionCode because
        // main only ever moves by fast-forward (docs/WORKFLOW.md §1), so it never decreases. Debug builds
        // add "+$count.$shortSha[.dirty]" below. Falls back to VERSION_BUILD / 1 with a warning when git
        // cannot answer — see GitBuildVersion.kt.
        versionName = buildVersion.versionName
        versionCode = buildVersion.versionCode.toInt()
    }

    // The release signing config never needs a secret to build (security-and-privacy.md §8.2): it is
    // read from keystore.properties at the repo root (gitignored) or, failing that, from environment
    // variables, and falls back to the debug key with a warning when neither is present. This keeps
    // `:app:assembleRelease` installable everywhere — forks, CI, and a machine with no keystore — which
    // an *unsigned* APK would not be (Android refuses to install one).
    val releaseSigningConfig = releaseSigningConfig(this)

    buildTypes {
        debug {
            // SemVer build metadata on debug installs only (0.1.0+46.576c910[.dirty]): it identifies the
            // exact commit in a feedback email, and a release build never carries it.
            versionNameSuffix = buildVersion.debugSuffix
        }
        release {
            signingConfig = releaseSigningConfig
            // AGP 9 DSL property confirmed from the gradle-api-9.3.3-sources.jar KDoc
            // (com.android.build.api.dsl.ApplicationBuildType.isProfileable): "Enabling this option
            // will declare the application as profileable in the AndroidManifest." This lets the owner
            // attach Android Studio's CPU/memory profiler to the exact APK they're judging for jank
            // (the debug build is not representative: debuggable=true plus Compose's debug
            // instrumentation both cost real frame time). Deliberately NOT isDebuggable — AGP warns if
            // a build type is both.
            isProfileable = true

            // R8 (ROADMAP M8 T2) through AGP 9's `optimization {}` DSL. Verified in the pinned AGP 9.3.3
            // (gradle-api and gradle sources): `Optimization.enable` is stable (not @Incubating), and
            // OptimizationDslInfoImpl turns it into *both* code shrinking (what isMinifyEnabled did) and
            // resource shrinking (isShrinkResources), so neither legacy flag is set as well.
            // `keepRules.includeDefault` defaults to true, which adds proguard-android-optimize.txt. The
            // app's own rules live in the `keepRules` source directory, src/main/keepRules/*.keep (the
            // replacement for the deprecated `keepRules.files` / `proguardFiles`); every library's
            // consumer rules are still merged in. See docs/release-builds.md "R8 and resource shrinking".
            optimization {
                enable = true
            }
        }
    }
}

/**
 * Resolves the release [ApkSigningConfig], preferring `keystore.properties` at the repo root (already
 * gitignored — see `.gitignore` and docs/security-and-privacy.md §8.2) over the `YEARAL_RELEASE_*`
 * environment variables, and falling back to the debug signing config (with a one-line configuration-time
 * warning, never a secret) when neither source is complete. No password is ever written to Gradle output
 * either way. See docs/release-builds.md for the runbook.
 */
fun Project.releaseSigningConfig(extension: ApplicationExtension): ApkSigningConfig {
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val (storeFilePath, storePassword, keyAliasValue, keyPasswordValue) = if (keystorePropertiesFile.isFile) {
        val properties = Properties().apply { keystorePropertiesFile.inputStream().use { load(it) } }
        listOf("storeFile", "storePassword", "keyAlias", "keyPassword").map { properties.getProperty(it) }
    } else {
        listOf(
            "YEARAL_RELEASE_STORE_FILE",
            "YEARAL_RELEASE_STORE_PASSWORD",
            "YEARAL_RELEASE_KEY_ALIAS",
            "YEARAL_RELEASE_KEY_PASSWORD",
        ).map { providers.environmentVariable(it).orNull }
    }

    if (listOf(storeFilePath, storePassword, keyAliasValue, keyPasswordValue).any { it.isNullOrBlank() }) {
        logger.warn(
            "No release keystore configured (neither keystore.properties nor YEARAL_RELEASE_* env vars " +
                "are complete) — :app:assembleRelease will be signed with the DEBUG key. " +
                "This build must NEVER be uploaded to Play. See docs/release-builds.md.",
        )
        return extension.signingConfigs.getByName("debug")
    }

    return extension.signingConfigs.create("release") {
        storeFile = rootProject.file(storeFilePath!!)
        this.storePassword = storePassword
        keyAlias = keyAliasValue
        keyPassword = keyPasswordValue
    }
}

dependencies {
    "testImplementation"(libs.findLibrary("junit4").get())
    "testImplementation"(libs.findLibrary("kotest-assertions-core").get())
    "testImplementation"(libs.findLibrary("robolectric").get())
    "testImplementation"(libs.findLibrary("androidx-test-core").get())
    "testImplementation"(libs.findLibrary("androidx-test-ext-junit").get())
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint(libs.findVersion("ktlint").get().requiredVersion)
    }
}
