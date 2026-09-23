package io.github.chrisjmendoza.yearal.feature.events.editor

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.chrisjmendoza.yearal.core.designsystem.format.IfcDateFormatter
import io.github.chrisjmendoza.yearal.core.domain.DateTicker
import io.github.chrisjmendoza.yearal.core.domain.event.Event
import io.github.chrisjmendoza.yearal.core.domain.event.EventCalendar
import io.github.chrisjmendoza.yearal.core.domain.event.EventCategory
import io.github.chrisjmendoza.yearal.core.domain.event.EventRepository
import io.github.chrisjmendoza.yearal.core.domain.event.IfcRecurrence
import io.github.chrisjmendoza.yearal.core.domain.event.IntercalaryDay
import io.github.chrisjmendoza.yearal.core.domain.event.LeapDayPolicy
import io.github.chrisjmendoza.yearal.core.domain.event.Recurrence
import io.github.chrisjmendoza.yearal.core.navigation.EventEditorKey
import io.github.chrisjmendoza.yearal.core.testing.EventFixtures
import io.github.chrisjmendoza.yearal.core.testing.FakeDateTicker
import io.github.chrisjmendoza.yearal.core.testing.FakeEventRepository
import io.github.chrisjmendoza.yearal.core.testing.FakeEventUidGenerator
import io.github.chrisjmendoza.yearal.core.testing.FakeZoneProvider
import io.github.chrisjmendoza.yearal.feature.events.notification.NotificationPermissionGate
import io.kotest.matchers.collections.shouldBeEmpty
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
import java.time.ZoneId
import java.util.Locale

/**
 * [EventEditorViewModel] against the fakes (`docs/contracts/Events.md` §7), written from ROADMAP M4 T4
 * and the frozen contract: recurrence options offered per date, the Leap Day policy asked only on a
 * real Leap Day, end-before-start rejected, length limits, stale-exdate dropping, the "today" default
 * crossing midnight, prefill validation, process death and fail-soft on a broken save.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class EventEditorViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var formatter: IfcDateFormatter
    private val specToday = LocalDate.of(2026, 9, 17)

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
        key: EventEditorKey = EventEditorKey(),
        repository: EventRepository = FakeEventRepository(),
        uidGenerator: FakeEventUidGenerator = FakeEventUidGenerator(),
        zoneProvider: FakeZoneProvider = FakeZoneProvider(ZoneId.of("UTC")),
        ticker: DateTicker = FakeDateTicker(specToday),
        handle: SavedStateHandle = SavedStateHandle(),
        notificationPermission: NotificationPermissionGate = FakeNotificationPermissionGate(needsPermission = false),
    ) = EventEditorViewModel(
        key,
        handle,
        repository,
        uidGenerator,
        zoneProvider,
        ticker,
        formatter,
        notificationPermission,
    )

    /** Keeps [viewModel] subscribed and returns a reader of its settled, loaded state. */
    private fun TestScope.observe(viewModel: EventEditorViewModel): () -> EventEditorUiState.Loaded {
        backgroundScope.launch { viewModel.uiState.collect {} }
        return {
            runCurrent()
            viewModel.uiState.value.shouldBeInstanceOf<EventEditorUiState.Loaded>()
        }
    }

    // ----- Default start follows today, and crosses midnight -----

    @Test
    fun `a new event with no prefill starts on today and follows it across midnight`() =
        runTest(dispatcher) {
            val ticker = FakeDateTicker(LocalDate.of(2026, 12, 31))
            val viewModel = viewModel(ticker = ticker)
            val state = observe(viewModel)

            state().startDate shouldBe LocalDate.of(2026, 12, 31)
            state().isNew shouldBe true

            ticker.set(LocalDate.of(2027, 1, 1))

            state().startDate shouldBe LocalDate.of(2027, 1, 1)
        }

    @Test
    fun `once the user picks a start date it no longer follows today`() =
        runTest(dispatcher) {
            val ticker = FakeDateTicker(specToday)
            val viewModel = viewModel(ticker = ticker)
            val state = observe(viewModel)

            viewModel.setStartDate(LocalDate.of(2026, 6, 30))
            ticker.set(specToday.plusDays(1))

            state().startDate shouldBe LocalDate.of(2026, 6, 30)
        }

    // ----- Prefill -----

    @Test
    fun `a valid prefill becomes the initial start date`() =
        runTest(dispatcher) {
            val prefill = LocalDate.of(2024, 6, 17)
            val state = observe(viewModel(key = EventEditorKey(prefillEpochDay = prefill.toEpochDay())))

            state().startDate shouldBe prefill
        }

    @Test
    fun `a prefill outside 1 to 9999 is ignored and today is used instead`() =
        runTest(dispatcher) {
            val state = observe(viewModel(key = EventEditorKey(prefillEpochDay = Long.MAX_VALUE)))

            state().startDate shouldBe specToday
        }

    // ----- Recurrence options offered per date -----

    @Test
    fun `monthly IFC is offered on a regular day and hidden on Year Day and Leap Day`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            val state = observe(viewModel)

            viewModel.setStartDate(EventFixtures.SOL_13_2026)
            state().monthlyIfcAvailable shouldBe true

            viewModel.setStartDate(EventFixtures.YEAR_DAY_2026)
            state().monthlyIfcAvailable shouldBe false

            viewModel.setStartDate(EventFixtures.LEAP_DAY_2024)
            state().monthlyIfcAvailable shouldBe false
        }

    @Test
    fun `the Leap Day policy is asked for only when the start is a real Leap Day`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            val state = observe(viewModel)
            viewModel.setRecurrenceKind(RecurrenceKind.YEARLY_IFC)

            viewModel.setStartDate(EventFixtures.SOL_13_2026)
            state().isLeapDayAnchor shouldBe false

            viewModel.setStartDate(EventFixtures.LEAP_DAY_2024)
            state().isLeapDayAnchor shouldBe true

            // 2025-06-17 is an ordinary day (June 28 in the IFC), not Leap Day.
            viewModel.setStartDate(LocalDate.of(2025, 6, 17))
            state().isLeapDayAnchor shouldBe false
        }

    // ----- end < start is rejected -----

    @Test
    fun `a timed end before the start blocks saving`() =
        runTest(dispatcher) {
            val repo = FakeEventRepository()
            val viewModel = viewModel(repository = repo)
            val state = observe(viewModel)
            viewModel.setAllDay(false)
            viewModel.setStartMinuteOfDay(10 * 60)
            viewModel.setEndMinuteOfDay(9 * 60)

            state().endBeforeStart shouldBe true
            state().canSave shouldBe false

            viewModel.save()
            runCurrent()
            repo.currentEvents shouldBe emptyList()
        }

    @Test
    fun `an all-day end date before the start blocks saving`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            val state = observe(viewModel)
            viewModel.setStartDate(LocalDate.of(2026, 6, 30))
            viewModel.setAllDayEndDate(LocalDate.of(2026, 6, 29))

            state().allDayEndBeforeStart shouldBe true
            state().canSave shouldBe false
        }

    @Test
    fun `an until date before the start blocks saving`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            val state = observe(viewModel)
            viewModel.setStartDate(LocalDate.of(2026, 6, 30))
            viewModel.setRecurrenceKind(RecurrenceKind.WEEKLY)
            viewModel.setRecurrenceEndKind(RecurrenceEndKind.UNTIL)
            viewModel.setUntilDate(LocalDate.of(2026, 1, 1))

            state().untilBeforeStart shouldBe true
            state().canSave shouldBe false
        }

    // ----- Length limits -----

    @Test
    fun `title, notes and location are clipped to the model's limits`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            val state = observe(viewModel)

            viewModel.setTitle("a".repeat(600))
            viewModel.setDescription("b".repeat(11_000))
            viewModel.setLocation("c".repeat(600))

            state().title.length shouldBe Event.MAX_TITLE_LENGTH
            state().description.length shouldBe Event.MAX_DESCRIPTION_LENGTH
            state().location.length shouldBe Event.MAX_LOCATION_LENGTH
        }

    // ----- Save: create, edit, delete -----

    @Test
    fun `saving a new event stores it with a fresh uid and navigates away`() =
        runTest(dispatcher) {
            val repo = FakeEventRepository()
            val uidGenerator = FakeEventUidGenerator()
            val viewModel = viewModel(repository = repo, uidGenerator = uidGenerator)
            observe(viewModel)
            runCurrent()
            viewModel.setTitle("Picnic")
            viewModel.setStartDate(LocalDate.of(2026, 6, 30))
            runCurrent()

            var navigated = false
            backgroundScope.launch {
                viewModel.editorEvents.collect {
                    if (it ==
                        EventEditorEvent.Saved
                    ) {
                        navigated = true
                    }
                }
            }
            viewModel.save()
            runCurrent()

            repo.currentEvents.single().title shouldBe "Picnic"
            repo.currentEvents.single().uid shouldBe "uid-1"
            navigated shouldBe true
        }

    @Test
    fun `editing an existing event keeps its id and uid`() =
        runTest(dispatcher) {
            val repo = FakeEventRepository()
            val stored = repo.seed(listOf(EventFixtures.sol13Yearly())).single()
            val viewModel = viewModel(key = EventEditorKey(eventId = stored.id), repository = repo)
            val state = observe(viewModel)
            state().title shouldBe stored.title

            viewModel.setTitle("Renamed")
            viewModel.save()
            runCurrent()

            val reloaded = repo.getEvent(stored.id)
            reloaded?.id shouldBe stored.id
            reloaded?.uid shouldBe stored.uid
            reloaded?.title shouldBe "Renamed"
        }

    @Test
    fun `an event that no longer exists shows NotFound`() =
        runTest(dispatcher) {
            val repo = FakeEventRepository()
            val viewModel = viewModel(key = EventEditorKey(eventId = 999), repository = repo)
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()

            viewModel.uiState.value shouldBe EventEditorUiState.NotFound
        }

    @Test
    fun `deleting an existing event removes it and navigates away`() =
        runTest(dispatcher) {
            val repo = FakeEventRepository()
            val stored = repo.seed(listOf(EventFixtures.sol13Yearly())).single()
            val viewModel = viewModel(key = EventEditorKey(eventId = stored.id), repository = repo)
            observe(viewModel)

            var deleted = false
            backgroundScope.launch {
                viewModel.editorEvents.collect {
                    if (it ==
                        EventEditorEvent.Deleted
                    ) {
                        deleted = true
                    }
                }
            }
            viewModel.requestDelete()
            runCurrent()
            viewModel.confirmDelete()
            runCurrent()

            repo.getEvent(stored.id) shouldBe null
            deleted shouldBe true
        }

    @Test
    fun `cancelling delete keeps the event and closes the dialog`() =
        runTest(dispatcher) {
            val repo = FakeEventRepository()
            val stored = repo.seed(listOf(EventFixtures.sol13Yearly())).single()
            val viewModel = viewModel(key = EventEditorKey(eventId = stored.id), repository = repo)
            val state = observe(viewModel)

            viewModel.requestDelete()
            state().showDeleteConfirm shouldBe true
            viewModel.cancelDelete()
            state().showDeleteConfirm shouldBe false
            repo.getEvent(stored.id) shouldBe stored
        }

    // ----- Unsaved-changes guard -----

    @Test
    fun `back with no changes leaves at once`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            observe(viewModel)
            var left = false
            backgroundScope.launch {
                viewModel.editorEvents.collect {
                    if (it ==
                        EventEditorEvent.NavigatedAway
                    ) {
                        left = true
                    }
                }
            }

            viewModel.requestBack()
            runCurrent()

            left shouldBe true
        }

    @Test
    fun `back after an edit shows the discard guard instead of leaving`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            val state = observe(viewModel)
            state()
            viewModel.setTitle("Something")
            runCurrent()

            var left = false
            backgroundScope.launch {
                viewModel.editorEvents.collect {
                    if (it ==
                        EventEditorEvent.NavigatedAway
                    ) {
                        left = true
                    }
                }
            }
            viewModel.requestBack()
            runCurrent()

            left shouldBe false
            state().showDiscardConfirm shouldBe true

            viewModel.confirmDiscard()
            runCurrent()
            left shouldBe true
        }

    // ----- Stale exdates dropped on save when the start or the rule changes -----

    @Test
    fun `saving after moving the start drops stale exdates`() =
        runTest(dispatcher) {
            val repo = FakeEventRepository()
            val withExdate = EventFixtures.sol13Yearly().copy(exdates = setOf(LocalDate.of(2027, 6, 30)))
            val stored = repo.seed(listOf(withExdate)).single()
            val viewModel = viewModel(key = EventEditorKey(eventId = stored.id), repository = repo)
            observe(viewModel)
            runCurrent()

            // Also moves the (otherwise stale) one-day all-day end date along with the start, so this
            // test is purely about the exdate-dropping rule, not the separate end-before-start guard.
            viewModel.setStartDate(LocalDate.of(2026, 7, 1))
            viewModel.setAllDayEndDate(LocalDate.of(2026, 7, 1))
            runCurrent()
            viewModel.save()
            runCurrent()

            repo.getEvent(stored.id)?.exdates shouldBe emptySet()
        }

    // ----- Fail soft: the event was deleted elsewhere before save -----

    @Test
    fun `saving an event deleted elsewhere fails soft and keeps the draft`() =
        runTest(dispatcher) {
            val repo = FakeEventRepository()
            val stored = repo.seed(listOf(EventFixtures.sol13Yearly())).single()
            val viewModel = viewModel(key = EventEditorKey(eventId = stored.id), repository = repo)
            val state = observe(viewModel)
            state()
            viewModel.setTitle("Still editing")
            runCurrent()
            repo.deleteEvent(stored.id)

            viewModel.save()
            runCurrent()

            state().saveFailed shouldBe true
            state().title shouldBe "Still editing"
        }

    // ----- Process death -----

    @Test
    fun `a new event's draft survives process death`() =
        runTest(dispatcher) {
            val handle = SavedStateHandle()
            val first = viewModel(handle = handle)
            observe(first)
            first.setTitle("Half-typed")
            first.setStartDate(LocalDate.of(2026, 6, 30))
            first.setRecurrenceKind(RecurrenceKind.YEARLY_IFC)
            runCurrent()

            val restored = SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) })
            val second = viewModel(handle = restored)
            val state = observe(second)

            state().title shouldBe "Half-typed"
            state().startDate shouldBe LocalDate.of(2026, 6, 30)
            state().recurrenceKind shouldBe RecurrenceKind.YEARLY_IFC
        }

    @Test
    fun `editing an existing event survives process death with the edited draft, not the stored one`() =
        runTest(dispatcher) {
            val repo = FakeEventRepository()
            val stored = repo.seed(listOf(EventFixtures.sol13Yearly())).single()
            val handle = SavedStateHandle()
            val first = viewModel(key = EventEditorKey(eventId = stored.id), repository = repo, handle = handle)
            observe(first)
            runCurrent()
            first.setTitle("Edited before death")
            runCurrent()

            val restoredHandle = SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) })
            val second =
                viewModel(key = EventEditorKey(eventId = stored.id), repository = repo, handle = restoredHandle)
            val state = observe(second)

            state().title shouldBe "Edited before death"
        }

    // ----- Leap Day policy carried through to the saved event -----

    @Test
    fun `each Leap Day policy is saved on the built event`() =
        runTest(dispatcher) {
            for (policy in LeapDayPolicy.entries) {
                val repo = FakeEventRepository()
                val viewModel = viewModel(repository = repo)
                observe(viewModel)
                runCurrent()
                viewModel.setStartDate(EventFixtures.LEAP_DAY_2024)
                viewModel.setRecurrenceKind(RecurrenceKind.YEARLY_IFC)
                viewModel.setLeapDayPolicy(policy)
                runCurrent()
                viewModel.save()
                runCurrent()

                val saved = repo.currentEvents.single()
                val rule = saved.recurrence as IfcRecurrence.YearlyOnIntercalary
                val day = rule.day as IntercalaryDay.LeapDay
                day.commonYearPolicy shouldBe policy
            }
        }

    // ----- Calendar id is preserved from the loaded event -----

    @Test
    fun `a new event uses the built-in calendar`() =
        runTest(dispatcher) {
            val repo = FakeEventRepository()
            val viewModel = viewModel(repository = repo)
            observe(viewModel)
            runCurrent()
            viewModel.setStartDate(LocalDate.of(2026, 6, 30))
            runCurrent()
            viewModel.save()
            runCurrent()

            repo.currentEvents.single().calendarId shouldBe EventCalendar.DEFAULT_ID
        }

    // ----- docs/design-plan.md §5.4: per-event colour and category -----

    @Test
    fun `setColor and setCategory update the loaded state and the saved event`() =
        runTest(dispatcher) {
            val repo = FakeEventRepository()
            val viewModel = viewModel(repository = repo)
            val state = observe(viewModel)
            viewModel.setStartDate(LocalDate.of(2026, 6, 30))
            state().colorArgb shouldBe null
            state().category shouldBe EventCategory.EVENT

            viewModel.setColor(0xFF993A7A.toInt())
            viewModel.setCategory(EventCategory.BIRTHDAY)

            state().colorArgb shouldBe 0xFF993A7A.toInt()
            state().category shouldBe EventCategory.BIRTHDAY

            viewModel.save()
            runCurrent()

            val saved = repo.currentEvents.single()
            saved.colorArgb shouldBe 0xFF993A7A.toInt()
            saved.category shouldBe EventCategory.BIRTHDAY
        }

    @Test
    fun `choosing Calendar colour resets colorArgb to null, even after an earlier choice`() =
        runTest(dispatcher) {
            val repo = FakeEventRepository()
            val viewModel = viewModel(repository = repo)
            val state = observe(viewModel)
            viewModel.setStartDate(LocalDate.of(2026, 6, 30))
            viewModel.setColor(0xFF1E6962.toInt())
            state().colorArgb shouldBe 0xFF1E6962.toInt()

            viewModel.setColor(null)

            state().colorArgb shouldBe null

            viewModel.save()
            runCurrent()

            repo.currentEvents.single().colorArgb shouldBe null
        }

    @Test
    fun `the chosen colour and category survive process death`() =
        runTest(dispatcher) {
            val handle = SavedStateHandle()
            val first = viewModel(handle = handle)
            observe(first)
            first.setColor(0xFFF28C28.toInt())
            first.setCategory(EventCategory.OBSERVANCE)
            runCurrent()

            val restored = SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) })
            val second = viewModel(handle = restored)
            val state = observe(second)

            state().colorArgb shouldBe 0xFFF28C28.toInt()
            state().category shouldBe EventCategory.OBSERVANCE
        }

    @Test
    fun `editing an existing event loads its own colour and category`() =
        runTest(dispatcher) {
            val repo = FakeEventRepository()
            val stored =
                repo
                    .seed(listOf(EventFixtures.sol13Yearly().copy(colorArgb = 0xFF554BCB.toInt())))
                    .single()
            val viewModel = viewModel(key = EventEditorKey(eventId = stored.id), repository = repo)
            val state = observe(viewModel)

            state().colorArgb shouldBe 0xFF554BCB.toInt()
            state().category shouldBe stored.category
        }

    // ----- POST_NOTIFICATIONS state machine (FEATURES E4, P2): first chip -> request; granted; denied
    // -> explanation, save still works; second chip -> no re-prompt; below API 33 -> no request.

    @Test
    fun `the first reminder chip requests POST_NOTIFICATIONS when the device needs the runtime grant`() =
        runTest(dispatcher) {
            val viewModel = viewModel(notificationPermission = FakeNotificationPermissionGate(needsPermission = true))
            observe(viewModel)
            runCurrent()
            val requests = mutableListOf<EventEditorEvent>()
            backgroundScope.launch { viewModel.editorEvents.collect { requests += it } }

            viewModel.toggleReminder(0)
            runCurrent()

            requests shouldBe listOf(EventEditorEvent.RequestNotificationPermission)
        }

    @Test
    fun `a second reminder chip does not request permission again`() =
        runTest(dispatcher) {
            val viewModel = viewModel(notificationPermission = FakeNotificationPermissionGate(needsPermission = true))
            observe(viewModel)
            runCurrent()
            val requests = mutableListOf<EventEditorEvent>()
            backgroundScope.launch { viewModel.editorEvents.collect { requests += it } }

            viewModel.toggleReminder(0)
            runCurrent()
            viewModel.toggleReminder(1440)
            runCurrent()

            requests shouldBe listOf(EventEditorEvent.RequestNotificationPermission)
        }

    @Test
    fun `removing then re-adding a chip does not request permission again either`() =
        runTest(dispatcher) {
            val viewModel = viewModel(notificationPermission = FakeNotificationPermissionGate(needsPermission = true))
            observe(viewModel)
            runCurrent()
            val requests = mutableListOf<EventEditorEvent>()
            backgroundScope.launch { viewModel.editorEvents.collect { requests += it } }

            viewModel.toggleReminder(0) // add
            runCurrent()
            viewModel.toggleReminder(0) // remove
            runCurrent()
            viewModel.toggleReminder(0) // add again
            runCurrent()

            requests shouldBe listOf(EventEditorEvent.RequestNotificationPermission)
        }

    @Test
    fun `below API 33 no permission is ever requested`() =
        runTest(dispatcher) {
            val viewModel = viewModel(notificationPermission = FakeNotificationPermissionGate(needsPermission = false))
            observe(viewModel)
            runCurrent()
            val requests = mutableListOf<EventEditorEvent>()
            backgroundScope.launch { viewModel.editorEvents.collect { requests += it } }

            viewModel.toggleReminder(0)
            runCurrent()

            requests.shouldBeEmpty()
        }

    @Test
    fun `denial shows a dismissible notice and never blocks saving, and granting clears it`() =
        runTest(dispatcher) {
            val repo = FakeEventRepository()
            val viewModel = viewModel(repository = repo, notificationPermission = FakeNotificationPermissionGate(true))
            val state = observe(viewModel)
            viewModel.setTitle("Reminder test")
            viewModel.setStartDate(LocalDate.of(2026, 6, 30))
            viewModel.toggleReminder(0)
            runCurrent()

            viewModel.onNotificationPermissionResult(granted = false)
            state().showNotificationPermissionNotice shouldBe true

            viewModel.save()
            runCurrent()
            repo.currentEvents.single().title shouldBe "Reminder test"

            viewModel.dismissNotificationPermissionNotice()
            state().showNotificationPermissionNotice shouldBe false

            viewModel.onNotificationPermissionResult(granted = true)
            state().showNotificationPermissionNotice shouldBe false
        }

    // ----- Individually deleted occurrences ("delete this occurrence" from Day detail): count and restore-all -----

    @Test
    fun `exdateCount reflects the loaded event, and restoreAllOccurrences clears every exdate`() =
        runTest(dispatcher) {
            val repo = FakeEventRepository()
            val withExdates =
                EventFixtures
                    .sol13Yearly()
                    .copy(exdates = setOf(LocalDate.of(2027, 6, 30), LocalDate.of(2028, 6, 30)))
            val stored = repo.seed(listOf(withExdates)).single()
            val viewModel = viewModel(key = EventEditorKey(eventId = stored.id), repository = repo)
            val state = observe(viewModel)

            state().exdateCount shouldBe 2

            viewModel.restoreAllOccurrences()
            runCurrent()

            state().exdateCount shouldBe 0
            repo
                .getEvent(stored.id)
                ?.exdates
                .orEmpty()
                .shouldBeEmpty()
        }

    @Test
    fun `restoreAllOccurrences is a no-op for a new event or one with nothing to restore`() =
        runTest(dispatcher) {
            val repo = FakeEventRepository()
            val stored = repo.seed(listOf(EventFixtures.sol13Yearly())).single()
            val viewModel = viewModel(key = EventEditorKey(eventId = stored.id), repository = repo)
            val state = observe(viewModel)
            state().exdateCount shouldBe 0

            viewModel.restoreAllOccurrences()
            runCurrent()

            state().exdateCount shouldBe 0
            repo.getEvent(stored.id) shouldBe stored
        }

    // ----- ROADMAP R2: rapid Save taps must never create a duplicate event, and the in-flight guard
    // covers delete too.

    @Test
    fun `two rapid saves of a new event store exactly one event`() =
        runTest(dispatcher) {
            // A counting wrapper, not just the final row count: a stable uid alone would also collapse
            // two concurrent inserts down to one stored row (the second insert would hit the contract's
            // duplicate-uid check and fail soft) — that is a legitimate second line of defence, but this
            // test is about the isSaving guard specifically, so it asserts upsertEvent is called once.
            val repo = CountingEventRepository()
            val viewModel = viewModel(repository = repo)
            val state = observe(viewModel)
            viewModel.setTitle("Picnic")
            viewModel.setStartDate(LocalDate.of(2026, 6, 30))
            state()

            viewModel.save()
            viewModel.save() // no-op: a save is already in flight (EditorFlags.isSaving)
            runCurrent()

            repo.upsertCount shouldBe 1
            state().saveFailed shouldBe false
            repo.fake.currentEvents.size shouldBe 1
            repo.fake.currentEvents
                .single()
                .title shouldBe "Picnic"
        }

    @Test
    fun `a save that fails can be retried and still stores exactly one event, under the same uid`() =
        runTest(dispatcher) {
            val repo = FakeEventRepository()
            // The very first uid FakeEventUidGenerator draws is "uid-1" — seed a conflicting event under
            // that uid so the first save fails the contract's duplicate-uid check, deterministically.
            val conflicting = repo.seed(listOf(EventFixtures.allDay().copy(uid = "uid-1"))).single()
            val viewModel = viewModel(repository = repo)
            val state = observe(viewModel)
            viewModel.setTitle("Picnic")
            viewModel.setStartDate(LocalDate.of(2026, 6, 30))
            state()

            viewModel.save()
            runCurrent()

            state().saveFailed shouldBe true
            state().isSaving shouldBe false
            repo.currentEvents.size shouldBe 1 // only the pre-existing conflicting event

            repo.deleteEvent(conflicting.id)
            viewModel.save()
            runCurrent()

            state().saveFailed shouldBe false
            repo.currentEvents.size shouldBe 1
            repo.currentEvents.single().title shouldBe "Picnic"
            repo.currentEvents.single().uid shouldBe "uid-1"
        }

    @Test
    fun `a new event's uid survives process death, before any save, and no second uid is ever drawn`() =
        runTest(dispatcher) {
            val handle = SavedStateHandle()
            val firstUidGenerator = FakeEventUidGenerator()
            val first = viewModel(handle = handle, uidGenerator = firstUidGenerator)
            observe(first)
            runCurrent()

            firstUidGenerator.issued shouldBe 1 // drawn once, eagerly, at construction — never at save time

            val restoredHandle = SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) })
            val secondUidGenerator = FakeEventUidGenerator()
            val repo = FakeEventRepository()
            val second = viewModel(handle = restoredHandle, uidGenerator = secondUidGenerator, repository = repo)
            val state = observe(second)
            second.setTitle("Restored after death")
            second.setStartDate(LocalDate.of(2026, 6, 30))
            state()
            second.save()
            runCurrent()

            secondUidGenerator.issued shouldBe 0 // never consulted: the persisted uid was reused
            repo.currentEvents.single().uid shouldBe "uid-1" // the FIRST generator's draw, from before death
        }

    @Test
    fun `two rapid saves while editing an existing event produce one update, not a duplicate`() =
        runTest(dispatcher) {
            // A stable id makes two concurrent writes to an existing event both land as updates to the
            // same row regardless of the guard, so the row count alone cannot tell the two apart — the
            // counting wrapper is what actually pins down that the second save() was a no-op.
            val repo = CountingEventRepository()
            val stored = repo.fake.seed(listOf(EventFixtures.sol13Yearly())).single()
            val viewModel = viewModel(key = EventEditorKey(eventId = stored.id), repository = repo)
            val state = observe(viewModel)
            state()
            viewModel.setTitle("Renamed")
            state()

            viewModel.save()
            viewModel.save() // no-op: a save is already in flight
            runCurrent()

            repo.upsertCount shouldBe 1
            repo.fake.currentEvents.size shouldBe 1
            repo.fake.getEvent(stored.id)?.title shouldBe "Renamed"
        }

    @Test
    fun `a second save after the first landed updates the same row instead of inserting another`() =
        runTest(dispatcher) {
            val repo = FakeEventRepository()
            val viewModel = viewModel(repository = repo)
            val state = observe(viewModel)
            viewModel.setTitle("First")
            viewModel.setStartDate(LocalDate.of(2026, 6, 30))
            state()
            viewModel.save()
            runCurrent()
            repo.currentEvents.size shouldBe 1

            viewModel.setTitle("Second")
            state()
            viewModel.save()
            runCurrent()

            repo.currentEvents.size shouldBe 1
            repo.currentEvents.single().title shouldBe "Second"
        }

    @Test
    fun `requestDelete and confirmDelete are no-ops while a save is in flight`() =
        runTest(dispatcher) {
            val repo = FakeEventRepository()
            val stored = repo.seed(listOf(EventFixtures.sol13Yearly())).single()
            val viewModel = viewModel(key = EventEditorKey(eventId = stored.id), repository = repo)
            val state = observe(viewModel)
            viewModel.setTitle("Editing")
            state()
            viewModel.save() // leaves isSaving true until runCurrent processes it

            viewModel.requestDelete()
            state().showDeleteConfirm shouldBe false

            runCurrent()
            repo.getEvent(stored.id) shouldNotBe null
        }

    // ----- ROADMAP R3: moving the start to Year Day or Leap Day while "monthly (IFC)" is chosen must
    // reset the choice visibly, never silently save a one-off.

    @Test
    fun `moving the start to Year Day while monthly IFC is chosen resets it and shows the notice`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            val state = observe(viewModel)
            viewModel.setStartDate(EventFixtures.SOL_13_2026)
            viewModel.setRecurrenceKind(RecurrenceKind.MONTHLY_IFC)
            state().recurrenceKind shouldBe RecurrenceKind.MONTHLY_IFC
            state().showRecurrenceResetNotice shouldBe false

            viewModel.setStartDate(EventFixtures.YEAR_DAY_2026)

            state().recurrenceKind shouldBe RecurrenceKind.NONE
            state().showRecurrenceResetNotice shouldBe true
        }

    @Test
    fun `moving the start to Leap Day while monthly IFC is chosen resets it too`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            val state = observe(viewModel)
            viewModel.setStartDate(EventFixtures.SOL_13_2026)
            viewModel.setRecurrenceKind(RecurrenceKind.MONTHLY_IFC)

            viewModel.setStartDate(EventFixtures.LEAP_DAY_2024)

            state().recurrenceKind shouldBe RecurrenceKind.NONE
            state().showRecurrenceResetNotice shouldBe true
        }

    @Test
    fun `choosing monthly IFC directly on an intercalary start is reset just the same`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            val state = observe(viewModel)
            viewModel.setStartDate(EventFixtures.YEAR_DAY_2026)

            viewModel.setRecurrenceKind(RecurrenceKind.MONTHLY_IFC)

            state().recurrenceKind shouldBe RecurrenceKind.NONE
            state().showRecurrenceResetNotice shouldBe true
        }

    @Test
    fun `dismissing the recurrence reset notice hides it`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            val state = observe(viewModel)
            viewModel.setStartDate(EventFixtures.SOL_13_2026)
            viewModel.setRecurrenceKind(RecurrenceKind.MONTHLY_IFC)
            viewModel.setStartDate(EventFixtures.YEAR_DAY_2026)
            state().showRecurrenceResetNotice shouldBe true

            viewModel.dismissRecurrenceResetNotice()

            state().showRecurrenceResetNotice shouldBe false
        }

    @Test
    fun `saving after the automatic reset stores a non-recurring event, never a crash`() =
        runTest(dispatcher) {
            val repo = FakeEventRepository()
            val viewModel = viewModel(repository = repo)
            val state = observe(viewModel)
            viewModel.setTitle("Reset test")
            viewModel.setStartDate(EventFixtures.SOL_13_2026)
            viewModel.setRecurrenceKind(RecurrenceKind.MONTHLY_IFC)
            viewModel.setStartDate(EventFixtures.YEAR_DAY_2026)
            state()

            viewModel.save()
            runCurrent()

            repo.currentEvents.single().recurrence shouldBe Recurrence.None
        }

    @Test
    fun `an undismissed recurrence reset notice survives process death, and a dismissed one stays dismissed`() =
        runTest(dispatcher) {
            val handle = SavedStateHandle()
            val first = viewModel(handle = handle)
            observe(first)
            first.setStartDate(EventFixtures.SOL_13_2026)
            first.setRecurrenceKind(RecurrenceKind.MONTHLY_IFC)
            first.setStartDate(EventFixtures.YEAR_DAY_2026)
            runCurrent()

            val restoredHandle = SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) })
            val second = viewModel(handle = restoredHandle)
            val secondState = observe(second)
            secondState().showRecurrenceResetNotice shouldBe true

            second.dismissRecurrenceResetNotice()

            val restoredHandle2 = SavedStateHandle(restoredHandle.keys().associateWith { restoredHandle.get<Any?>(it) })
            val third = viewModel(handle = restoredHandle2)
            val thirdState = observe(third)
            thirdState().showRecurrenceResetNotice shouldBe false
        }

    // ----- ROADMAP R3 sibling audits -----

    @Test
    fun `an until date that was valid blocks saving once a later start date moves past it`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            val state = observe(viewModel)
            viewModel.setStartDate(LocalDate.of(2026, 1, 1))
            viewModel.setRecurrenceKind(RecurrenceKind.WEEKLY)
            viewModel.setRecurrenceEndKind(RecurrenceEndKind.UNTIL)
            viewModel.setUntilDate(LocalDate.of(2026, 6, 1))
            state().canSave shouldBe true

            viewModel.setStartDate(LocalDate.of(2026, 12, 1)) // now after the until date

            state().untilBeforeStart shouldBe true
            state().canSave shouldBe false
        }

    @Test
    fun `the recurrence end condition is kept when the recurrence kind changes`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            val state = observe(viewModel)
            viewModel.setStartDate(LocalDate.of(2026, 1, 5))
            viewModel.setRecurrenceKind(RecurrenceKind.WEEKLY)
            viewModel.setRecurrenceEndKind(RecurrenceEndKind.UNTIL)
            viewModel.setUntilDate(LocalDate.of(2030, 1, 1))

            viewModel.setRecurrenceKind(RecurrenceKind.YEARLY_GREGORIAN)

            state().recurrenceEndKind shouldBe RecurrenceEndKind.UNTIL
            state().untilDate shouldBe LocalDate.of(2030, 1, 1)
        }

    // ----- ROADMAP R4: the per-option explainer flags follow the effective start date -----

    @Test
    fun `the recurrence shift flags reflect the effective start date`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            val state = observe(viewModel)

            viewModel.setStartDate(LocalDate.of(2026, 6, 30)) // Sol 13: stable both ways
            state().yearlyIfcGregorianShifts shouldBe false
            state().yearlyGregorianIfcShifts shouldBe false

            viewModel.setStartDate(LocalDate.of(2026, 3, 1)) // inside both shifting ranges
            state().yearlyIfcGregorianShifts shouldBe true
            state().yearlyGregorianIfcShifts shouldBe true
        }
}

/** A settable [NotificationPermissionGate] for the permission state-machine tests. */
private class FakeNotificationPermissionGate(
    private val needsPermission: Boolean,
) : NotificationPermissionGate {
    override fun needsRuntimePermission(): Boolean = needsPermission
}

/**
 * Wraps a [FakeEventRepository] and counts [upsertEvent] calls (ROADMAP R2 tests): the row count alone
 * cannot always tell a true no-op second save from two writes that happen to converge on one row (a
 * stable id or uid can do that on its own), so these tests assert the call count directly.
 */
private class CountingEventRepository(
    val fake: FakeEventRepository = FakeEventRepository(),
) : EventRepository by fake {
    var upsertCount: Int = 0
        private set

    override suspend fun upsertEvent(event: Event): Long {
        upsertCount++
        return fake.upsertEvent(event)
    }
}
