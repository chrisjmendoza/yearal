package io.github.chrisjmendoza.yearal.feature.events.list

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.chrisjmendoza.yearal.core.designsystem.format.IfcDateFormatter
import io.github.chrisjmendoza.yearal.core.domain.event.Event
import io.github.chrisjmendoza.yearal.core.domain.event.EventCalendar
import io.github.chrisjmendoza.yearal.core.domain.event.EventCategory
import io.github.chrisjmendoza.yearal.core.domain.event.EventTiming
import io.github.chrisjmendoza.yearal.core.testing.EventFixtures
import io.github.chrisjmendoza.yearal.core.testing.FakeEventRepository
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.Locale

/**
 * [EventListViewModel] against [FakeEventRepository] (`docs/ROADMAP.md` M4 T5): list ordering follows
 * the contract's [Event.LIST_ORDER] unchanged, search filters title/notes/location case-insensitively,
 * hidden calendars are joined in (never dropped — this is a management view), and a blank title or
 * calendar name is carried through as-is for the screen to placeholder (`docs/contracts/Events.md` T5).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class EventListViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var formatter: IfcDateFormatter

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        formatter = IfcDateFormatter(ApplicationProvider.getApplicationContext<Context>().resources, Locale.US)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        repository: FakeEventRepository = FakeEventRepository(),
        handle: SavedStateHandle = SavedStateHandle(),
    ) = EventListViewModel(handle, repository, formatter)

    private fun TestScope.observe(viewModel: EventListViewModel): () -> EventListUiState.Loaded {
        backgroundScope.launch { viewModel.uiState.collect {} }
        return {
            runCurrent()
            viewModel.uiState.value.shouldBeInstanceOf<EventListUiState.Loaded>()
        }
    }

    @Test
    fun `the list follows the repository's own LIST_ORDER, unchanged`() =
        runTest(dispatcher) {
            val repository = FakeEventRepository()
            repository.seed(EventFixtures.all())
            val state = observe(viewModel(repository))

            state().items.map { it.eventId } shouldContainExactly repository.currentEvents.map { it.id }
        }

    @Test
    fun `a blank title and a blank calendar name are carried through, not replaced`() =
        runTest(dispatcher) {
            val repository = FakeEventRepository()
            repository.seed(listOf(EventFixtures.allDay(title = "")))
            val state = observe(viewModel(repository))

            val item = state().items.single()
            item.title shouldBe ""
            item.calendarName shouldBe ""
        }

    @Test
    fun `search matches the title, notes and location case-insensitively`() =
        runTest(dispatcher) {
            val repository = FakeEventRepository()
            repository.seed(
                listOf(
                    Event(
                        uid = "1",
                        title = "Team Standup",
                        timing = EventTiming.AllDay(LocalDate.of(2026, 1, 1)),
                    ),
                    Event(
                        uid = "2",
                        title = "Dentist",
                        description = "Bring the STANDUP notes",
                        timing = EventTiming.AllDay(LocalDate.of(2026, 1, 2)),
                    ),
                    Event(
                        uid = "3",
                        title = "Unrelated",
                        location = "standup room",
                        timing = EventTiming.AllDay(LocalDate.of(2026, 1, 3)),
                    ),
                    Event(
                        uid = "4",
                        title = "Nothing matches",
                        timing = EventTiming.AllDay(LocalDate.of(2026, 1, 4)),
                    ),
                ),
            )
            val viewModel = viewModel(repository)
            val state = observe(viewModel)

            viewModel.setQuery("standup")
            state().items shouldHaveSize 3
            state().items.map { it.title } shouldContainExactly listOf("Team Standup", "Dentist", "Unrelated")

            viewModel.setQuery("")
            state().items shouldHaveSize 4
        }

    @Test
    fun `events of a hidden calendar are joined in and flagged, not dropped`() =
        runTest(dispatcher) {
            val repository = FakeEventRepository()
            val hiddenId = repository.upsertCalendar(EventCalendar(name = "Hidden", visible = false))
            repository.seed(listOf(EventFixtures.allDay(calendarId = hiddenId)))
            val state = observe(viewModel(repository))

            val item = state().items.single()
            item.calendarHidden shouldBe true
        }

    @Test
    fun `the colour falls back from the event to its calendar to the default`() =
        runTest(dispatcher) {
            val repository = FakeEventRepository()
            val customId = repository.upsertCalendar(EventCalendar(name = "Work", colorArgb = 0xFF112233.toInt()))
            repository.seed(
                listOf(
                    EventFixtures.allDay(calendarId = customId, title = "Custom colour"),
                    EventFixtures.allDay(title = "Default colour"),
                ),
            )
            val state = observe(viewModel(repository))

            val items = state().items
            items.first { it.eventId == 1L }.calendarColorArgb shouldBe 0xFF112233.toInt()
            items.first { it.eventId == 2L }.calendarColorArgb shouldBe EventCalendar.DEFAULT_COLOR_ARGB
        }

    // docs/design-plan.md §4.5, §8 decision 5: rows group under IFC month headers, not Gregorian ones.

    @Test
    fun `two events in the same IFC month share one header across a Gregorian month boundary`() =
        runTest(dispatcher) {
            val repository = FakeEventRepository()
            repository.seed(
                listOf(
                    // Both IFC Sol 2026 (Gregorian June 18 - July 15), one on each side of the Gregorian
                    // month boundary.
                    EventFixtures.allDay(date = LocalDate.of(2026, 6, 20), title = "Late June"),
                    EventFixtures.allDay(date = LocalDate.of(2026, 7, 10), title = "Early July"),
                ),
            )
            val state = observe(viewModel(repository))

            val items = state().items
            items.map { it.monthHeaderKey }.distinct() shouldHaveSize 1
            items.map { it.monthHeaderLabel } shouldContainExactly listOf("Sol 2026", "Sol 2026")
        }

    @Test
    fun `a Year Day event gets its own header, distinct from December's regular days`() =
        runTest(dispatcher) {
            val repository = FakeEventRepository()
            repository.seed(
                listOf(
                    EventFixtures.allDay(date = LocalDate.of(2026, 12, 20), title = "December"),
                    EventFixtures.allDay(date = EventFixtures.YEAR_DAY_2026, title = "Year Day"),
                ),
            )
            val state = observe(viewModel(repository))

            val items = state().items
            items.first { it.title == "December" }.monthHeaderLabel shouldBe "December 2026"
            items.first { it.title == "Year Day" }.monthHeaderLabel shouldBe "Year Day, 2026"
            items.first { it.title == "December" }.monthHeaderKey shouldNotBe
                items.first { it.title == "Year Day" }.monthHeaderKey
        }

    @Test
    fun `a Leap Day event gets its own header, distinct from June's regular days`() =
        runTest(dispatcher) {
            val repository = FakeEventRepository()
            repository.seed(
                listOf(
                    EventFixtures.allDay(date = LocalDate.of(2024, 6, 1), title = "June"),
                    EventFixtures.allDay(date = EventFixtures.LEAP_DAY_2024, title = "Leap Day"),
                ),
            )
            val state = observe(viewModel(repository))

            val items = state().items
            items.first { it.title == "June" }.monthHeaderLabel shouldBe "June 2024"
            items.first { it.title == "Leap Day" }.monthHeaderLabel shouldBe "Leap Day, 2024"
            items.first { it.title == "June" }.monthHeaderKey shouldNotBe
                items.first { it.title == "Leap Day" }.monthHeaderKey
        }

    // docs/design-plan.md §5.4: the row carries the event's category for the list's chip.

    @Test
    fun `the row carries the event's category`() =
        runTest(dispatcher) {
            val repository = FakeEventRepository()
            repository.seed(listOf(EventFixtures.yearDayYearly())) // OBSERVANCE, per EventFixtures
            val state = observe(viewModel(repository))

            state().items.single().category shouldBe EventCategory.OBSERVANCE
        }

    @Test
    fun `search text survives process death`() =
        runTest(dispatcher) {
            val handle = SavedStateHandle()
            val repository = FakeEventRepository()
            repository.seed(listOf(EventFixtures.allDay(title = "Keep me")))
            viewModel(repository, handle).setQuery("keep")

            val restored = SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) })
            val state = observe(viewModel(repository, restored))

            state().query shouldBe "keep"
            state().items shouldHaveSize 1
        }

    // docs/ROADMAP.md M3 T4: the expanded-width list-detail pane's own selection — ignored at
    // compact/medium widths, where EventListRoute never reads it.

    @Test
    fun `selection starts at None`() =
        runTest(dispatcher) {
            viewModel().selection.value shouldBe EventListSelection.None
        }

    @Test
    fun `selecting an event is reflected in the selection`() =
        runTest(dispatcher) {
            val viewModel = viewModel()

            viewModel.selectEvent(42L)

            viewModel.selection.value shouldBe EventListSelection.Existing(42L)
        }

    @Test
    fun `selecting new event is reflected in the selection`() =
        runTest(dispatcher) {
            val viewModel = viewModel()

            viewModel.selectNewEvent()

            viewModel.selection.value shouldBe EventListSelection.New
        }

    @Test
    fun `clearing the selection resets it to None`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.selectEvent(42L)

            viewModel.clearSelection()

            viewModel.selection.value shouldBe EventListSelection.None
        }

    @Test
    fun `selecting a different event replaces rather than accumulates`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.selectEvent(1L)

            viewModel.selectEvent(2L)

            viewModel.selection.value shouldBe EventListSelection.Existing(2L)
        }
}
