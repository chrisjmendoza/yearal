import com.android.build.api.dsl.LibraryExtension

// Convention for Android library modules (:core:designsystem, :core:navigation, :core:data, ...).
// Shared Android configuration lives in IfcAndroid.kt; this adds the JUnit4 + Robolectric test
// stack that every Android module uses (docs/ARCHITECTURE.md §6) and ktlint via Spotless.

plugins {
    id("com.android.library")
    id("com.diffplug.spotless")
}

extensions.configure<LibraryExtension> {
    configureIfcAndroid(this)
}

// Module-boundary check (ROADMAP M0 T4), every Android library module: only `:app` wires a feature
// and `:core:data` (or `:core:scheduling`, `:core:devicecalendar`, `:widget`) together, so nothing that
// applies this plugin — `:core:designsystem`, `:core:navigation`, `:core:data` itself,
// `:core:scheduling`, `:widget`, and (through `ifc.android.feature`, which applies this plugin too)
// every `:feature:*` module — may depend on a feature or on `:core:data` (docs/ARCHITECTURE.md §2
// "Dependency direction"; `:widget` "may not depend on :feature:calendar" and "cannot depend on :app"
// are the same rule stated there for `:widget` specifically; CLAUDE.md rule 10).
forbidProjectDependencies(
    listOf(":feature:", ":core:data"),
    "an Android library module never depends on a feature or on :core:data; only :app wires those " +
        "together for Hilt (CLAUDE.md rule 10; docs/ARCHITECTURE.md §2 \"Dependency direction\").",
)

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
