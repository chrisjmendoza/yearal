package io.github.chrisjmendoza.yearal.feature.holidays

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import io.github.chrisjmendoza.yearal.core.calendar.IfcDate
import io.github.chrisjmendoza.yearal.core.designsystem.format.IfcDateFormatter
import io.github.chrisjmendoza.yearal.core.designsystem.picker.DatePickerRange
import io.github.chrisjmendoza.yearal.core.domain.holiday.HolidayEngine
import io.github.chrisjmendoza.yearal.core.domain.settings.UserSettings
import io.github.chrisjmendoza.yearal.core.holidays.HolidayPackLoader
import io.github.chrisjmendoza.yearal.core.testing.FakeDateTicker
import io.github.chrisjmendoza.yearal.core.testing.FakeSettingsRepository
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.Locale

/**
 * [HolidaysViewModel] against [FakeSettingsRepository], [FakeDateTicker] and the real
 * [HolidayPackLoader] / [HolidayEngine] / bundled packs (ROADMAP M6 T2): browsing and toggling sets
 * agrees with Settings' own store, the per-year list has both dates and the mandatory `IFC` prefix,
 * Leap Day is present only in leap years (CLAUDE.md rule 6) and Year Day in every year, the year clamps
 * to [DatePickerRange] and follows [io.github.chrisjmendoza.yearal.core.domain.DateTicker] across a
 * midnight year rollover (docs/WORKFLOW.md §3) until the user pages away, and every set disabled yields
 * an empty year list rather than an error.
 *
 * A [StandardTestDispatcher] is used on purpose, as in `SettingsViewModelTest` and `YearViewModelTest`:
 * with an unconfined one the initial [HolidaysUiState.Loading] is conflated away before it can be
 * observed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class HolidaysViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val loader = HolidayPackLoader()
    private val engine = HolidayEngine()
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val formatter: IfcDateFormatter get() = IfcDateFormatter(context.resources, Locale.US)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // The last argument is the test's own dispatcher: in production the view model evaluates packs on
    // Dispatchers.Default, which virtual time cannot control, so leaving it there made these tests race
    // real threads — they passed alone and failed in a loaded full-suite run (docs/WORKFLOW.md §2).
    private fun viewModel(
        repository: FakeSettingsRepository = FakeSettingsRepository(),
        ticker: FakeDateTicker = FakeDateTicker(LocalDate.of(2026, 9, 17)),
    ) = HolidaysViewModel(repository, loader, engine, ticker, formatter, context, dispatcher)

    // ----- Browsing and toggling (task 1): agrees live with SettingsRepository.enabledHolidaySets -----

    @Test
    fun `starts loading, then lists every bundled set with its name, region and on-off state`() =
        runTest(dispatcher) {
            viewModel(FakeSettingsRepository(UserSettings(enabledHolidaySets = setOf("ifc", "us")))).uiState.test {
                awaitItem() shouldBe HolidaysUiState.Loading
                val loaded = awaitItem().shouldBeInstanceOf<HolidaysUiState.Loaded>()
                loaded.sets.map { it.id } shouldContainExactly listOf("ifc", "us", "religious-christian")
                val ifc = loaded.sets.first { it.id == "ifc" }
                ifc.name shouldBe "International Fixed Calendar"
                ifc.region shouldBe null
                ifc.enabled shouldBe true
                ifc.holidayCount shouldBe loader.loadBundled("ifc").holidays.size

                val us = loaded.sets.first { it.id == "us" }
                us.region shouldBe "United States"
                us.enabled shouldBe true

                val christian = loaded.sets.first { it.id == "religious-christian" }
                christian.enabled shouldBe false
            }
        }

    @Test
    fun `a set disabled in Settings shows as off here`() =
        runTest(dispatcher) {
            val repository = FakeSettingsRepository(UserSettings(enabledHolidaySets = setOf("ifc")))
            viewModel(repository).uiState.test {
                awaitItem()
                val loaded = awaitItem().shouldBeInstanceOf<HolidaysUiState.Loaded>()
                loaded.sets.first { it.id == "us" }.enabled shouldBe false
            }
        }

    @Test
    fun `toggling a set updates the list and writes through the repository`() =
        runTest(dispatcher) {
            val repository = FakeSettingsRepository()
            val viewModel = viewModel(repository)
            viewModel.uiState.test {
                awaitItem()
                awaitItem()
                    .shouldBeInstanceOf<HolidaysUiState.Loaded>()
                    .sets
                    .first { it.id == "religious-christian" }
                    .enabled shouldBe false

                viewModel.setHolidaySetEnabled("religious-christian", true)

                awaitItem()
                    .shouldBeInstanceOf<HolidaysUiState.Loaded>()
                    .sets
                    .first { it.id == "religious-christian" }
                    .enabled shouldBe true
                repository.current.enabledHolidaySets shouldBe setOf("ifc", "us", "religious-christian")

                viewModel.setHolidaySetEnabled("us", false)

                awaitItem()
                    .shouldBeInstanceOf<HolidaysUiState.Loaded>()
                    .sets
                    .first { it.id == "us" }
                    .enabled shouldBe
                    false
                repository.current.enabledHolidaySets shouldBe setOf("ifc", "religious-christian")
            }
        }

    // ----- The per-year list (task 2) -----

    // CLAUDE.md rule 6: Leap Day exists only in leap years; 1900 and 2100 are common (Gregorian) years.
    @Test
    fun `Leap Day appears in leap year 2028 but not in common year 2026`() =
        runTest(dispatcher) {
            viewModel(ticker = FakeDateTicker(LocalDate.of(2028, 1, 1))).uiState.test {
                awaitItem()
                awaitItem()
                    .shouldBeInstanceOf<HolidaysUiState.Loaded>()
                    .groups
                    .flatMap { it.rows }
                    .map { it.name } shouldContain "Leap Day"
            }
            viewModel(ticker = FakeDateTicker(LocalDate.of(2026, 1, 1))).uiState.test {
                awaitItem()
                awaitItem()
                    .shouldBeInstanceOf<HolidaysUiState.Loaded>()
                    .groups
                    .flatMap { it.rows }
                    .map { it.name } shouldNotContain "Leap Day"
            }
        }

    @Test
    fun `Leap Day is absent in the century common years 1900 and 2100`() =
        runTest(dispatcher) {
            for (year in listOf(1900, 2100)) {
                viewModel(ticker = FakeDateTicker(LocalDate.of(year, 1, 1))).uiState.test {
                    awaitItem()
                    awaitItem()
                        .shouldBeInstanceOf<HolidaysUiState.Loaded>()
                        .groups
                        .flatMap { it.rows }
                        .map { it.name } shouldNotContain "Leap Day"
                }
            }
        }

    @Test
    fun `Year Day appears every year, leap or not`() =
        runTest(dispatcher) {
            for (year in listOf(2026, 2028, 1900, 2100)) {
                viewModel(ticker = FakeDateTicker(LocalDate.of(year, 1, 1))).uiState.test {
                    awaitItem()
                    val loaded = awaitItem().shouldBeInstanceOf<HolidaysUiState.Loaded>()
                    loaded.groups.last().monthLabel shouldBe "December"
                    loaded.groups
                        .last()
                        .rows
                        .map { it.name } shouldContain "Year Day"
                }
            }
        }

    // Row order: groups in calendar month order, rows in date order within a group.
    @Test
    fun `groups are in IFC month order and rows are in date order within a group`() =
        runTest(dispatcher) {
            viewModel(ticker = FakeDateTicker(LocalDate.of(2026, 1, 1))).uiState.test {
                awaitItem()
                val loaded = awaitItem().shouldBeInstanceOf<HolidaysUiState.Loaded>()
                // "January" first, "December" last, and every group's own rows already sorted by epochDay.
                loaded.groups.first().monthLabel shouldBe "January"
                loaded.groups.last().monthLabel shouldBe "December"
                for (group in loaded.groups) {
                    group.rows.map { it.epochDay } shouldBe group.rows.map { it.epochDay }.sorted()
                }
            }
        }

    // Both dates on a row, and the mandatory "IFC" prefix on the numeric one (CLAUDE.md rule 5).
    @Test
    fun `a row carries both dates, with the IFC prefix on the numeric form`() =
        runTest(dispatcher) {
            viewModel(ticker = FakeDateTicker(LocalDate.of(2026, 1, 1))).uiState.test {
                awaitItem()
                val loaded = awaitItem().shouldBeInstanceOf<HolidaysUiState.Loaded>()
                val newYear =
                    loaded.groups
                        .first()
                        .rows
                        .first { it.name == "New Year's Day" }
                val expectedIfc = IfcDate.from(LocalDate.of(2026, 1, 1))
                newYear.ifcLong shouldBe formatter.formatLong(expectedIfc)
                newYear.ifcNumeric shouldBe expectedIfc.toPrefixedString()
                newYear.ifcNumeric shouldStartWith "IFC "
                newYear.gregorianLong shouldBe formatter.formatGregorianLong(LocalDate.of(2026, 1, 1))
                newYear.epochDay shouldBe LocalDate.of(2026, 1, 1).toEpochDay()
            }
        }

    // docs/design-plan.md section 4.7: the Holidays screen shows Year Day and Leap Day with the
    // intercalary mark instead of the plain holiday diamond, driven by this flag.
    @Test
    fun `isIntercalary is true only for Year Day and Leap Day rows`() =
        runTest(dispatcher) {
            viewModel(ticker = FakeDateTicker(LocalDate.of(2028, 1, 1))).uiState.test {
                awaitItem()
                val loaded = awaitItem().shouldBeInstanceOf<HolidaysUiState.Loaded>()
                val rows = loaded.groups.flatMap { it.rows }
                rows.first { it.name == "New Year's Day" }.isIntercalary shouldBe false
                rows.first { it.name == "Year Day" }.isIntercalary shouldBe true
                rows.first { it.name == "Leap Day" }.isIntercalary shouldBe true
            }
        }

    // ----- The default year (docs/WORKFLOW.md §3: crossing midnight, including a year rollover) -----

    @Test
    fun `the default year follows the ticker`() =
        runTest(dispatcher) {
            val ticker = FakeDateTicker(LocalDate.of(2026, 9, 17))
            viewModel(ticker = ticker).uiState.test {
                awaitItem()
                awaitItem().shouldBeInstanceOf<HolidaysUiState.Loaded>().year shouldBe 2026

                ticker.set(LocalDate.of(2027, 3, 1))

                awaitItem().shouldBeInstanceOf<HolidaysUiState.Loaded>().year shouldBe 2027
            }
        }

    @Test
    fun `crossing from Year Day into January 1 rolls the default year into the next one`() =
        runTest(dispatcher) {
            val ticker = FakeDateTicker(LocalDate.of(2026, 12, 31))
            viewModel(ticker = ticker).uiState.test {
                awaitItem()
                awaitItem().shouldBeInstanceOf<HolidaysUiState.Loaded>().year shouldBe 2026

                ticker.set(LocalDate.of(2027, 1, 1))

                awaitItem().shouldBeInstanceOf<HolidaysUiState.Loaded>().year shouldBe 2027
            }
        }

    @Test
    fun `once the user pages away, the ticker no longer moves the shown year`() =
        runTest(dispatcher) {
            val ticker = FakeDateTicker(LocalDate.of(2026, 12, 31))
            val viewModel = viewModel(ticker = ticker)
            viewModel.uiState.test {
                awaitItem()
                awaitItem().shouldBeInstanceOf<HolidaysUiState.Loaded>().year shouldBe 2026

                viewModel.goToYear(2030)
                awaitItem().shouldBeInstanceOf<HolidaysUiState.Loaded>().year shouldBe 2030

                ticker.set(LocalDate.of(2027, 1, 1))
                expectNoEvents()
            }
        }

    // ARCHITECTURE "Reconciled decisions" 6: years clamp to DatePickerRange (1583..9999).
    @Test
    fun `goToYear clamps at 1583 and 9999`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.uiState.test {
                awaitItem()
                awaitItem()

                viewModel.goToYear(1000)
                awaitItem().shouldBeInstanceOf<HolidaysUiState.Loaded>().let {
                    it.year shouldBe DatePickerRange.MIN_YEAR
                    it.canGoPreviousYear shouldBe false
                    it.canGoNextYear shouldBe true
                }

                viewModel.goToYear(20000)
                awaitItem().shouldBeInstanceOf<HolidaysUiState.Loaded>().let {
                    it.year shouldBe DatePickerRange.MAX_YEAR
                    it.canGoPreviousYear shouldBe true
                    it.canGoNextYear shouldBe false
                }
            }
        }

    // ----- Empty state -----

    @Test
    fun `every set disabled yields an empty year list, not an error`() =
        runTest(dispatcher) {
            val repository = FakeSettingsRepository(UserSettings(enabledHolidaySets = emptySet()))
            viewModel(repository).uiState.test {
                awaitItem()
                val loaded = awaitItem().shouldBeInstanceOf<HolidaysUiState.Loaded>()
                loaded.groups.shouldBeEmpty()
                loaded.sets.all { !it.enabled } shouldBe true
            }
        }
}
