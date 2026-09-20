import com.android.build.api.dsl.ApkSigningConfig
import com.android.build.api.dsl.ApplicationExtension
import java.util.Properties

// Convention for the :app module. Shared Android configuration lives in IfcAndroid.kt; this adds the
// target SDK, the SemVer-derived versionCode (docs/ARCHITECTURE.md §7 "Versioning"), the JUnit4 +
// Robolectric test stack, and release signing (docs/release-builds.md, docs/security-and-privacy.md §8.2).

plugins {
    id("com.android.application")
    id("com.diffplug.spotless")
}

extensions.configure<ApplicationExtension> {
    configureIfcAndroid(this)

    defaultConfig {
        targetSdk = libs.findVersion("targetSdk").get().requiredVersion.toInt()

        // VERSION_NAME lives in gradle.properties; VERSION_BUILD may be passed by a release job.
        val versionName = providers.gradleProperty("VERSION_NAME").get()
        val build = providers.gradleProperty("VERSION_BUILD").orNull?.toInt() ?: 0
        val (major, minor, patch) = versionName.split('.').map { it.toInt() }
        require(minor < 100 && patch < 100 && build < 100) {
            "VERSION_NAME $versionName / VERSION_BUILD $build overflow the versionCode scheme"
        }
        this.versionName = versionName
        versionCode = major * 1_000_000 + minor * 10_000 + patch * 100 + build
    }

    // The release signing config never needs a secret to build (security-and-privacy.md §8.2): it is
    // read from keystore.properties at the repo root (gitignored) or, failing that, from environment
    // variables, and falls back to the debug key with a warning when neither is present. This keeps
    // `:app:assembleRelease` installable everywhere — forks, CI, and a machine with no keystore — which
    // an *unsigned* APK would not be (Android refuses to install one).
    val releaseSigningConfig = releaseSigningConfig(this)

    buildTypes {
        release {
            signingConfig = releaseSigningConfig
            // AGP 9 DSL property confirmed from the gradle-api-9.3.3-sources.jar KDoc
            // (com.android.build.api.dsl.ApplicationBuildType.isProfileable): "Enabling this option
            // will declare the application as profileable in the AndroidManifest." This lets the owner
            // attach Android Studio's CPU/memory profiler to the exact APK they're judging for jank
            // (the debug build is not representative: debuggable=true plus Compose's debug
            // instrumentation both cost real frame time). Deliberately NOT isDebuggable — AGP warns if
            // a build type is both. isMinifyEnabled is left at its current default (off): R8 needs its
            // own keep rules for Hilt/Room3/kotlinx-serialization first (ROADMAP M8 T2); enabling it
            // here without them risks a release-only crash, exactly what the owner must not hit while
            // judging performance.
            isProfileable = true
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
