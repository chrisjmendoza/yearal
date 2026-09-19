package io.github.chrisjmendoza.yearal.feature.calendar.month

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.chrisjmendoza.yearal.core.calendar.IfcDate
import io.github.chrisjmendoza.yearal.core.calendar.IfcMonth
import io.github.chrisjmendoza.yearal.core.calendar.IfcYearMonth
import io.github.chrisjmendoza.yearal.core.designsystem.adaptive.WindowWidthClass
import io.github.chrisjmendoza.yearal.core.designsystem.format.IfcDateFormatter
import io.github.chrisjmendoza.yearal.core.designsystem.theme.IfcTheme
import io.github.chrisjmendoza.yearal.feature.calendar.day.buildDayUiState
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.util.Locale

private val MinTouchTarget = 48.dp

/**
 * [CalendarScreen] (docs/ROADMAP.md M3 T4; docs/ARCHITECTURE.md §4 "Adaptive layouts"): at
 * compact/medium widths it is exactly [MonthScreen] (a day tap navigates); at expanded widths both
 * panes of [MonthListDetailScreen] are composed and a day tap only updates the detail pane in place.
 * Written from the spec citations already in `MonthScreenTest` and `DayScreenTest` — this class covers
 * only what changes with the width, not the grid or the sheet's own content again.
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

    private fun monthState(selected: LocalDate? = null) =
        MonthUiState(
            currentPage = MonthPages.pageOf(october2026),
            today = today,
            todayPage = MonthPages.pageOf(IfcYearMonth.from(IfcDate.from(today))),
            selected = selected,
        )

    private fun emptyDayCallbacks(onClose: () -> Unit = {}) =
        DayDetailCallbacks(
            onEventClick = {},
            onAddEvent = {},
            onOpenInConverter = {},
            onRequestDelete = {},
            onConfirmDelete = {},
            onCancelDelete = {},
            onClose = onClose,
        )

    @Test
    fun `compact width renders only the Month screen and a day tap navigates`() {
        val navigated = mutableListOf<IfcDate>()
        val selectedInPlace = mutableListOf<IfcDate>()
        compose.setContent {
            IfcTheme(dynamicColor = false) {
                CalendarScreen(
                    widthClass = WindowWidthClass.COMPACT,
                    monthState = monthState(),
                    onPageChanged = {},
                    onTitleClick = {},
                    onJumpToDate = {},
                    onDayClick = { navigated += it },
                    onSelectDay = { selectedInPlace += it },
                    dayState = null,
                    dayCallbacks = emptyDayCallbacks(),
                )
            }
        }

        compose.onNode(hasContentDescription("October 5, IFC", substring = true)).performClick()

        navigated shouldContainExactly listOf(IfcDate.Regular(2026, IfcMonth.OCTOBER, 5))
        selectedInPlace shouldBe emptyList()
        // No detail pane exists at compact width.
        compose.onAllNodesWithText("No day selected").assertCountEquals(0)
    }

    @Test
    fun `expanded width composes both panes and a tap only selects in place`() {
        val navigated = mutableListOf<IfcDate>()
        val selectedInPlace = mutableListOf<IfcDate>()
        compose.setContent {
            IfcTheme(dynamicColor = false) {
                CalendarScreen(
                    widthClass = WindowWidthClass.EXPANDED,
                    monthState = monthState(),
                    onPageChanged = {},
                    onTitleClick = {},
                    onJumpToDate = {},
                    onDayClick = { navigated += it },
                    onSelectDay = { selectedInPlace += it },
                    dayState = null,
                    dayCallbacks = emptyDayCallbacks(),
                )
            }
        }

        // The grid (list pane) and the empty-state detail pane are both on screen at once.
        compose.onNode(hasContentDescription("October 5, IFC", substring = true)).assertIsDisplayed()
        compose.onNodeWithText("No day selected").assertIsDisplayed()

        compose.onNode(hasContentDescription("October 5, IFC", substring = true)).performClick()

        selectedInPlace shouldContainExactly listOf(IfcDate.Regular(2026, IfcMonth.OCTOBER, 5))
        navigated shouldBe emptyList()
    }

    @Test
    fun `expanded width shows the selected day's detail without navigating away from the grid`() {
        val onScreen = LocalDate.of(2026, 10, 12) // IFC October 5, 2026.
        val dayState = buildDayUiState(onScreen, today, formatter, holidays = emptyList())
        compose.setContent {
            IfcTheme(dynamicColor = false) {
                CalendarScreen(
                    widthClass = WindowWidthClass.EXPANDED,
                    monthState = monthState(selected = onScreen),
                    onPageChanged = {},
                    onTitleClick = {},
                    onJumpToDate = {},
                    onDayClick = {},
                    onSelectDay = {},
                    dayState = dayState,
                    dayCallbacks = emptyDayCallbacks(),
                )
            }
        }

        // The list pane's month heading is still visible…
        compose.onNode(hasText("October 2026").and(hasClickAction())).assertIsDisplayed()
        // …at the same time as the detail pane's content.
        compose.onNodeWithText(dayState.numeric).assertIsDisplayed()
        compose.onNode(hasContentDescription("October 5, IFC", substring = true)).assertIsSelected()
    }

    @Test
    fun `the detail pane's close action is wired to the caller's callback`() {
        var closed = false
        val onScreen = LocalDate.of(2026, 10, 12)
        val dayState = buildDayUiState(onScreen, today, formatter, holidays = emptyList())
        compose.setContent {
            IfcTheme(dynamicColor = false) {
                CalendarScreen(
                    widthClass = WindowWidthClass.EXPANDED,
                    monthState = monthState(selected = onScreen),
                    onPageChanged = {},
                    onTitleClick = {},
                    onJumpToDate = {},
                    onDayClick = {},
                    onSelectDay = {},
                    dayState = dayState,
                    dayCallbacks = emptyDayCallbacks(onClose = { closed = true }),
                )
            }
        }

        compose.onNodeWithContentDescription("Close").performClick()

        closed shouldBe true
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
                    onDayClick = {},
                    onSelectDay = {},
                    dayState = null,
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
                    onDayClick = {},
                    onSelectDay = {},
                    dayState = buildDayUiState(onScreen, today, formatter, holidays = emptyList()),
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
        val dayState = buildDayUiState(onScreen, today, formatter, holidays = emptyList())
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                IfcTheme(dynamicColor = false) {
                    CalendarScreen(
                        widthClass = WindowWidthClass.EXPANDED,
                        monthState = monthState(selected = onScreen),
                        onPageChanged = {},
                        onTitleClick = {},
                        onJumpToDate = {},
                        onDayClick = {},
                        onSelectDay = {},
                        dayState = dayState,
                        dayCallbacks = emptyDayCallbacks(),
                    )
                }
            }
        }

        // The day cell (this task's own new touch target inside the list pane at this width) and the
        // detail pane's "Add event" action (already proven at 48dp for the sheet in `DayScreenTest`,
        // proven again here because the same content now also renders without the sheet's chrome).
        compose.onNode(hasContentDescription("October 5, IFC", substring = true)).assertHeightIsAtLeast(MinTouchTarget)
        compose.onNodeWithText("Add event").assertHeightIsAtLeast(MinTouchTarget)
        compose.onNodeWithText(dayState.numeric).assertIsDisplayed()
    }
}
