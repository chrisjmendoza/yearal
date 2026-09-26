// Convention for :feature:* modules: an Android library with Compose and Hilt, the standard core
// dependencies, and the module-boundary check from docs/ARCHITECTURE.md §2 "Dependency direction":
// a feature may depend on :core:domain, :core:designsystem, :core:navigation, :core:calendar,
// :core:holidays and :core:testing, never on :core:data or on another feature (CLAUDE.md rule 10).
// The check itself is `ifc.android.library`'s (applied below): every Android library module is
// covered, not only features, so it is not repeated here.

plugins {
    id("ifc.android.library")
    id("ifc.android.compose")
    id("ifc.hilt")
}

dependencies {
    "implementation"(project(":core:calendar"))
    "implementation"(project(":core:designsystem"))
    "implementation"(project(":core:domain"))
    "implementation"(project(":core:navigation"))

    "implementation"(libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
    "implementation"(libs.findLibrary("androidx-hilt-lifecycle-viewmodel-compose").get())
    "implementation"(libs.findLibrary("kotlinx-coroutines-android").get())

    "testImplementation"(project(":core:testing"))
    "testImplementation"(libs.findLibrary("kotlinx-coroutines-test").get())
    "testImplementation"(libs.findLibrary("turbine").get())
}
