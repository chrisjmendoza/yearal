package io.github.chrisjmendoza.yearal.feature.calendar.month

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.chrisjmendoza.yearal.core.calendar.IfcDate
import io.github.chrisjmendoza.yearal.core.calendar.IfcMonth
import io.github.chrisjmendoza.yearal.core.calendar.IfcYearMonth
import io.github.chrisjmendoza.yearal.core.designsystem.adaptive.WindowWidthClass
import io.github.chrisjmendoza.yearal.core.designsystem.format.IfcDateFormatter
import io.github.chrisjmendoza.yearal.core.designsystem.theme.IfcTheme
import io.github.chrisjmendoza.yearal.feature.calendar.agenda.AgendaItemUi
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.util.Locale
import kotlin.math.absoluteValue

private val MinTouchTarget = 48.dp

/**
 * [CalendarScreen] (docs/ROADMAP.md M3 T4; docs/ARCHITECTURE.md §4 "Adaptive layouts"; the day-card
 * merge): at compact/medium widths it is exactly [MonthScreen] with its day card below the grid; at
 * expanded widths [MonthListDetailScreen] composes the grid and the same card side by side. A day tap
 * never navigates at either width — it only calls [onSelectDay]. Written from the spec citations
 * already in `MonthScreenTest` — this class covers only what changes with the width, not the grid or
 * the card's own content again (that's `MonthScreenTest`'s and a future day-card content test's job).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1000dp-h800dp")
class CalendarScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val formatter =
        IfcDateFormatter(ApplicationProvider.getApplicationContext<Context>().resources, Locale.US)
    private val today = LocalDate.of(2026, 9, 17)
    private val october2026 = IfcYearMonth(2026, IfcMonth.OCTOBER)

    private fun monthState(
        selected: LocalDate? = null,
        dayDetail: DayDetailUi? = buildDayDetailUi(selected ?: today, today, formatter, holidays = emptyList()),
    ) = MonthUiState(
        currentPage = MonthPages.pageOf(october2026),
        today = today,
        todayPage = MonthPages.pageOf(IfcYearMonth.from(IfcDate.from(today))),
        selected = selected,
        dayDetail = dayDetail,
    )

    /** The day card's facts line (docs/design-plan.md §4.4): the numeric form, day/week, quarter. */
    private fun factsLine(detail: DayDetailUi) = "${detail.numeric} · ${detail.dayAndWeek} · ${detail.quarter}"

    private fun showCalendar(
        widthClass: WindowWidthClass,
        state: MonthUiState = monthState(),
    ) {
        compose.setContent {
            IfcTheme(dynamicColor = false) {
                CalendarScreen(
                    widthClass = widthClass,
                    monthState = state,
                    onPageChanged = {},
                    onTitleClick = {},
                    onJumpToDate = {},
                    onSelectDay = {},
                    dayCallbacks = emptyDayCallbacks(),
                )
            }
        }
    }

    private fun cell(description: String) = compose.onNode(hasContentDescription(description, substring = true))

    private fun emptyDayCallbacks() =
        DayDetailCallbacks(
            onEventClick = {},
            onAddEvent = {},
            onOpenInConverter = {},
            onRequestDelete = {},
            onConfirmDelete = {},
            onCancelDelete = {},
        )

    @Test
    fun `compact width renders only the Month screen and a tap only selects`() {
        val selected = mutableListOf<IfcDate>()
        compose.setContent {
            IfcTheme(dynamicColor = false) {
                CalendarScreen(
                    widthClass = WindowWidthClass.COMPACT,
                    monthState = monthState(),
                    onPageChanged = {},
                    onTitleClick = {},
                    onJumpToDate = {},
                    onSelectDay = { selected += it },
                    dayCallbacks = emptyDayCallbacks(),
                )
            }
        }

        compose.onNode(hasContentDescription("October 5, IFC", substring = true)).performClick()

        selected shouldContainExactly listOf(IfcDate.Regular(2026, IfcMonth.OCTOBER, 5))
    }

    @Test
    fun `expanded width composes both panes and a tap only selects in place`() {
        val selected = mutableListOf<IfcDate>()
        compose.setContent {
            IfcTheme(dynamicColor = false) {
                CalendarScreen(
                    widthClass = WindowWidthClass.EXPANDED,
                    monthState = monthState(),
                    onPageChanged = {},
                    onTitleClick = {},
                    onJumpToDate = {},
                    onSelectDay = { selected += it },
                    dayCallbacks = emptyDayCallbacks(),
                )
            }
        }

        // The grid (list pane) and the day card (detail pane, already showing today — there is no
        // separate empty state any more) are both on screen at once.
        compose.onNode(hasContentDescription("October 5, IFC", substring = true)).assertIsDisplayed()
        compose.onNodeWithText("No events on this day.").assertIsDisplayed()

        compose.onNode(hasContentDescription("October 5, IFC", substring = true)).performClick()

        selected shouldContainExactly listOf(IfcDate.Regular(2026, IfcMonth.OCTOBER, 5))
    }

    @Test
    fun `expanded width shows the selected day's card without navigating away from the grid`() {
        val onScreen = LocalDate.of(2026, 10, 12) // IFC October 5, 2026.
        val dayDetail = buildDayDetailUi(onScreen, today, formatter, holidays = emptyList())
        compose.setContent {
            IfcTheme(dynamicColor = false) {
                CalendarScreen(
                    widthClass = WindowWidthClass.EXPANDED,
                    monthState = monthState(selected = onScreen, dayDetail = dayDetail),
                    onPageChanged = {},
                    onTitleClick = {},
                    onJumpToDate = {},
                    onSelectDay = {},
                    dayCallbacks = emptyDayCallbacks(),
                )
            }
        }

        // The list pane's month heading is still visible…
        compose.onNode(hasText("October 2026").and(hasClickAction())).assertIsDisplayed()
        // …at the same time as the detail pane's content.
        compose.onNodeWithText(factsLine(dayDetail)).assertIsDisplayed()
        compose.onNode(hasContentDescription("October 5, IFC", substring = true)).assertIsSelected()
    }

    @Test
    fun `Leap Day and Year Day still render in the expanded list pane`() {
        compose.setContent {
            IfcTheme(dynamicColor = false) {
                CalendarScreen(
                    widthClass = WindowWidthClass.EXPANDED,
                    monthState = monthState().copy(currentPage = MonthPages.pageOf(IfcYearMonth(2028, IfcMonth.JUNE))),
                    onPageChanged = {},
                    onTitleClick = {},
                    onJumpToDate = {},
                    onSelectDay = {},
                    dayCallbacks = emptyDayCallbacks(),
                )
            }
        }

        compose.onNodeWithText("Leap Day", substring = true).assertIsDisplayed()
    }

    @Test
    fun `selection survives the width class flipping back and forth`() {
        val onScreen = LocalDate.of(2026, 10, 12)
        var widthClass by mutableStateOf(WindowWidthClass.EXPANDED)
        compose.setContent {
            IfcTheme(dynamicColor = false) {
                CalendarScreen(
                    widthClass = widthClass,
                    monthState = monthState(selected = onScreen),
                    onPageChanged = {},
                    onTitleClick = {},
                    onJumpToDate = {},
                    onSelectDay = {},
                    dayCallbacks = emptyDayCallbacks(),
                )
            }
        }
        compose.onNode(hasContentDescription("October 5, IFC", substring = true)).assertIsSelected()

        // A fold-out then a fold-in: the resize is not a reset because MonthUiState.selected lives in
        // the caller (the ViewModel in production), never in this composable's own state.
        widthClass = WindowWidthClass.COMPACT
        compose.waitForIdle()
        compose.onNode(hasContentDescription("October 5, IFC", substring = true)).assertIsSelected()

        widthClass = WindowWidthClass.EXPANDED
        compose.waitForIdle()
        compose.onNode(hasContentDescription("October 5, IFC", substring = true)).assertIsSelected()
    }

    @Test
    fun `expanded layout touch targets stay at least 48dp at 200 percent font scale`() {
        val onScreen = LocalDate.of(2026, 10, 12)
        val dayDetail = buildDayDetailUi(onScreen, today, formatter, holidays = emptyList())
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                IfcTheme(dynamicColor = false) {
                    CalendarScreen(
                        widthClass = WindowWidthClass.EXPANDED,
                        monthState = monthState(selected = onScreen, dayDetail = dayDetail),
                        onPageChanged = {},
                        onTitleClick = {},
                        onJumpToDate = {},
                        onSelectDay = {},
                        dayCallbacks = emptyDayCallbacks(),
                    )
                }
            }
        }

        // The day cell (this task's own new touch target inside the list pane at this width) and the
        // detail pane's "Add event" action (already proven at 48dp for the card in a future day-card
        // content test, proven again here because the same content renders in this pane too).
        compose.onNode(hasContentDescription("October 5, IFC", substring = true)).assertHeightIsAtLeast(MinTouchTarget)
        compose.onNodeWithText("Add event").assertHeightIsAtLeast(MinTouchTarget)
        compose.onNodeWithText(factsLine(dayDetail)).assertIsDisplayed()
    }

    // FEATURES C11 (tablet layouts): at medium widths (600–839dp) the single-pane page — grid and day
    // card together — is capped at 600dp and centred, so the grid keeps a phone's proportions instead
    // of stretching its cells across the window.

    @Test
    @Config(qualifiers = "w800dp-h1200dp")
    fun `medium width caps the grid and the day card at 600dp and centres them`() {
        showCalendar(WindowWidthClass.MEDIUM)

        val firstCell = cell("October 1, IFC").getUnclippedBoundsInRoot()
        val lastCell = cell("October 7, IFC").getUnclippedBoundsInRoot()
        (lastCell.right - firstCell.left <= 600.dp) shouldBe true
        val card = compose.onNodeWithTag(DAY_CARD_TEST_TAG).assertIsDisplayed().getUnclippedBoundsInRoot()
        (card.width <= 600.dp) shouldBe true
        // Centred in the 800dp window, and below the grid (one column, not a split).
        ((card.left + card.right) / 2 - 400.dp).value.absoluteValue shouldBeLessThan 1f
        ((firstCell.left + lastCell.right) / 2 - 400.dp).value.absoluteValue shouldBeLessThan 1f
        (card.top >= lastCell.bottom) shouldBe true
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun `compact width is never capped - the grid spans the phone`() {
        showCalendar(WindowWidthClass.COMPACT)

        val firstCell = cell("October 1, IFC").getUnclippedBoundsInRoot()
        val lastCell = cell("October 7, IFC").getUnclippedBoundsInRoot()
        // 360dp less the page's 16dp side padding on each side.
        (lastCell.right - firstCell.left >= 320.dp) shouldBe true
    }

    @Test
    @Config(qualifiers = "w840dp-h1200dp")
    fun `expanded width at 840dp puts the day card beside the grid, not below it`() {
        showCalendar(WindowWidthClass.EXPANDED)

        val lastCell = cell("October 7, IFC").getUnclippedBoundsInRoot()
        val card = compose.onNodeWithTag(DAY_CARD_TEST_TAG).assertIsDisplayed().getUnclippedBoundsInRoot()
        (card.left >= lastCell.right) shouldBe true
        (card.top < lastCell.bottom) shouldBe true
        // The grid pane is wide enough for seven 48dp cells even at the narrowest expanded width.
        cell("October 1, IFC").assertWidthIsAtLeast(MinTouchTarget)
    }

    // The expanded detail pane scrolls on its own, so a long day is never clipped at the bottom of a
    // landscape tablet: the card's last action can always be scrolled into view.
    @Test
    @Config(qualifiers = "w1000dp-h600dp")
    fun `the expanded detail pane scrolls a long day's card into view`() {
        val agenda =
            List(12) { index ->
                AgendaItemUi(
                    eventId = index.toLong(),
                    title = "Event $index",
                    isAllDay = true,
                    startTime = null,
                    endTime = null,
                    colorArgb = 0xFF123F3D.toInt(),
                )
            }
        val detail = buildDayDetailUi(today, today, formatter, holidays = emptyList(), agenda = agenda)
        showCalendar(WindowWidthClass.EXPANDED, monthState(dayDetail = detail))

        compose.onNodeWithText("Open in converter").performScrollTo().assertIsDisplayed()
    }
}
