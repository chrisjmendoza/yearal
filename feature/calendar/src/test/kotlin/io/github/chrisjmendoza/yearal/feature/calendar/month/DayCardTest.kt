package io.github.chrisjmendoza.yearal.feature.calendar.month

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.chrisjmendoza.yearal.core.designsystem.format.IfcDateFormatter
import io.github.chrisjmendoza.yearal.core.designsystem.theme.IfcTheme
import io.github.chrisjmendoza.yearal.feature.calendar.agenda.AgendaItemUi
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

/**
 * [DayCard] under Robolectric (the day-card merge — ported from the old `DayScreenTest`, which tested
 * the popup Day detail this card superseded). The card follows the Today hero's labelled-block rule
 * (`docs/design-plan.md` §4.1, §4.4; owner, 2026-09-27): an "IFC" eyebrow over the bare IFC weekday
 * ("no IFC weekday" on intercalary days, spec §4.1 item 5; spoken with both weekdays labelled, item 7)
 * and the IFC date as a heading, a "Gregorian" eyebrow over the real date with its real weekday, then
 * one facts line with the numeric form's `IFC` marker, day/week and quarter — each weekday drawn once.
 * Then the holiday names, the "Today" badge only on today, and an interactive agenda: tap opens the
 * event, long-press or its TalkBack action requests delete, with a confirmation dialog worded by
 * whether the row recurs (FEATURES E1).
 */
@RunWith(AndroidJUnit4::class)
// A tall window so the whole card, holidays included, is on screen; the content scrolls otherwise.
@Config(qualifiers = "w360dp-h900dp")
class DayCardTest {
    @get:Rule
    val compose = createComposeRule()

    private val formatter =
        IfcDateFormatter(ApplicationProvider.getApplicationContext<Context>().resources, Locale.US)

    private fun show(
        day: LocalDate,
        today: LocalDate = LocalDate.of(2026, 9, 17),
        holidays: List<String> = emptyList(),
        agenda: List<AgendaItemUi> = emptyList(),
        pendingDelete: AgendaItemUi? = null,
        onEventClick: (Long) -> Unit = {},
        onAddEvent: () -> Unit = {},
        onOpenInConverter: () -> Unit = {},
        onRequestDelete: (AgendaItemUi) -> Unit = {},
        onConfirmDelete: () -> Unit = {},
        onCancelDelete: () -> Unit = {},
        fontScale: Float = 1f,
    ) {
        val detail = buildDayDetailUi(day, today, formatter, holidays, agenda, pendingDelete)
        val state =
            MonthUiState(currentPage = 0, today = today, todayPage = 0, selected = day, dayDetail = detail)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                IfcTheme(dynamicColor = false) {
                    DayCard(
                        state = state,
                        onEventClick = onEventClick,
                        onAddEvent = onAddEvent,
                        onOpenInConverter = onOpenInConverter,
                        onRequestDelete = onRequestDelete,
                        onConfirmDelete = onConfirmDelete,
                        onCancelDelete = onCancelDelete,
                    )
                }
            }
        }
    }

    @Test
    fun `a regular day shows the labelled IFC and Gregorian blocks, each weekday once, and the facts line`() {
        show(LocalDate.of(2026, 9, 17))

        // The "IFC" block: eyebrow, the bare IFC weekday — spoken with both weekdays labelled on that
        // same line (spec §4.1 item 7) — and the IFC date as the card's heading.
        val ifcEyebrow = compose.onNodeWithText("IFC").assertIsDisplayed()
        val weekday =
            compose
                .onNode(hasText("Sunday").and(hasContentDescription("IFC Sunday, actual Thursday")))
                .assertIsDisplayed()
        val date =
            compose
                .onNode(hasText("September 8, 2026").and(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)))
                .assertIsDisplayed()
        // The "Gregorian" block carries the real weekday — the only place it is drawn.
        val gregorianEyebrow = compose.onNodeWithText("GREGORIAN").assertIsDisplayed()
        compose.onNodeWithText("Thursday, September 17, 2026").assertIsDisplayed()
        compose.onAllNodesWithText("Thursday").assertCountEquals(0)
        // No labelled second copy of either weekday (the former weekday bubble is gone), and no
        // "Gregorian:" prefix — the eyebrow labels the whole block.
        compose.onAllNodesWithText("IFC weekday: Sunday", useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodesWithText("Actual weekday: Thursday", useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodesWithText("Gregorian: ", substring = true).assertCountEquals(0)
        // The facts line: the numeric form keeps its IFC prefix (CLAUDE.md rule 5).
        compose.onNodeWithText("IFC 2026-10-08 · Day 260 · Week 38 of 52 · Q3").assertIsDisplayed()
        compose.onNodeWithText("Today").assertIsDisplayed()
        compose.onNodeWithText("No holidays on this day.").assertIsDisplayed()

        // Top to bottom: IFC eyebrow, IFC weekday, IFC date, Gregorian eyebrow.
        val tops = listOf(ifcEyebrow, weekday, date, gregorianEyebrow).map { it.getUnclippedBoundsInRoot().top }
        tops shouldBe tops.sorted()
    }

    @Test
    fun `Year Day shows no IFC weekday and its actual weekday`() {
        show(LocalDate.of(2026, 12, 31), holidays = listOf("Year Day", "New Year's Eve"))

        compose.onNodeWithText("Year Day, 2026").assertIsDisplayed()
        // No IFC weekday: the weekday slot says so (spec §4.1 item 5) rather than showing the real one,
        // and TalkBack still hears both labelled on that line (item 7).
        compose
            .onNode(hasText("no IFC weekday").and(hasContentDescription("no IFC weekday, actual Thursday")))
            .assertIsDisplayed()
        compose.onNodeWithText("Thursday, December 31, 2026").assertIsDisplayed()
        compose.onAllNodesWithText("Thursday").assertCountEquals(0)
        compose.onAllNodesWithText("Actual weekday: Thursday", useUnmergedTree = true).assertCountEquals(0)
        compose.onNodeWithText("IFC 2026-13-29 · Day 365 · outside the weeks · Q4").assertIsDisplayed()
        // The section eyebrow heading is shown uppercased, matching every other card section.
        compose.onNodeWithText("HOLIDAYS").assertIsDisplayed()
        compose.onNodeWithText("Year Day").assertIsDisplayed()
        compose.onNodeWithText("New Year's Eve").assertIsDisplayed()
        compose.onAllNodesWithText("Today").assertCountEquals(0)
    }

    // docs/design-plan.md §4.4: Year Day and Leap Day get the intercalary header instead of the plain
    // title row a regular day shows.

    @Test
    fun `Year Day shows the intercalary header, a regular day does not`() {
        show(LocalDate.of(2026, 12, 31))

        compose.onNodeWithTag(DAY_INTERCALARY_HEADER_TEST_TAG).assertIsDisplayed()
    }

    // The amber header wraps the IFC block only: the Gregorian block and the facts line sit on the card
    // itself, exactly as on a regular day.
    @Test
    fun `the intercalary header wraps the IFC block and nothing else`() {
        show(LocalDate.of(2026, 12, 31))

        val inHeader = hasAnyAncestor(hasTestTag(DAY_INTERCALARY_HEADER_TEST_TAG))
        compose.onNode(hasText("IFC").and(inHeader)).assertIsDisplayed()
        compose.onNode(hasText("no IFC weekday").and(inHeader)).assertIsDisplayed()
        compose.onNode(hasText("Year Day, 2026").and(inHeader)).assertIsDisplayed()
        compose.onNode(hasText("GREGORIAN").and(!inHeader)).assertIsDisplayed()
        compose.onNode(hasText("Thursday, December 31, 2026").and(!inHeader)).assertIsDisplayed()
        compose.onNode(hasText("IFC 2026-13-29", substring = true).and(!inHeader)).assertIsDisplayed()
    }

    @Test
    fun `Leap Day shows no IFC weekday and can be today`() {
        show(LocalDate.of(2028, 6, 17), today = LocalDate.of(2028, 6, 17), holidays = listOf("Leap Day"))

        compose.onNodeWithText("Leap Day, 2028").assertIsDisplayed()
        compose
            .onNode(hasText("no IFC weekday").and(hasContentDescription("no IFC weekday, actual Saturday")))
            .assertIsDisplayed()
        compose.onNodeWithText("Saturday, June 17, 2028").assertIsDisplayed()
        compose.onAllNodesWithText("Saturday").assertCountEquals(0)
        compose.onAllNodesWithText("Actual weekday: Saturday", useUnmergedTree = true).assertCountEquals(0)
        compose.onNodeWithText("IFC 2028-06-29 · Day 169 · outside the weeks · Q2").assertIsDisplayed()
        // The Today badge sits at the end of the IFC eyebrow row, inside the amber header.
        compose
            .onNode(hasText("Today").and(hasAnyAncestor(hasTestTag(DAY_INTERCALARY_HEADER_TEST_TAG))))
            .assertIsDisplayed()
        compose.onNodeWithText("Leap Day").assertIsDisplayed()
        compose.onNodeWithTag(DAY_INTERCALARY_HEADER_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `a regular day does not show the intercalary header`() {
        show(LocalDate.of(2026, 9, 17))

        compose.onAllNodesWithTag(DAY_INTERCALARY_HEADER_TEST_TAG).assertCountEquals(0)
    }

    @Test
    fun `an observed holiday is rendered with its label`() {
        show(LocalDate.of(2026, 7, 3), holidays = listOf("Independence Day (observed)"))

        compose.onNodeWithText("Sol 16, 2026").assertIsDisplayed()
        compose.onNodeWithText("Independence Day (observed)").assertIsDisplayed()
    }

    // FEATURES C5: the day's agenda, all-day first then by start time, tap navigates by id only.

    @Test
    fun `the day's agenda shows all-day and timed entries, and tapping one reports its id`() {
        val agenda =
            listOf(
                AgendaItemUi(
                    eventId = 1,
                    title = "Conference",
                    isAllDay = true,
                    startTime = null,
                    endTime = null,
                    colorArgb = 0xFF123F3D.toInt(),
                ),
                AgendaItemUi(
                    eventId = 2,
                    title = "Standup",
                    isAllDay = false,
                    startTime = LocalTime.of(9, 0),
                    endTime = LocalTime.of(9, 30),
                    colorArgb = 0xFF123F3D.toInt(),
                ),
            )
        val clicked = mutableListOf<Long>()
        show(LocalDate.of(2026, 9, 17), agenda = agenda, onEventClick = { clicked += it })

        compose.onNodeWithText("EVENTS").assertIsDisplayed()
        compose.onNodeWithText("Conference").assertIsDisplayed()
        compose.onNodeWithText("All day").assertIsDisplayed()
        compose.onNodeWithText("Standup").performClick()

        clicked shouldBe listOf(2L)
    }

    @Test
    fun `a blank title shows the untitled placeholder`() {
        val agenda =
            listOf(
                AgendaItemUi(
                    eventId = 1,
                    title = "",
                    isAllDay = true,
                    startTime = null,
                    endTime = null,
                    colorArgb = 0xFF123F3D.toInt(),
                ),
            )
        show(LocalDate.of(2026, 9, 17), agenda = agenda)

        compose.onNodeWithText("(No title)").assertIsDisplayed()
    }

    @Test
    fun `the Add event action invokes its callback`() {
        var added = 0
        show(LocalDate.of(2026, 9, 17), onAddEvent = { added++ })

        compose.onNodeWithText("Add event").performClick()

        added shouldBe 1
    }

    // FEATURES D1: "Open in converter" (docs/ROADMAP.md M3 T5).

    @Test
    fun `the Open in converter action invokes its callback`() {
        var opened = 0
        show(LocalDate.of(2026, 9, 17), onOpenInConverter = { opened++ })

        compose.onNodeWithText("Open in converter").performClick()

        opened shouldBe 1
    }

    // docs/ARCHITECTURE.md §4 "Accessibility": 200% font scale, 48dp touch targets.
    @Test
    fun `at 200 percent font scale the agenda row and Add event keep their touch target`() {
        val agenda =
            listOf(
                AgendaItemUi(
                    eventId = 1,
                    title = "Conference",
                    isAllDay = true,
                    startTime = null,
                    endTime = null,
                    colorArgb = 0xFF123F3D.toInt(),
                ),
            )
        show(LocalDate.of(2026, 9, 17), agenda = agenda, fontScale = 2f)

        compose.onNodeWithText("Conference").assertIsDisplayed()
        compose.onNodeWithText("Add event").assertHeightIsAtLeast(48.dp)
    }

    // a11y audit finding #5: AgendaRow's vertical padding must floor it at 48dp, same as
    // TodayScreen.kt's TodayAgendaRow, at both the default and 200% font scale.

    @Test
    fun `an agenda row's touch target is at least 48dp tall`() {
        val agenda =
            listOf(
                AgendaItemUi(
                    eventId = 1,
                    title = "Conference",
                    isAllDay = true,
                    startTime = null,
                    endTime = null,
                    colorArgb = 0xFF123F3D.toInt(),
                ),
            )
        show(LocalDate.of(2026, 9, 17), agenda = agenda)

        // The row's mergeDescendants semantics make "Conference" resolve to the row itself, same as
        // the existing long-press test above.
        compose.onNodeWithText("Conference").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun `an agenda row's touch target is still at least 48dp tall at 200 percent font scale`() {
        val agenda =
            listOf(
                AgendaItemUi(
                    eventId = 1,
                    title = "Conference",
                    isAllDay = true,
                    startTime = null,
                    endTime = null,
                    colorArgb = 0xFF123F3D.toInt(),
                ),
            )
        show(LocalDate.of(2026, 9, 17), agenda = agenda, fontScale = 2f)

        compose.onNodeWithText("Conference").assertHeightIsAtLeast(48.dp)
    }

    // The popup Day detail's Loading/Unavailable states are gone with it (docs/ROADMAP.md, the day-card
    // merge): an invalid MonthKey.selectedEpochDay now just fails soft to no selection (MonthPages.selectedDateOf),
    // so the card only ever has two states — no day yet (before the first date tick) or a real one.

    @Test
    fun `the card shows a loading message when there is no day yet`() {
        compose.setContent {
            IfcTheme(dynamicColor = false) {
                DayCard(
                    state = MonthUiState(currentPage = 0, today = null, todayPage = null, selected = null),
                    onEventClick = {},
                    onAddEvent = {},
                    onOpenInConverter = {},
                    onRequestDelete = {},
                    onConfirmDelete = {},
                    onCancelDelete = {},
                )
            }
        }

        compose.onNodeWithText("Loading…").assertIsDisplayed()
        compose.onAllNodesWithText("GREGORIAN").assertCountEquals(0)
        compose.onAllNodesWithText("IFC 20", substring = true).assertCountEquals(0)
    }

    // FEATURES E1: "delete this occurrence" and plain delete, with a TalkBack-reachable action.

    private val recurringItem =
        AgendaItemUi(
            eventId = 5,
            title = "Sol 13 picnic",
            isAllDay = true,
            startTime = null,
            endTime = null,
            colorArgb = 0xFF123F3D.toInt(),
            isRecurring = true,
            occurrenceDate = LocalDate.of(2026, 6, 30),
        )

    private val oneOffItem =
        AgendaItemUi(
            eventId = 6,
            title = "Once",
            isAllDay = true,
            startTime = null,
            endTime = null,
            colorArgb = 0xFF123F3D.toInt(),
            isRecurring = false,
            occurrenceDate = LocalDate.of(2026, 9, 17),
        )

    @Test
    fun `long-pressing a recurring row requests its delete`() {
        val requested = mutableListOf<AgendaItemUi>()
        show(LocalDate.of(2026, 9, 17), agenda = listOf(recurringItem), onRequestDelete = { requested += it })

        compose.onNodeWithText("Sol 13 picnic").performTouchInput { longClick() }

        requested shouldBe listOf(recurringItem)
    }

    @Test
    fun `a row's TalkBack custom action requests the same delete as long-press`() {
        val requested = mutableListOf<AgendaItemUi>()
        show(LocalDate.of(2026, 9, 17), agenda = listOf(oneOffItem), onRequestDelete = { requested += it })

        val actionLabel = "Delete event"
        compose
            .onNodeWithText("Once")
            .performSemanticsAction(actionLabel)

        requested shouldBe listOf(oneOffItem)
    }

    @Test
    fun `a recurring row's confirmation offers delete-this-occurrence wording`() {
        var confirmed = 0
        var cancelled = 0
        show(
            LocalDate.of(2026, 9, 17),
            agenda = listOf(recurringItem),
            pendingDelete = recurringItem,
            onConfirmDelete = { confirmed++ },
            onCancelDelete = { cancelled++ },
        )

        compose.onNodeWithText("Delete this occurrence?").assertIsDisplayed()
        compose.onNodeWithText("Delete this occurrence").performClick()
        confirmed shouldBe 1
        cancelled shouldBe 0
    }

    @Test
    fun `a non-recurring row's confirmation offers a plain, permanent delete`() {
        var confirmed = 0
        show(
            LocalDate.of(2026, 9, 17),
            agenda = listOf(oneOffItem),
            pendingDelete = oneOffItem,
            onConfirmDelete = { confirmed++ },
        )

        compose.onNodeWithText("Delete event?").assertIsDisplayed()
        compose.onNodeWithText("This deletes the whole event. This can't be undone.").assertIsDisplayed()
        compose.onNodeWithText("Delete event").performClick()
        confirmed shouldBe 1
    }

    @Test
    fun `cancelling the delete confirmation invokes onCancelDelete, not onConfirmDelete`() {
        var confirmed = 0
        var cancelled = 0
        show(
            LocalDate.of(2026, 9, 17),
            agenda = listOf(recurringItem),
            pendingDelete = recurringItem,
            onConfirmDelete = { confirmed++ },
            onCancelDelete = { cancelled++ },
        )

        compose.onNodeWithText("Cancel").performClick()
        confirmed shouldBe 0
        cancelled shouldBe 1
    }

    /** Invokes a semantics [CustomAccessibilityAction] by its label — the TalkBack local-context-menu path. */
    private fun SemanticsNodeInteraction.performSemanticsAction(label: String) {
        val node = fetchSemanticsNode()
        val actions = node.config.getOrNull(SemanticsActions.CustomActions).orEmpty()
        val action = actions.first { it.label == label }
        action.action.invoke()
    }
}
