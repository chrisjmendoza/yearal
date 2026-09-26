package io.github.chrisjmendoza.yearal.core.data.event

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import java.io.File

/**
 * Guards the other half of ROADMAP M8 T5's premise: that schema v1 really is still the *only* frozen
 * contract, and that the copy [SchemaFreezeMigrationTest] and [LegacyDatabaseOpenTest] actually read
 * (`core/data/src/test/resources/frozen-schemas/`) has not silently drifted from what Room's KSP
 * processor currently exports (`core/data/schemas/`, `ifc.room`'s `schemaDirectory`).
 *
 * [YearalDatabase]'s `@Database(version = ...)` annotation is [androidx.room3.Database]-`BINARY`-
 * retained (not visible to runtime reflection), so this test cannot read it back directly; instead it
 * reads the one artifact that annotation actually controls — the exported schema directory Room writes
 * to on every compile.
 *
 * Room never deletes an old export when the version bumps; it adds a new `<version>.json` alongside it
 * (`@Database`'s own KDoc: "it is a good practice to have version history of your schema"). So the
 * moment someone bumps [YearalDatabase]'s version without following through, `core/data/schemas/`
 * gains a `2.json` next to `1.json`, and the first test below fails immediately.
 *
 * The third test below catches the other mistake: an entity edited **without** a version bump. Room's
 * `copyRoomSchemas` task overwrites `core/data/schemas/.../1.json` in place to match such an edit (see
 * [SchemaFreezeMigrationTest]'s KDoc), so the live export alone cannot tell "still frozen" from
 * "silently drifted". Comparing it against the frozen copy — which nothing but a person, deliberately,
 * ever changes — can.
 *
 * **Refreshing the frozen copy** is only ever correct in the same change as: a version bump on
 * [YearalDatabase], the new version's own exported JSON, a real
 * [Migration][androidx.room3.migration.Migration], and a migration test proving it (CLAUDE.md rule 12
 * — the frozen copy is an input, exactly like `core/data/schemas/` itself, never edited to match the
 * code on its own).
 */
public class SchemaExportGuardTest {
    @Test
    public fun `schema v1 is still the only exported version`() {
        val entries =
            liveSchemaDir()
                .listFiles()
                ?.map { it.name }
                ?.sorted()
                .orEmpty()

        check(entries == listOf(FROZEN_SCHEMA_FILE_NAME)) {
            "core/data/schemas/$DATABASE_QUALIFIED_NAME/ now contains $entries, not just " +
                "[$FROZEN_SCHEMA_FILE_NAME]. Schema v1 is 1.0's frozen contract (CLAUDE.md rule 12): " +
                "a real schema change needs a version bump on YearalDatabase, the new exported JSON " +
                "committed alongside this one, a Migration, and a migration test here " +
                "(see SchemaFreezeMigrationTest and LegacyDatabaseOpenTest) — not editing this guard."
        }
    }

    @Test
    public fun `the frozen JSON's own declared version is 1`() {
        val schemaJson = File(frozenSchemaDir(), FROZEN_SCHEMA_FILE_NAME)
        val declaredVersion =
            Json
                .parseToJsonElement(schemaJson.readText())
                .jsonObject["database"]
                ?.jsonObject
                ?.get("version")
                ?.jsonPrimitive
                ?.int

        declaredVersion shouldBe 1
    }

    @Test
    public fun `the exported schema still matches the frozen copy tests are validated against`() {
        val live = File(liveSchemaDir(), FROZEN_SCHEMA_FILE_NAME).readBytes()
        val frozen = File(frozenSchemaDir(), FROZEN_SCHEMA_FILE_NAME).readBytes()

        check(live.contentEquals(frozen)) {
            "core/data/schemas/$DATABASE_QUALIFIED_NAME/1.json no longer matches the frozen copy at " +
                "core/data/src/test/resources/frozen-schemas/$DATABASE_QUALIFIED_NAME/1.json — the " +
                "exported schema changed without a version bump. A schema change needs version 2, a " +
                "new 2.json, a Migration, a migration test, and only then a refreshed frozen copy."
        }
    }

    /** The live directory Room's KSP processor currently exports to (`core/data/schemas/...`). */
    private fun liveSchemaDir(): File = schemaDirFromProperty(LIVE_SCHEMAS_DIR_PROPERTY, "core/data/schemas")

    /**
     * The frozen copy [SchemaFreezeMigrationTest] and [LegacyDatabaseOpenTest] actually load as a test
     * asset (`core/data/src/test/resources/frozen-schemas/...`) — never rewritten by any Gradle task.
     */
    private fun frozenSchemaDir(): File =
        schemaDirFromProperty(FROZEN_SCHEMAS_DIR_PROPERTY, "core/data/src/test/resources/frozen-schemas")

    private fun schemaDirFromProperty(
        systemProperty: String,
        describedAs: String,
    ): File {
        val root =
            checkNotNull(System.getProperty(systemProperty)) {
                "System property '$systemProperty' is not set. Run the tests through Gradle " +
                    "(:core:data:test), or pass -D$systemProperty=<path to $describedAs>."
            }
        val dir = File(root, DATABASE_QUALIFIED_NAME)
        check(dir.isDirectory) { "Expected a schema directory at $dir; $describedAs has moved or been renamed." }
        return dir
    }

    private companion object {
        const val LIVE_SCHEMAS_DIR_PROPERTY = "ifc.roomSchemasDir"
        const val FROZEN_SCHEMAS_DIR_PROPERTY = "ifc.frozenRoomSchemasDir"
        const val DATABASE_QUALIFIED_NAME = "io.github.chrisjmendoza.yearal.core.data.event.YearalDatabase"
        const val FROZEN_SCHEMA_FILE_NAME = "1.json"
    }
}
