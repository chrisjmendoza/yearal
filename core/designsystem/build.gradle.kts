@file:OptIn(ExperimentalRoborazziApi::class)

import com.github.takahirom.roborazzi.ExperimentalRoborazziApi

plugins {
    id("ifc.android.library")
    id("ifc.android.compose")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

android {
    namespace = "io.github.chrisjmendoza.yearal.core.designsystem"
}

dependencies {
    api(project(":core:calendar"))
    // WeekdayDisplay drives the grid's header rows (docs/ARCHITECTURE.md §4); it is part of the
    // public signature of WeekdayHeaders and MonthGrid, hence `api`.
    api(project(":core:domain"))

    // docs/ROADMAP.md M3 T4 "Adaptive layouts": the window size class source behind
    // `currentWindowWidthClass()` (adaptive/WindowWidthClass.kt) — the same AndroidX artifact `:app`
    // already carries transitively through `androidx-compose-material3-adaptive-navigation-suite` for
    // its own bar/rail switch (docs/ARCHITECTURE.md §1 "Flagged as unverified" / §4). Apache 2.0,
    // ~40KB (androidx.compose.material3.adaptive:adaptive-android 1.3.0, BOM-managed); `implementation`
    // because only this module's own `adaptive` package needs it — feature modules read
    // `WindowWidthClass` instead of the library's own types.
    implementation(libs.findLibrary("androidx-compose-material3-adaptive").get())

    // ROADMAP R6 / M2 T10: the Compose preview scanner (see `roborazzi { }` below) that captures
    // every @Preview in this module as a Roborazzi screenshot without a hand-written test per preview.
    testImplementation(libs.findLibrary("roborazzi-compose-preview-scanner-support").get())
    testImplementation(libs.findLibrary("composable-preview-scanner-android").get())
}

// :core:designsystem is the first (and, for this task, only) module wired to Roborazzi's preview
// scanner (docs/ARCHITECTURE.md §6 "Goldens"; ROADMAP R6): it generates one Robolectric test per
// @Preview it finds under the package below, each captured with `captureRoboImage` at task-run time.
// `sdk`/`qualifiers` are fixed so the same previews render identically on every run — no device- or
// locale-default drift between local Windows runs and the Linux CI recorder (Windows/Linux pixel
// output still differs, which is why only CI records; see docs/ARCHITECTURE.md §6).
roborazzi {
    generateComposePreviewRobolectricTests {
        enable.set(true)
        packages.set(listOf("io.github.chrisjmendoza.yearal.core.designsystem"))
        robolectricConfig.set(
            mapOf(
                "sdk" to "[36]",
                "qualifiers" to "\"w360dp-h640dp-xhdpi\"",
            ),
        )
    }
}
