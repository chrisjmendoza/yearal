import com.android.build.api.dsl.CommonExtension
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import io.github.takahirom.roborazzi.RoborazziExtension

// Convention for Android modules that contain Compose UI. Applies the Compose compiler plugin, turns
// on the build feature, and adds the BOM-managed Compose dependencies plus the Roborazzi screenshot
// stack (docs/ARCHITECTURE.md §1 "UI and AndroidX" and §6 "Screenshots"). Apply after
// ifc.android.library or ifc.android.application.

plugins {
    id("org.jetbrains.kotlin.plugin.compose")
    id("io.github.takahirom.roborazzi")
}

extensions.configure<CommonExtension> {
    // `buildFeatures.compose` written out in full: inside a precompiled script the bare name `compose`
    // resolves to the Kotlin Compose plugin accessor instead.
    buildFeatures.compose = true
}

// ROADMAP R6 / M2 T10: Roborazzi's default `outputDir` is `build/outputs/roborazzi`, which is
// gitignored, so `recordRoborazziDebug` would write goldens nobody could commit and
// `verifyRoborazziDebug` would always run against an empty directory. Every Compose module writes
// and reads its goldens from a tracked, per-module source directory instead (not covered by any
// `.gitignore` rule — `build/` is, `src/test/screenshots/` is not).
@OptIn(ExperimentalRoborazziApi::class)
extensions.configure<RoborazziExtension> {
    outputDir.set(layout.projectDirectory.dir("src/test/screenshots"))
    // Compare/verify writes `*_compare.png` / `*_actual.png` diff artifacts next to its input by
    // default, which would be the tracked directory above once `outputDir` moves there. Point those
    // at an explicit build/ subdirectory (still Roborazzi's own default path shape) so a local
    // `compareRoborazziDebug` — or `check`, see below — never dirties the golden directory.
    compare {
        outputDir.set(layout.buildDirectory.dir("outputs/roborazzi"))
    }
}

dependencies {
    val bom = libs.findLibrary("androidx-compose-bom").get()
    "implementation"(platform(bom))
    "implementation"(libs.findLibrary("androidx-compose-ui").get())
    "implementation"(libs.findLibrary("androidx-compose-ui-tooling-preview").get())
    "implementation"(libs.findLibrary("androidx-compose-material3").get())
    "implementation"(libs.findLibrary("androidx-lifecycle-runtime-compose").get())
    "debugImplementation"(libs.findLibrary("androidx-compose-ui-tooling").get())
    "debugImplementation"(libs.findLibrary("androidx-compose-ui-test-manifest").get())

    "testImplementation"(platform(bom))
    "testImplementation"(libs.findLibrary("androidx-compose-ui-test-junit4").get())
    "testImplementation"(libs.findLibrary("roborazzi").get())
    "testImplementation"(libs.findLibrary("roborazzi-compose").get())
    "testImplementation"(libs.findLibrary("roborazzi-junit-rule").get())
}
