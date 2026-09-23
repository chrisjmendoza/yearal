plugins {
    id("ifc.android.feature")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

android {
    namespace = "io.github.chrisjmendoza.yearal.feature.converter"
}

dependencies {
    // Generated dates for the round-trip test (docs/ROADMAP.md M3 exit; docs/ARCHITECTURE.md §6). Kotest
    // is used as a library under JUnit4, as everywhere else; the catalog entry already exists.
    testImplementation(libs.findLibrary("kotest-property").get())

    // The result card's Share action (design-plan §4.6): material-icons-core has Share but no
    // swap/copy/open glyph (see ic_swap.xml, ic_copy.xml, ic_open.xml — the same reasoning as :app's
    // ic_convert.xml, docs/ARCHITECTURE.md §1 "Stack decisions").
    implementation(libs.findLibrary("androidx-compose-material-icons-core").get())
}
