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
