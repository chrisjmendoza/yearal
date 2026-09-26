package io.github.chrisjmendoza.yearal.core.data.event

import android.content.Context
import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.chrisjmendoza.yearal.core.domain.event.EventCalendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The safety net for `docs/ARCHITECTURE.md` §3.2's frozen schema v1 (ROADMAP M8 T5): the compiled
 * [YearalDatabase] must still match the **frozen** copy of the exported schema at
 * `core/data/src/test/resources/frozen-schemas/io.github.chrisjmendoza.yearal.core.data.event.YearalDatabase/1.json`
 * bit for bit.
 *
 * **Why this is two checks, not one `runMigrationsAndValidate` call.** Room 3's
 * `MigrationTestHelper.runMigrationsAndValidate` (`androidx.room3:room3-testing`) does not compare
 * against the compiled entities at all — verified by decompiling `MigrateOpenDelegate.onValidateSchema`
 * (`room3-testing-android-3.0.3-sources.jar`): its "expected" side is built purely from the
 * [androidx.room3.migration.bundle.DatabaseBundle] loaded from the same schema asset [createDatabase]
 * already used, so calling it with the version [createDatabase] just built from is a self-consistency
 * check on the JSON, not a drift detector against the code. (This differs from Room 2's helper, which
 * opened the migrated database through the compiled `RoomDatabase`'s own generated validator.) So:
 * 1. [runMigrationsAndValidate] still runs, as a cheap proof the frozen JSON is internally well-formed —
 *    a real `TableInfo`/`FtsTableInfo`/`ViewInfo` comparison, just against itself.
 * 2. The actual drift detector is opening the same file with [YearalDatabase.create] — the **real**
 *    production builder — and forcing a query. Room's own identity-hash check in `onOpen`
 *    (`BaseRoomConnectionManager.checkIdentity`) throws *"Room cannot verify the data integrity. Looks
 *    like you've changed schema but forgot to update the version number."* the moment the compiled
 *    entities disagree with the frozen schema this file was built from — a renamed column, a widened
 *    type, a dropped index, a new entity, or an edit that leaves the version number at `1`. This is the
 *    same mechanism [LegacyDatabaseOpenTest] exercises; that test additionally proves old *data*
 *    survives the round trip, where this one only proves the *shape* does.
 *
 * **Why a frozen copy, and not `core/data/schemas/` directly:** Room's own `ifc.room` schema-export
 * task (`androidx.room3.gradle.RoomSchemaCopyTask`) overwrites the checked-in `core/data/schemas/.../
 * 1.json` to match the currently compiled entities whenever the version number is unchanged — by
 * design, so the exported file always mirrors "the current version's shape" — and it does so with no
 * declared task output (its own KDoc: "This task does not correctly declare outputs by design"), so no
 * test could depend on its timing reliably. Reading that live, mutable directory here would make step 2
 * above self-heal the moment `:core:data` rebuilds after a same-version edit, before the test ever ran
 * — and whether that happened before or after Gradle merged the test's assets was a race, not a
 * guarantee. The frozen copy under `src/test/resources/` is never written by any Gradle task, so this
 * test is deterministic regardless of build ordering. [SchemaExportGuardTest] is what makes sure the
 * frozen copy and the live export do not quietly drift apart from each other; CLAUDE.md rule 12 applies
 * to the frozen copy exactly as it does to `core/data/schemas/`: an input, never edited to match the
 * code. Refreshing it is a deliberate step that only happens alongside a version bump, a new exported
 * JSON, and a real [Migration][androidx.room3.migration.Migration] — see [SchemaExportGuardTest]'s KDoc.
 */
@RunWith(AndroidJUnit4::class)
public class SchemaFreezeMigrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dispatcher = Dispatchers.IO

    /**
     * The rule under test. Its `file` argument is [YearalDatabase.FILE_NAME]'s exact production path
     * (`context.getDatabasePath`) — deliberately the *same* file [YearalDatabase.create] will open
     * below, so that call sees exactly the database this rule built from the frozen schema, the same
     * pairing [LegacyDatabaseOpenTest] uses. Robolectric gives each test method its own sandboxed
     * `filesDir`, so this never collides with that test's run.
     */
    @get:Rule
    public val migrationHelper: MigrationTestHelper =
        MigrationTestHelper(
            instrumentation = instrumentation,
            file = context.getDatabasePath(YearalDatabase.FILE_NAME).also { it.delete() },
            driver = BundledSQLiteDriver(),
            databaseClass = YearalDatabase::class,
        )

    @Test
    public fun `schema v1 still matches the compiled entities, with no migration needed`(): Unit =
        runTest {
            // A version-1 database, built from the frozen schema, exactly as it would exist on a
            // device that installed 1.0.
            migrationHelper.createDatabase(version = 1).close()

            // Self-consistency check on the frozen JSON itself (see this class's KDoc for why this
            // alone cannot catch an entity changed without a version bump).
            migrationHelper.runMigrationsAndValidate(version = 1, migrations = emptyList())

            // The real drift detector: the production builder must accept the same file without a
            // Migration. A forced query is required -- Room opens the connection lazily, and the
            // identity-hash check that throws Room's "forgot to update the version number" error lives
            // in that open path, not in `create()` itself.
            val database = YearalDatabase.create(context, dispatcher)
            try {
                database.calendarDao().findById(EventCalendar.DEFAULT_ID)
            } finally {
                database.close()
            }
        }
}
