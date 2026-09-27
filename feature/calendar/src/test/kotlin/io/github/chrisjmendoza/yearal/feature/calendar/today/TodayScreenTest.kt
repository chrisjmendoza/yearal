package io.github.chrisjmendoza.yearal.feature.calendar.today

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.chrisjmendoza.yearal.core.designsystem.adaptive.WindowWidthClass
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
 * [TodayScreen] under Robolectric: the hero's two labelled blocks (the IFC weekday and date under
 * "IFC", the real date with its real weekday under "Gregorian"), the facts line with the numeric form's
 * `IFC` marker, and the intercalary "no IFC weekday" text are on screen; the IFC weekday line speaks both
 * weekdays in one description (spec §4.1 item 7). The last tests cover the width classes (FEATURES
 * C11): two columns at expanded widths, one column below.
 */
@RunWith(AndroidJUnit4::class)
// A tall window so the whole screen, agenda and holidays included, is on screen without scrolling.
@Config(qualifiers = "w360dp-h1200dp")
class TodayScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val formatter =
        IfcDateFormatter(ApplicationProvider.getApplicationContext<Context>().resources, Locale.US)

    private fun show(
        today: LocalDate,
        holidays: List<String> = emptyList(),
        nextHoliday: Pair<LocalDate, String>? = null,
        agenda: List<AgendaItemUi> = emptyList(),
        onAgendaItemClick: (Long) -> Unit = {},
        fontScale: Float = 1f,
        widthClass: WindowWidthClass = WindowWidthClass.COMPACT,
    ) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                IfcTheme(dynamicColor = false) {
                    TodayScreen(
                        state = buildTodayUiState(today, formatter, holidays, nextHoliday, agenda),
                        widthClass = widthClass,
                        onAgendaItemClick = onAgendaItemClick,
                    )
                }
            }
        }
    }

    @Test
    fun `regular day shows the labelled IFC and Gregorian blocks, each weekday once`() {
        show(LocalDate.of(2026, 9, 17))

        // The "IFC" block: eyebrow, the bare IFC weekday (spoken with both weekdays labelled, spec §4.1
        // item 7), the hero date.
        compose.onNodeWithText("IFC", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Sunday").assertIsDisplayed()
        compose.onNodeWithContentDescription("IFC Sunday, actual Thursday").assertIsDisplayed()
        compose.onNodeWithText("September 8, 2026").assertIsDisplayed()
        // The "Gregorian" block carries the real weekday — the only place it is drawn.
        compose.onNodeWithText("GREGORIAN", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Thursday, September 17, 2026").assertIsDisplayed()
        compose.onAllNodesWithText("Thursday").assertCountEquals(0)
        // No second, labelled copy of either weekday (the former weekday block is gone).
        compose.onAllNodesWithText("IFC weekday: Sunday", useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodesWithText("Actual weekday: Thursday", useUnmergedTree = true).assertCountEquals(0)
        // The facts line: the numeric form keeps its IFC prefix (CLAUDE.md rule 5).
        compose.onNodeWithText("IFC 2026-10-08 · Day 260 · Week 38 of 52 · Q3").assertIsDisplayed()
        compose.onNodeWithContentDescription("71% of the year").assertIsDisplayed()
        compose.onNodeWithText("105 days until Year Day").assertIsDisplayed()
    }

    @Test
    fun `Year Day shows no IFC weekday and its actual weekday`() {
        show(LocalDate.of(2026, 12, 31))

        // No IFC weekday: the weekday slot says so (spec §4.1 item 5) rather than showing the real one.
        compose.onAllNodesWithText("Thursday").assertCountEquals(0)
        compose.onNodeWithText("no IFC weekday").assertIsDisplayed()
        compose.onNodeWithContentDescription("no IFC weekday, actual Thursday").assertIsDisplayed()
        compose.onNodeWithText("Year Day, 2026").assertIsDisplayed()
        compose.onNodeWithText("Thursday, December 31, 2026").assertIsDisplayed()
        compose.onNodeWithText("IFC 2026-13-29 · Day 365 · outside the weeks · Q4").assertIsDisplayed()
    }

    @Test
    fun `Leap Day shows no IFC weekday and counts down to Year Day`() {
        show(LocalDate.of(2028, 6, 17))

        compose.onAllNodesWithText("Saturday").assertCountEquals(0)
        compose.onNodeWithText("no IFC weekday").assertIsDisplayed()
        compose.onNodeWithContentDescription("no IFC weekday, actual Saturday").assertIsDisplayed()
        compose.onNodeWithText("Leap Day, 2028").assertIsDisplayed()
        compose.onNodeWithText("Saturday, June 17, 2028").assertIsDisplayed()
        compose.onNodeWithText("IFC 2028-06-29 · Day 169 · outside the weeks · Q2").assertIsDisplayed()
        compose.onNodeWithText("197 days until Year Day").assertIsDisplayed()
    }

    @Test
    fun `loading state shows only the spinner`() {
        compose.setContent { IfcTheme(dynamicColor = false) { TodayScreen(state = TodayUiState.Loading) } }

        compose.onNodeWithContentDescription("Loading today’s date").assertIsDisplayed()
    }

    // FEATURES T5: today's agenda summary and the next holiday.

    @Test
    fun `today's holidays, the next holiday and the agenda summary are shown`() {
        show(
            LocalDate.of(2026, 7, 3),
            holidays = listOf("Independence Day (observed)"),
            nextHoliday = LocalDate.of(2026, 7, 4) to "Independence Day",
            agenda =
                listOf(
                    AgendaItemUi(
                        eventId = 1,
                        title = "Picnic",
                        isAllDay = true,
                        startTime = null,
                        endTime = null,
                        colorArgb = 0xFF123F3D.toInt(),
                    ),
                ),
        )

        compose.onNodeWithText("HOLIDAYS", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Independence Day (observed)").assertIsDisplayed()
        compose.onNodeWithText("1 day until Independence Day").assertIsDisplayed()
        compose.onNodeWithText("TODAY’S EVENTS", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Picnic").assertIsDisplayed()
        compose.onNodeWithText("All day").assertIsDisplayed()
    }

    // docs/design-plan.md §4.1: empty Today still shows both cards, each with one quiet line, rather
    // than hiding them.

    @Test
    fun `no holidays or agenda still shows both cards with a quiet line`() {
        show(LocalDate.of(2026, 9, 17))

        compose.onNodeWithText("HOLIDAYS", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("No holidays today.").assertIsDisplayed()
        compose.onNodeWithText("TODAY’S EVENTS", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Nothing on your agenda today.").assertIsDisplayed()
    }

    // Left over from M4 T7 (docs/ROADMAP.md): today's agenda rows are tappable, id only (CLAUDE.md rule 8).

    @Test
    fun `tapping an agenda row reports its event id`() {
        val clicked = mutableListOf<Long>()
        show(
            LocalDate.of(2026, 9, 17),
            agenda =
                listOf(
                    AgendaItemUi(
                        eventId = 7,
                        title = "Standup",
                        isAllDay = false,
                        startTime = LocalTime.of(9, 0),
                        endTime = LocalTime.of(9, 30),
                        colorArgb = 0xFF123F3D.toInt(),
                    ),
                ),
            onAgendaItemClick = { clicked += it },
        )

        compose.onNodeWithText("Standup").performClick()

        clicked shouldBe listOf(7L)
    }

    // a11y audit finding #21: the year-progress bar and its visible percentage label must speak as one
    // node, not two — before the fix, TalkBack spoke "71% of the year" from the indicator's own
    // contentDescription and then again from the plain Text below it.

    @Test
    fun `the year-progress bar and its label speak as a single node`() {
        show(LocalDate.of(2026, 9, 17))

        compose.onNodeWithContentDescription("71% of the year").assertIsDisplayed()
        compose
            .onAllNodes(hasText("71% of the year", substring = true).or(hasContentDescription("71% of the year")))
            .assertCountEquals(1)
    }

    // docs/ARCHITECTURE.md §4 "Accessibility": 200% font scale, no clipping, 48dp touch targets
    // (a11y audit finding #23 — TodayScreenTest had no Robolectric assertion for this, only previews).

    @Test
    fun `at 200 percent font scale the hero and agenda row keep their content and touch target`() {
        val agenda =
            listOf(
                AgendaItemUi(
                    eventId = 7,
                    title = "Standup",
                    isAllDay = false,
                    startTime = LocalTime.of(9, 0),
                    endTime = LocalTime.of(9, 30),
                    colorArgb = 0xFF123F3D.toInt(),
                ),
            )
        show(LocalDate.of(2026, 9, 17), agenda = agenda, fontScale = 2f)

        compose.onNodeWithText("Sunday").assertIsDisplayed()
        compose.onNodeWithText("September 8, 2026").assertIsDisplayed()
        compose.onNodeWithContentDescription("71% of the year").assertIsDisplayed()
        compose.onNodeWithText("Standup").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
    }

    // FEATURES C11 (tablet layouts, docs/ARCHITECTURE.md §4 "Adaptive layouts"): at expanded widths the
    // hero (with the countdown chip) and the Holidays/Events cards sit side by side, each column its own
    // TalkBack traversal group, hero first; below that, one column.

    private val heroColumn = hasTestTag(TODAY_HERO_COLUMN_TEST_TAG)
    private val cardsColumn = hasTestTag(TODAY_CARDS_COLUMN_TEST_TAG)

    @Test
    @Config(qualifiers = "w840dp-h1200dp")
    fun `expanded width puts the hero on the left and the cards on the right`() {
        show(LocalDate.of(2026, 9, 17), widthClass = WindowWidthClass.EXPANDED)

        val hero = compose.onNode(heroColumn).assertIsDisplayed().getUnclippedBoundsInRoot()
        val cards = compose.onNode(cardsColumn).assertIsDisplayed().getUnclippedBoundsInRoot()
        (cards.left >= hero.right) shouldBe true
        (hero.width > cards.width) shouldBe true
        // Side by side, not stacked: both columns start at the same height.
        hero.top shouldBe cards.top

        // The hero and its countdown chip in the left column; both cards in the right one.
        compose.onNode(hasText("September 8, 2026").and(hasAnyAncestor(heroColumn))).assertIsDisplayed()
        compose.onNode(hasText("105 days until Year Day").and(hasAnyAncestor(heroColumn))).assertIsDisplayed()
        compose
            .onNode(hasText("HOLIDAYS").and(hasAnyAncestor(cardsColumn)), useUnmergedTree = true)
            .assertIsDisplayed()
        compose
            .onNode(hasText("TODAY’S EVENTS").and(hasAnyAncestor(cardsColumn)), useUnmergedTree = true)
            .assertIsDisplayed()

        // TalkBack reads the whole hero column, then the whole cards column.
        compose
            .onNode(heroColumn)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.IsTraversalGroup, true))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.TraversalIndex, 0f))
        compose
            .onNode(cardsColumn)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.IsTraversalGroup, true))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.TraversalIndex, 1f))
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun `expanded width on a landscape tablet keeps Year Day's hero and both cards on screen`() {
        show(LocalDate.of(2026, 12, 31), widthClass = WindowWidthClass.EXPANDED)

        compose.onNode(hasText("no IFC weekday").and(hasAnyAncestor(heroColumn))).assertIsDisplayed()
        compose.onNode(hasText("Thursday, December 31, 2026").and(hasAnyAncestor(heroColumn))).assertIsDisplayed()
        compose.onNodeWithText("No holidays today.").assertIsDisplayed()
        compose.onNodeWithText("Nothing on your agenda today.").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w600dp-h1200dp")
    fun `medium width stays one column, the cards under the hero`() {
        show(LocalDate.of(2026, 9, 17), widthClass = WindowWidthClass.MEDIUM)

        compose.onAllNodes(heroColumn).assertCountEquals(0)
        compose.onAllNodes(cardsColumn).assertCountEquals(0)
        val chip = compose.onNodeWithText("105 days until Year Day").getUnclippedBoundsInRoot()
        val holidays = compose.onNodeWithText("HOLIDAYS", useUnmergedTree = true).getUnclippedBoundsInRoot()
        (holidays.top >= chip.bottom) shouldBe true
    }
}
