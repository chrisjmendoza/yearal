plugins {
    id("ifc.android.library")
    id("ifc.kotlin.serialization")
    id("ifc.hilt")
    id("ifc.room")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

android {
    namespace = "io.github.chrisjmendoza.yearal.core.data"

    // MigrationTestHelper (ROADMAP M8 T5) loads the schema as an Android asset, keyed by
    // YearalDatabase's qualified name (androidx.room3.testing.AndroidMigrationTestHelper.loadSchema).
    // This points ONLY at the frozen copy under src/test/resources/frozen-schemas -- never at
    // core/data/schemas/ directly -- because Room's own copyRoomSchemas task overwrites
    // core/data/schemas/.../1.json to match the current entities whenever the version number is
    // unchanged, and (by its own KDoc) "does not correctly declare outputs", so nothing here could
    // order a test against it reliably; a same-version entity edit could race the asset merge and
    // pass. The frozen copy is never touched by any Gradle task, so SchemaFreezeMigrationTest and
    // LegacyDatabaseOpenTest are deterministic regardless of task ordering. Refreshing it is a
    // deliberate, manual step -- see SchemaExportGuardTest's KDoc for when that is allowed.
    sourceSets {
        getByName("test") {
            assets.directories.add("$projectDir/src/test/resources/frozen-schemas")
        }
    }
}

// SchemaExportGuardTest (ROADMAP M8 T5) compares the live exported core/data/schemas/ against the
// frozen copy above; both paths are handed over as system properties, the same way SpecVectorsTest
// reads docs/calendar-spec.md (core/calendar/build.gradle.kts), so the test does not guess the JVM's
// working directory. copyRoomSchemas does not correctly declare its own task outputs (see the KDoc
// above), so an explicit `dependsOn` is the only reliable way to guarantee the live directory reflects
// the just-compiled entities before this test reads it -- without it, the comparison could run before
// or after the copy depending on unrelated scheduling, which is exactly the non-determinism this whole
// setup exists to avoid.
tasks.withType<Test>().configureEach {
    dependsOn("copyRoomSchemas")
    val liveSchemas = layout.projectDirectory.dir("schemas")
    val frozenSchemas = layout.projectDirectory.dir("src/test/resources/frozen-schemas")
    inputs.dir(liveSchemas).withPropertyName("roomSchemas")
    inputs.dir(frozenSchemas).withPropertyName("frozenRoomSchemas")
    systemProperty("ifc.roomSchemasDir", liveSchemas.asFile.absolutePath)
    systemProperty("ifc.frozenRoomSchemasDir", frozenSchemas.asFile.absolutePath)
}

dependencies {
    api(project(":core:domain"))
    implementation(libs.findLibrary("androidx-datastore").get())
    implementation(libs.findLibrary("kotlinx-coroutines-core").get())
    implementation(libs.findLibrary("kotlinx-serialization-json").get())

    testImplementation(project(":core:testing"))
    testImplementation(libs.findLibrary("kotlinx-coroutines-test").get())
    testImplementation(libs.findLibrary("turbine").get())
    testImplementation(libs.findLibrary("androidx-room3-testing").get())
}
