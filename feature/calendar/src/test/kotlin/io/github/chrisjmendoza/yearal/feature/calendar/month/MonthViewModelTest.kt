package io.github.chrisjmendoza.yearal.feature.calendar.month

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import io.github.chrisjmendoza.yearal.core.calendar.IfcMonth
import io.github.chrisjmendoza.yearal.core.calendar.IfcYearMonth
import io.github.chrisjmendoza.yearal.core.designsystem.format.IfcDateFormatter
import io.github.chrisjmendoza.yearal.core.domain.event.AgendaEntry
import io.github.chrisjmendoza.yearal.core.domain.event.Event
import io.github.chrisjmendoza.yearal.core.domain.event.EventCalendar
import io.github.chrisjmendoza.yearal.core.domain.event.EventTiming
import io.github.chrisjmendoza.yearal.core.domain.event.IfcRecurrence
import io.github.chrisjmendoza.yearal.core.domain.event.Occurrence
import io.github.chrisjmendoza.yearal.core.domain.holiday.HolidayEngine
import io.github.chrisjmendoza.yearal.core.domain.settings.UserSettings
import io.github.chrisjmendoza.yearal.core.domain.settings.WeekdayDisplay
import io.github.chrisjmendoza.yearal.core.holidays.HolidayPackLoader
import io.github.chrisjmendoza.yearal.core.navigation.MonthKey
import io.github.chrisjmendoza.yearal.core.testing.EventFixtures
import io.github.chrisjmendoza.yearal.core.testing.FakeDateTicker
import io.github.chrisjmendoza.yearal.core.testing.FakeEventRepository
import io.github.chrisjmendoza.yearal.core.testing.FakeObserveAgendaUseCase
import io.github.chrisjmendoza.yearal.core.testing.FakeSettingsRepository
import io.github.chrisjmendoza.yearal.feature.calendar.agenda.AgendaItemUi
import io.github.chrisjmendoza.yearal.feature.calendar.holiday.HolidayCatalog
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

/**
 * [MonthViewModel] and [MonthPages] against [FakeDateTicker] and [FakeSettingsRepository] with the
 * real [HolidayEngine] and bundled packs: the page ↔ month arithmetic at both ends of the
 * `docs/calendar-spec.md` §7.1 UI range, holidays per visible month from the enabled sets
 * (docs/ARCHITECTURE.md §3.3–3.4), the today page moving across midnight (docs/WORKFLOW.md §3) and
 * the live settings.
 *
 * A [StandardTestDispatcher] is used on purpose, as in `TodayViewModelTest`: the initial state (no
 * today yet) is observable before the first combined emission.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class MonthViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var catalog: HolidayCatalog
    private lateinit var formatter: IfcDateFormatter

    /** IFC month 10 is September, not October (CLAUDE.md rule 5): the month of the spec §4.1 example. */
    private val september2026 = IfcYearMonth(2026, IfcMonth.SEPTEMBER)
    private val october2026 = IfcYearMonth(2026, IfcMonth.OCTOBER)
    private val december2026 = IfcYearMonth(2026, IfcMonth.DECEMBER)
    private val christmas2026 = LocalDate.of(2026, 12, 25)
    private val yearDay2026 = LocalDate.of(2026, 12, 31)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        catalog = HolidayCatalog(HolidayEngine(), HolidayPackLoader(), context)
        formatter = IfcDateFormatter(context.resources, Locale.US)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        initialMonth: IfcYearMonth,
        initialSelectedDate: LocalDate? = null,
        ticker: FakeDateTicker = FakeDateTicker(LocalDate.of(2026, 9, 17)),
        settings: FakeSettingsRepository = FakeSettingsRepository(),
        observeAgenda: FakeObserveAgendaUseCase = FakeObserveAgendaUseCase(),
        eventRepository: FakeEventRepository = FakeEventRepository(),
    ) = MonthViewModel(
        initialMonth,
        initialSelectedDate,
        ticker,
        settings,
        catalog,
        observeAgenda,
        formatter,
        eventRepository,
    )

    // Spec §7.1: the pager covers 1583..9999, 13 pages a year.

    @Test
    fun `page 0 is January 1583 and the last page is December 9999`() {
        MonthPages.COUNT shouldBe (9999 - 1583 + 1) * 13
        MonthPages.pageOf(IfcYearMonth(1583, IfcMonth.JANUARY)) shouldBe 0
        MonthPages.monthAt(0) shouldBe IfcYearMonth(1583, IfcMonth.JANUARY)
        MonthPages.pageOf(IfcYearMonth(9999, IfcMonth.DECEMBER)) shouldBe MonthPages.LAST_PAGE
        MonthPages.monthAt(MonthPages.LAST_PAGE) shouldBe IfcYearMonth(9999, IfcMonth.DECEMBER)
    }

    @Test
    fun `MonthKey(2026, 10) is IFC September 2026 and round-trips through its page`() {
        val month = MonthPages.monthOf(MonthKey(2026, 10))
        month shouldBe september2026
        month.month.number shouldBe 10
        val page = MonthPages.pageOf(month)
        page shouldBe (2026 - 1583) * 13 + 9
        MonthPages.monthAt(page) shouldBe september2026
        MonthPages.monthAt(page + 1) shouldBe october2026
        MonthPages.monthAt(page - 9) shouldBe IfcYearMonth(2026, IfcMonth.JANUARY)
        MonthPages.monthAt(page - 10) shouldBe IfcYearMonth(2025, IfcMonth.DECEMBER)
    }

    @Test
    fun `keys outside the UI range are clamped, never thrown`() {
        MonthPages.monthOf(MonthKey(1200, 7)) shouldBe IfcYearMonth(1583, IfcMonth.SOL)
        MonthPages.monthOf(MonthKey(12000, 0)) shouldBe IfcYearMonth(9999, IfcMonth.JANUARY)
        MonthPages.monthOf(MonthKey(2026, 14)) shouldBe IfcYearMonth(2026, IfcMonth.DECEMBER)
        MonthPages.pageOf(IfcYearMonth(1, IfcMonth.MARCH)) shouldBe 2
    }

    @Test
    fun `starts on the key's page with no today, then fills in from the ticker and settings`() =
        runTest(dispatcher) {
            val viewModel = viewModel(september2026)
            val page = MonthPages.pageOf(september2026)
            viewModel.uiState.value shouldBe
                MonthUiState(currentPage = page, today = null, todayPage = null, selected = null)
            viewModel.uiState.test {
                awaitItem().today shouldBe null
                val loaded = awaitItem()
                loaded.currentPage shouldBe page
                loaded.today shouldBe LocalDate.of(2026, 9, 17)
                // Gregorian September 17, 2026 is IFC September 8, 2026 (spec §4.1): the same page.
                loaded.todayPage shouldBe page
                loaded.selected shouldBe null
                loaded.weekdayDisplay shouldBe WeekdayDisplay.BOTH
                loaded.holidaysByMonth.keys shouldContainExactly
                    setOf(IfcYearMonth(2026, IfcMonth.AUGUST), september2026, october2026)
                // Labor Day, Monday September 7, 2026, is day 250 = IFC August 26 (spec §7.4), so it
                // belongs to the August page; the September page (Gregorian Sep 10 – Oct 7) has Patriot Day.
                loaded.holidaysByMonth[IfcYearMonth(2026, IfcMonth.AUGUST)] shouldBe
                    mapOf(LocalDate.of(2026, 9, 7) to "Labor Day")
                loaded.holidaysByMonth[september2026] shouldBe mapOf(LocalDate.of(2026, 9, 11) to "Patriot Day")
            }
        }

    // ARCHITECTURE §3.3–3.4: holidays of the enabled sets, per visible month, keyed by Gregorian date.

    @Test
    fun `December 2026 lists Christmas Day and Year Day from the default packs`() =
        runTest(dispatcher) {
            viewModel(december2026).uiState.test {
                awaitItem()
                val december = awaitItem().holidaysByMonth[december2026].shouldNotBeNull()
                december[christmas2026] shouldBe "Christmas Day"
                // Year Day (ifc pack) shares Dec 31 with the US pack's New Year's Eve and Kwanzaa.
                december[yearDay2026].shouldNotBeNull() shouldContain "Year Day"
                december shouldContainKey LocalDate.of(2026, 12, 24)
            }
        }

    @Test
    fun `disabling the us pack removes Christmas but keeps Year Day`() =
        runTest(dispatcher) {
            val settings = FakeSettingsRepository(UserSettings(enabledHolidaySets = setOf("ifc")))
            viewModel(december2026, settings = settings).uiState.test {
                awaitItem()
                val december = awaitItem().holidaysByMonth[december2026].shouldNotBeNull()
                december shouldNotContainKey christmas2026
                december[yearDay2026] shouldBe "Year Day"
            }
        }

    @Test
    fun `toggling a pack in settings updates the holidays live`() =
        runTest(dispatcher) {
            val settings = FakeSettingsRepository()
            viewModel(december2026, settings = settings).uiState.test {
                awaitItem()
                awaitItem().holidaysByMonth[december2026].shouldNotBeNull() shouldContainKey christmas2026

                settings.update { it.copy(enabledHolidaySets = emptySet()) }

                awaitItem().holidaysByMonth.values.all { it.isEmpty() } shouldBe true
            }
        }

    @Test
    fun `paging evaluates holidays for the new page and its neighbours`() =
        runTest(dispatcher) {
            val viewModel = viewModel(october2026)
            viewModel.uiState.test {
                awaitItem()
                awaitItem().holidaysByMonth shouldNotContainKey december2026

                viewModel.showPage(MonthPages.pageOf(IfcYearMonth(2026, IfcMonth.NOVEMBER)))

                val paged = awaitItem()
                paged.currentPage shouldBe MonthPages.pageOf(IfcYearMonth(2026, IfcMonth.NOVEMBER))
                paged.holidaysByMonth[december2026].shouldNotBeNull()[christmas2026] shouldBe "Christmas Day"
            }
        }

    @Test
    fun `an out-of-range page is clamped`() =
        runTest(dispatcher) {
            val viewModel = viewModel(october2026)
            viewModel.uiState.test {
                awaitItem()
                awaitItem()
                viewModel.showPage(-5)
                awaitItem().currentPage shouldBe 0
                viewModel.showPage(Int.MAX_VALUE)
                awaitItem().currentPage shouldBe MonthPages.LAST_PAGE
            }
        }

    // WORKFLOW §3: crossing midnight with a fake clock; the today ring and the Today target both move.

    @Test
    fun `crossing midnight moves today and, at a month boundary, the today page`() =
        runTest(dispatcher) {
            // IFC September 28, 2026 is Gregorian October 7; the next day is IFC October 1.
            val ticker = FakeDateTicker(LocalDate.of(2026, 10, 7))
            viewModel(october2026, ticker = ticker).uiState.test {
                awaitItem()
                val before = awaitItem()
                before.today shouldBe LocalDate.of(2026, 10, 7)
                before.todayPage shouldBe MonthPages.pageOf(IfcYearMonth(2026, IfcMonth.SEPTEMBER))

                ticker.set(LocalDate.of(2026, 10, 8))

                val after = awaitItem()
                after.today shouldBe LocalDate.of(2026, 10, 8)
                after.todayPage shouldBe MonthPages.pageOf(october2026)
            }
        }

    @Test
    fun `Year Day as today targets the December page`() =
        runTest(dispatcher) {
            viewModel(october2026, ticker = FakeDateTicker(yearDay2026)).uiState.test {
                awaitItem()
                awaitItem().todayPage shouldBe MonthPages.pageOf(december2026)
            }
        }

    @Test
    fun `Leap Day as today targets the June page`() =
        runTest(dispatcher) {
            viewModel(october2026, ticker = FakeDateTicker(LocalDate.of(2028, 6, 17))).uiState.test {
                awaitItem()
                awaitItem().todayPage shouldBe MonthPages.pageOf(IfcYearMonth(2028, IfcMonth.JUNE))
            }
        }

    @Test
    fun `changing the weekday display setting updates the state`() =
        runTest(dispatcher) {
            val settings = FakeSettingsRepository()
            viewModel(october2026, settings = settings).uiState.test {
                awaitItem()
                awaitItem().weekdayDisplay shouldBe WeekdayDisplay.BOTH

                settings.update { it.copy(weekdayDisplay = WeekdayDisplay.ACTUAL) }

                awaitItem().weekdayDisplay shouldBe WeekdayDisplay.ACTUAL
            }
        }

    @Test
    fun `selecting a day is reflected in the state`() =
        runTest(dispatcher) {
            val viewModel = viewModel(december2026)
            viewModel.uiState.test {
                awaitItem()
                awaitItem().selected shouldBe null

                viewModel.select(yearDay2026)

                awaitItem().selected shouldBe yearDay2026
            }
        }

    // docs/ROADMAP.md M3 T4: the expanded-width list-detail pane's close action and its BackHandler
    // both call clearSelection — proven directly on the ViewModel since MonthRoute's own wiring needs
    // a real window and Hilt, neither of which this module's tests set up (docs/WORKFLOW.md §3, no
    // XRoute in this codebase is unit-tested for the same reason).

    @Test
    fun `clearing the selection resets it to null`() =
        runTest(dispatcher) {
            val viewModel = viewModel(december2026)
            viewModel.uiState.test {
                awaitItem()
                awaitItem().selected shouldBe null

                viewModel.select(yearDay2026)
                awaitItem().selected shouldBe yearDay2026

                viewModel.clearSelection()
                awaitItem().selected shouldBe null
            }
        }

    // FEATURES C4: event dots, sourced from ObserveAgendaUseCase for the three warm pages.

    @Test
    fun `event counts are requested for the current page and its two neighbours`() =
        runTest(dispatcher) {
            val agenda = FakeObserveAgendaUseCase()
            viewModel(september2026, observeAgenda = agenda).uiState.test {
                awaitItem()
                val loaded = awaitItem()
                loaded.eventCountsByMonth.keys shouldContainExactly
                    setOf(IfcYearMonth(2026, IfcMonth.AUGUST), september2026, october2026)
                // Plus one single-day range for the selected-day summary (today, since nothing is
                // selected), from the separately keyed MonthViewModel.summaryAgenda flow.
                agenda.requestedRanges shouldContainExactly
                    listOf(
                        IfcYearMonth(2026, IfcMonth.AUGUST).gregorianRange,
                        september2026.gregorianRange,
                        october2026.gregorianRange,
                        LocalDate.of(2026, 9, 17)..LocalDate.of(2026, 9, 17),
                    )
            }
        }

    // Compose-perf pass: a page change used to tear down and re-issue observeAgenda for every warm
    // month, including the two that stayed warm across a single-page swipe (docs/ARCHITECTURE.md §3.4
    // "The pager keeps three months warm"). MonthViewModel now caches each month's shared flow
    // (agendaCountsFor) so only the month that newly enters the window is queried again.

    @Test
    fun `paging keeps the agenda subscription for months that stay warm`() =
        runTest(dispatcher) {
            val agenda = FakeObserveAgendaUseCase()
            val november2026 = IfcYearMonth(2026, IfcMonth.NOVEMBER)
            val viewModel = viewModel(october2026, observeAgenda = agenda)
            viewModel.uiState.test {
                awaitItem()
                awaitItem()
                // The initial warm window {Sep, Oct, Nov} issues exactly one query per month, plus one
                // single-day range for the selected-day summary (today; see the comment on the test
                // above).
                agenda.requestedRanges shouldContainExactly
                    listOf(
                        september2026.gregorianRange,
                        october2026.gregorianRange,
                        november2026.gregorianRange,
                        LocalDate.of(2026, 9, 17)..LocalDate.of(2026, 9, 17),
                    )

                viewModel.showPage(MonthPages.pageOf(november2026))
                awaitItem()
                awaitItem()

                // The new warm window {Oct, Nov, Dec} adds only December: Oct and Nov are not
                // re-requested, because their shared flows from the previous window are reused; the
                // summary's own range is untouched by paging (neither today nor the selection changed).
                agenda.requestedRanges shouldContainExactly
                    listOf(
                        september2026.gregorianRange,
                        october2026.gregorianRange,
                        november2026.gregorianRange,
                        LocalDate.of(2026, 9, 17)..LocalDate.of(2026, 9, 17),
                        december2026.gregorianRange,
                    )
            }
        }

    @Test
    fun `an event's occurrence count reaches the grid for its page`() =
        runTest(dispatcher) {
            val agenda = FakeObserveAgendaUseCase()
            val onScreen = LocalDate.of(2026, 9, 17)
            agenda.putEntry(EventFixtures.entry(EventFixtures.allDay(date = onScreen, title = "Meetup")))
            viewModel(september2026, observeAgenda = agenda).uiState.test {
                awaitItem()
                val loaded = awaitItem()
                loaded.eventCountsByMonth[september2026]?.get(onScreen) shouldBe 1
            }
        }

    // docs/design-plan.md §4.2, owner note 2: the selected-day summary below the grid — today when
    // nothing is selected, the selected day once one is picked.

    @Test
    fun `the summary follows today when nothing is selected`() =
        runTest(dispatcher) {
            viewModel(september2026).uiState.test {
                awaitItem()
                val loaded = awaitItem()
                loaded.summaryDate shouldBe LocalDate.of(2026, 9, 17)
            }
        }

    @Test
    fun `selecting a day moves the summary to it`() =
        runTest(dispatcher) {
            val viewModel = viewModel(december2026)
            viewModel.uiState.test {
                awaitItem()
                awaitItem()

                viewModel.select(yearDay2026)

                val loaded = awaitItem()
                loaded.summaryDate shouldBe yearDay2026
            }
        }

    @Test
    fun `the summary's holidays come from the enabled sets on the summary date`() =
        runTest(dispatcher) {
            val viewModel = viewModel(december2026)
            viewModel.uiState.test {
                awaitItem()
                awaitItem()

                viewModel.select(christmas2026)

                val loaded = awaitItem()
                loaded.dayDetail?.holidays shouldBe listOf("Christmas Day")
            }
        }

    @Test
    fun `the summary's agenda comes from the selected day's own occurrences`() =
        runTest(dispatcher) {
            val agenda = FakeObserveAgendaUseCase()
            agenda.putEntry(EventFixtures.entry(EventFixtures.allDay(date = christmas2026, title = "Party")))
            val viewModel = viewModel(december2026, observeAgenda = agenda)
            viewModel.uiState.test {
                awaitItem()
                awaitItem()

                viewModel.select(christmas2026)

                // The summary's own agenda comes from a separately keyed flow
                // (MonthViewModel.summaryAgenda), so its settled value lands an emission after the one
                // that already shows the new selection — see the regression test below for what that
                // first emission must (and must not) show.
                awaitItem()
                val settled = awaitItem()
                settled.summaryDate shouldBe christmas2026
                settled.dayDetail?.agenda?.map { it.title } shouldBe listOf("Party")
            }
        }

    // Design-pass fix 1: select() updates the partial state immediately, but summaryAgenda (a flatMapLatest keyed
    // on the summary date) settles later, so the emission in between must never pair the new date with
    // the previous day's rows.

    @Test
    fun `selecting a new day never pairs its date with the previous day's agenda rows`() =
        runTest(dispatcher) {
            val today = LocalDate.of(2026, 9, 17)
            val agenda = FakeObserveAgendaUseCase()
            agenda.putEntry(EventFixtures.entry(EventFixtures.allDay(date = today, title = "Standup")))
            agenda.putEntry(EventFixtures.entry(EventFixtures.allDay(date = christmas2026, title = "Party")))
            val viewModel = viewModel(december2026, observeAgenda = agenda)
            viewModel.uiState.test {
                awaitItem()
                val initial = awaitItem()
                initial.summaryDate shouldBe today
                initial.dayDetail?.agenda?.map { it.title } shouldBe listOf("Standup")

                viewModel.select(christmas2026)

                // The very next emission already carries the new summary date. It must not show
                // "Standup" (today's rows, still the only value MonthViewModel.summaryAgenda had ready)
                // under christmas2026 — that pairing was the bug; the fix shows no rows for the date
                // mismatch instead.
                val justSelected = awaitItem()
                justSelected.summaryDate shouldBe christmas2026
                justSelected.dayDetail?.agenda shouldBe emptyList()

                // Once the re-issued subscription for christmas2026's range delivers its own value, the
                // correct rows settle.
                val settled = awaitItem()
                settled.summaryDate shouldBe christmas2026
                settled.dayDetail?.agenda?.map { it.title } shouldBe listOf("Party")
            }
        }

    @Test
    fun `paging re-evaluates event counts for the new warm window`() =
        runTest(dispatcher) {
            val agenda = FakeObserveAgendaUseCase()
            agenda.putEntry(EventFixtures.entry(EventFixtures.allDay(date = christmas2026, title = "Party")))
            val viewModel = viewModel(october2026, observeAgenda = agenda)
            viewModel.uiState.test {
                awaitItem()
                awaitItem().eventCountsByMonth shouldNotContainKey december2026

                viewModel.showPage(MonthPages.pageOf(IfcYearMonth(2026, IfcMonth.NOVEMBER)))

                // The page and its event counts come from two combined flows, so the page change and the
                // settled event counts can land in separate emissions; the second is always consistent.
                awaitItem()
                val paged = awaitItem()
                paged.eventCountsByMonth[december2026]?.get(christmas2026) shouldBe 1
            }
        }

    // The day-card merge (owner request): the popup Day detail is gone, and MonthViewModel now owns
    // its full formatted content directly, as MonthUiState.dayDetail. These tests were ported from the
    // old DayViewModelTest, adapted to a selected (or "today") day inside the Month pager's own state
    // instead of a standalone epoch-day-keyed ViewModel.

    @Test
    fun `the day card shows the spec's worked example when nothing is selected`() =
        runTest(dispatcher) {
            viewModel(september2026).uiState.test {
                awaitItem()
                val detail = awaitItem().dayDetail.shouldNotBeNull()
                detail.gregorianDate shouldBe LocalDate.of(2026, 9, 17)
                detail.ifcLong shouldBe "September 8, 2026"
                detail.numeric shouldBe "IFC 2026-10-08"
                detail.gregorianLong shouldBe "Thursday, September 17, 2026"
                detail.nominalWeekday shouldBe "IFC weekday: Sunday"
                detail.actualWeekday shouldBe "Actual weekday: Thursday"
                detail.weekdaysDescription shouldBe "IFC Sunday, actual Thursday"
                detail.dayAndWeek shouldBe "Day 260 · Week 38 of 52"
                detail.quarter shouldBe "Q3"
                detail.isToday shouldBe true
                detail.holidays shouldBe emptyList()
            }
        }

    @Test
    fun `selecting Leap Day shows no IFC weekday and its holiday`() =
        runTest(dispatcher) {
            val viewModel = viewModel(IfcYearMonth(2028, IfcMonth.JUNE))
            viewModel.uiState.test {
                awaitItem()
                awaitItem()

                viewModel.select(LocalDate.of(2028, 6, 17))

                val detail = awaitItem().dayDetail.shouldNotBeNull()
                detail.ifcLong shouldBe "Leap Day, 2028"
                detail.numeric shouldBe "IFC 2028-06-29"
                detail.nominalWeekday shouldBe "no IFC weekday"
                detail.dayAndWeek shouldBe "Day 169 · outside the weeks"
                detail.holidays shouldContainExactly listOf("Leap Day")
            }
        }

    @Test
    fun `selecting Year Day shows no IFC weekday and shares the day with the US pack`() =
        runTest(dispatcher) {
            val viewModel = viewModel(december2026)
            viewModel.uiState.test {
                awaitItem()
                awaitItem()

                viewModel.select(yearDay2026)

                val detail = awaitItem().dayDetail.shouldNotBeNull()
                detail.ifcLong shouldBe "Year Day, 2026"
                detail.numeric shouldBe "IFC 2026-13-29"
                detail.nominalWeekday shouldBe "no IFC weekday"
                // Engine order: by set id (ifc before us), then holiday id (kwanzaa before new_years_eve).
                detail.holidays shouldContainExactly listOf("Year Day", "Kwanzaa", "New Year's Eve")
            }
        }

    // WORKFLOW §3: crossing midnight with a fake clock; the card's own "Today" badge must roll over
    // even though the badge belongs to a *selected* day, not just to "today" itself.

    @Test
    fun `the selected day card's isToday flips across midnight in both directions`() =
        runTest(dispatcher) {
            val ticker = FakeDateTicker(LocalDate.of(2026, 9, 17))
            val viewModel = viewModel(september2026, ticker = ticker)
            viewModel.uiState.test {
                awaitItem()
                awaitItem()

                viewModel.select(LocalDate.of(2026, 9, 18))
                awaitItem().dayDetail?.isToday shouldBe false

                ticker.set(LocalDate.of(2026, 9, 18))
                awaitItem().dayDetail?.isToday shouldBe true

                ticker.set(LocalDate.of(2026, 9, 19))
                val after = awaitItem()
                after.dayDetail?.isToday shouldBe false
                // The selected day itself never changes with the clock.
                after.dayDetail?.ifcLong shouldBe "September 9, 2026"
            }
        }

    // FEATURES E1: "delete this occurrence" — docs/contracts/Events.md §4, §7 "T6–T8": the exdate key
    // is the occurrence's own start date, never the day the card is showing. Moved from the old
    // DayViewModelTest along with the rest of the delete flow (the day-card merge).

    /** A three-day recurring all-day event, Dec 30 – Year Day – Jan 1, whose own start is Dec 30. */
    private suspend fun seedMultiDayRecurringEvent(repository: FakeEventRepository): Event {
        val draft =
            EventFixtures.allDay(
                date = LocalDate.of(2026, 12, 30),
                days = 3,
                title = "New Year trip",
                recurrence = IfcRecurrence.yearlyOn(LocalDate.of(2026, 12, 30)),
            )
        val id = repository.upsertEvent(draft)
        return checkNotNull(repository.getEvent(id))
    }

    private fun multiDayOccurrence(event: Event): Occurrence =
        Occurrence(
            eventId = event.id,
            startLocal = LocalDateTime.of(2026, 12, 30, 0, 0),
            endLocal = LocalDateTime.of(2027, 1, 2, 0, 0),
            zone = null,
            allDay = true,
        )

    @Test
    fun `deleting a shown occurrence exdates its own start date, not the day being viewed`() =
        runTest(dispatcher) {
            val repository = FakeEventRepository()
            val event = seedMultiDayRecurringEvent(repository)
            val occurrence = multiDayOccurrence(event)
            val shownDay = LocalDate.of(2026, 12, 31) // Year Day: the middle of the span, not its own start.
            val agenda = FakeObserveAgendaUseCase()
            agenda.putEntry(AgendaEntry(event, occurrence, EventCalendar.DEFAULT_COLOR_ARGB, ZoneId.of("UTC")))

            val viewModel = viewModel(december2026, observeAgenda = agenda, eventRepository = repository)
            viewModel.uiState.test {
                awaitItem()
                awaitItem()

                viewModel.select(shownDay)
                awaitItem()
                val loaded = awaitItem()
                val item =
                    loaded.dayDetail
                        .shouldNotBeNull()
                        .agenda
                        .single()
                item.isRecurring shouldBe true
                item.occurrenceDate shouldBe LocalDate.of(2026, 12, 30)

                viewModel.requestDelete(item)
                awaitItem().dayDetail?.pendingDelete shouldBe item

                viewModel.confirmDelete()
                awaitItem().dayDetail?.pendingDelete.shouldBeNull()
            }
            repository.getEvent(event.id)?.exdates shouldBe setOf(LocalDate.of(2026, 12, 30))
        }

    @Test
    fun `confirming a recurring delete offers undo, and undo clears the exdate`() =
        runTest(dispatcher) {
            val repository = FakeEventRepository()
            val event = seedMultiDayRecurringEvent(repository)
            val occurrence = multiDayOccurrence(event)
            val item =
                AgendaItemUi(
                    eventId = event.id,
                    title = event.title,
                    isAllDay = true,
                    startTime = null,
                    endTime = null,
                    colorArgb = EventCalendar.DEFAULT_COLOR_ARGB,
                    isRecurring = true,
                    occurrenceDate = occurrence.occurrenceDate,
                )
            val viewModel =
                viewModel(
                    december2026,
                    observeAgenda = FakeObserveAgendaUseCase(),
                    eventRepository = repository,
                )
            viewModel.events.test {
                viewModel.requestDelete(item)
                viewModel.confirmDelete()

                val deleted = awaitItem().shouldBeInstanceOf<MonthEvent.OccurrenceDeleted>()
                deleted.eventId shouldBe event.id
                deleted.occurrenceDate shouldBe LocalDate.of(2026, 12, 30)

                repository.getEvent(event.id)?.exdates shouldBe setOf(LocalDate.of(2026, 12, 30))

                viewModel.undoDeleteOccurrence(deleted.eventId, deleted.occurrenceDate)
                runCurrent()
                repository
                    .getEvent(event.id)
                    ?.exdates
                    .orEmpty()
                    .shouldBeEmpty()
            }
        }

    @Test
    fun `cancelling a delete confirmation changes nothing`() =
        runTest(dispatcher) {
            val repository = FakeEventRepository()
            val event = seedMultiDayRecurringEvent(repository)
            val occurrence = multiDayOccurrence(event)
            val agenda = FakeObserveAgendaUseCase()
            agenda.putEntry(AgendaEntry(event, occurrence, EventCalendar.DEFAULT_COLOR_ARGB, ZoneId.of("UTC")))

            val viewModel =
                viewModel(december2026, observeAgenda = agenda, eventRepository = repository)
            viewModel.uiState.test {
                awaitItem()
                awaitItem()

                viewModel.select(LocalDate.of(2026, 12, 30))
                awaitItem()
                val loaded = awaitItem()

                viewModel.requestDelete(
                    loaded.dayDetail
                        .shouldNotBeNull()
                        .agenda
                        .single(),
                )
                awaitItem().dayDetail?.pendingDelete.shouldNotBeNull()

                viewModel.cancelDelete()
                awaitItem().dayDetail?.pendingDelete.shouldBeNull()
            }
            repository
                .getEvent(event.id)
                ?.exdates
                .orEmpty()
                .shouldBeEmpty()
        }

    @Test
    fun `a non-recurring row deletes the whole event, not an exdate`() =
        runTest(dispatcher) {
            val repository = FakeEventRepository()
            val day = LocalDate.of(2026, 9, 17)
            val storedId = repository.upsertEvent(EventFixtures.allDay(date = day, title = "Once"))
            val event = checkNotNull(repository.getEvent(storedId))
            val agenda = FakeObserveAgendaUseCase()
            agenda.putEntry(EventFixtures.entry(event))

            val viewModel = viewModel(september2026, observeAgenda = agenda, eventRepository = repository)
            viewModel.uiState.test {
                awaitItem()
                val loaded = awaitItem()
                val item =
                    loaded.dayDetail
                        .shouldNotBeNull()
                        .agenda
                        .single()
                item.isRecurring shouldBe false

                viewModel.requestDelete(item)
                awaitItem().dayDetail?.pendingDelete.shouldNotBeNull()
                viewModel.confirmDelete()
                awaitItem().dayDetail?.pendingDelete.shouldBeNull()
            }
            repository.getEvent(event.id).shouldBeNull()
        }

    // A pending delete belongs to one specific day; if the card's own date moves out from under it —
    // here, a new selection — the confirmation must be dropped rather than left open on the wrong day.

    @Test
    fun `pending delete clears when the summary date changes`() =
        runTest(dispatcher) {
            val repository = FakeEventRepository()
            val day = LocalDate.of(2026, 9, 17)
            val storedId = repository.upsertEvent(EventFixtures.allDay(date = day, title = "Once"))
            val event = checkNotNull(repository.getEvent(storedId))
            val agenda = FakeObserveAgendaUseCase()
            agenda.putEntry(EventFixtures.entry(event))

            val viewModel = viewModel(september2026, observeAgenda = agenda, eventRepository = repository)
            viewModel.uiState.test {
                awaitItem()
                val loaded = awaitItem()
                val item =
                    loaded.dayDetail
                        .shouldNotBeNull()
                        .agenda
                        .single()

                viewModel.requestDelete(item)
                awaitItem().dayDetail?.pendingDelete shouldBe item

                viewModel.select(LocalDate.of(2026, 9, 18))

                // Two independent flows react to the selection change (the main uiState combine, and the
                // dedicated collector that clears pendingDelete), so the exact number of intervening
                // emissions is not load-bearing — only that pendingDelete has settled to null once things
                // catch up.
                var state = awaitItem()
                while (state.dayDetail?.pendingDelete != null) {
                    state = awaitItem()
                }
                state.dayDetail?.pendingDelete.shouldBeNull()
            }
            // Nothing was actually deleted — only the confirmation was dropped.
            repository
                .getEvent(event.id)
                ?.exdates
                .orEmpty()
                .shouldBeEmpty()
        }

    // docs/ROADMAP.md, the day-card merge: MonthKey.selectedEpochDay replaces DayKey, and
    // MonthPages.selectedDateOf resolves it fail-soft — the same treatment ConverterKey's and
    // EventEditorKey's own prefill epoch days get (never a crash from a synthesized widget or
    // notification intent, docs/security-and-privacy.md §6.3).

    @Test
    fun `selectedDateOf resolves a valid epoch day`() {
        val epochDay = LocalDate.of(2026, 9, 17).toEpochDay()
        MonthPages.selectedDateOf(MonthKey(2026, 10, selectedEpochDay = epochDay)) shouldBe
            LocalDate.of(2026, 9, 17)
    }

    @Test
    fun `selectedDateOf is null when the key names no selection`() {
        MonthPages.selectedDateOf(MonthKey(2026, 10)).shouldBeNull()
    }

    @Test
    fun `selectedDateOf fails soft for an epoch day LocalDate cannot represent`() {
        MonthPages.selectedDateOf(MonthKey(2026, 10, selectedEpochDay = Long.MIN_VALUE)).shouldBeNull()
        MonthPages.selectedDateOf(MonthKey(2026, 10, selectedEpochDay = Long.MAX_VALUE)).shouldBeNull()
    }

    @Test
    fun `selectedDateOf fails soft for a year outside the pager's UI range`() {
        val beforeFirstYear = LocalDate.of(1582, 12, 31).toEpochDay()
        val afterLastYear = LocalDate.of(10000, 1, 1).toEpochDay()
        MonthPages.selectedDateOf(MonthKey(1583, 1, selectedEpochDay = beforeFirstYear)).shouldBeNull()
        MonthPages.selectedDateOf(MonthKey(9999, 13, selectedEpochDay = afterLastYear)).shouldBeNull()
    }

    @Test
    fun `an initial selection from MonthKey is reflected in state from the start`() =
        runTest(dispatcher) {
            val selectedDate = LocalDate.of(2026, 12, 31)
            val viewModel = viewModel(december2026, initialSelectedDate = selectedDate)

            viewModel.uiState.value.selected shouldBe selectedDate

            viewModel.uiState.test {
                awaitItem().selected shouldBe selectedDate
                awaitItem().summaryDate shouldBe selectedDate
            }
        }
}
