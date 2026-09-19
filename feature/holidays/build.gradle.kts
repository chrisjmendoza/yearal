plugins {
    id("ifc.android.feature")
}

android {
    namespace = "io.github.chrisjmendoza.yearal.feature.holidays"
}

dependencies {
    // Browsing every bundled pack (not just the enabled ones) needs the raw catalogue, the same reason
    // :feature:calendar depends on this pure-JVM module (FEATURES H1, H2, H3, H5; ROADMAP M6 T2;
    // docs/ARCHITECTURE.md §3.3). :core:holidays exposes HolidaySet values, not a data layer, so the
    // feature boundary rule allows it.
    implementation(project(":core:holidays"))
    implementation(libs.findLibrary("androidx-compose-material-icons-core").get())
}
