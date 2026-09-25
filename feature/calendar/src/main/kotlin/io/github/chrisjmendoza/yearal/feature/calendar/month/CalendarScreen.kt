package io.github.chrisjmendoza.yearal.feature.calendar.month

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.chrisjmendoza.yearal.core.calendar.IfcDate
import io.github.chrisjmendoza.yearal.core.designsystem.adaptive.TwoPaneLayout
import io.github.chrisjmendoza.yearal.core.designsystem.adaptive.WindowWidthClass
import io.github.chrisjmendoza.yearal.feature.calendar.agenda.AgendaItemUi
import java.time.LocalDate

/**
 * Every callback the day card reports (docs/ROADMAP.md M3 T4; the day-card merge), grouped like
 * [io.github.chrisjmendoza.yearal.feature.events.editor.EventEditorCallbacks] to keep [MonthScreen]'s
 * and [MonthListDetailScreen]'s own signatures short. Shared by both layouts: [MonthScreen] passes
 * these to the [DayCard] it renders below the grid at compact/medium widths, and
 * [MonthListDetailScreen] passes them to the [DayCard] it renders as the whole detail pane at expanded
 * widths, so the two never drift out of sync.
 *
 * @property onEventClick invoked with an agenda row's event id.
 * @property onAddEvent invoked by the card's "Add event" action.
 * @property onOpenInConverter invoked by the card's "Open in converter" action.
 * @property onRequestDelete invoked with an agenda row to open its delete confirmation.
 * @property onConfirmDelete invoked when the delete confirmation is accepted.
 * @property onCancelDelete invoked when the delete confirmation is dismissed.
 */
data class DayDetailCallbacks(
    val onEventClick: (Long) -> Unit,
    val onAddEvent: () -> Unit,
    val onOpenInConverter: () -> Unit,
    val onRequestDelete: (AgendaItemUi) -> Unit,
    val onConfirmDelete: () -> Unit,
    val onCancelDelete: () -> Unit,
)

/**
 * Switches Calendar between the single-pane [MonthScreen] (compact and medium widths — the day card
 * sits below the grid) and [MonthListDetailScreen] (expanded widths — the grid and the same card side
 * by side), per docs/ARCHITECTURE.md §4 "Adaptive layouts". A day tap never navigates at either width —
 * [onSelectDay] only calls `MonthViewModel.select` — so [MonthRoute] is the only caller that needs to
 * read [io.github.chrisjmendoza.yearal.core.designsystem.adaptive.currentWindowWidthClass]; this takes
 * [widthClass] as a plain parameter instead, so a test can drive both branches deterministically
 * without a real window.
 *
 * @param widthClass which branch to render.
 * @param monthState the Month pager's state, shared by both branches; its
 * [MonthUiState.dayDetail] is the day card's whole content at both widths.
 * @param onPageChanged invoked with the page the pager settles towards.
 * @param onTitleClick invoked with the visible page's IFC year when the app bar title is tapped.
 * @param onJumpToDate invoked with the date chosen from the jump-to-date action.
 * @param onSelectDay invoked on any day tap, at every width; never navigates.
 * @param dayCallbacks the day card's own callbacks, used by whichever layout renders it.
 * @param modifier applied to whichever branch renders.
 * @param snackbarHostState hosts the undo snackbar after an occurrence delete.
 */
@Composable
fun CalendarScreen(
    widthClass: WindowWidthClass,
    monthState: MonthUiState,
    onPageChanged: (Int) -> Unit,
    onTitleClick: (Int) -> Unit,
    onJumpToDate: (LocalDate) -> Unit,
    onSelectDay: (IfcDate) -> Unit,
    dayCallbacks: DayDetailCallbacks,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState? = null,
) {
    when (widthClass) {
        WindowWidthClass.COMPACT, WindowWidthClass.MEDIUM -> {
            MonthScreen(
                state = monthState,
                onPageChanged = onPageChanged,
                onDayClick = onSelectDay,
                onTitleClick = onTitleClick,
                onJumpToDate = onJumpToDate,
                dayCardCallbacks = dayCallbacks,
                snackbarHostState = snackbarHostState,
                modifier = modifier,
            )
        }

        WindowWidthClass.EXPANDED -> {
            MonthListDetailScreen(
                monthState = monthState,
                onPageChanged = onPageChanged,
                onTitleClick = onTitleClick,
                onJumpToDate = onJumpToDate,
                onSelectDay = onSelectDay,
                dayCallbacks = dayCallbacks,
                modifier = modifier,
                snackbarHostState = snackbarHostState,
            )
        }
    }
}

/**
 * The expanded-width Calendar list-detail Scene (docs/ARCHITECTURE.md §4 "Adaptive layouts";
 * docs/ROADMAP.md M3 T4): [MonthScreen] (with `showDayCard = false`) as [TwoPaneLayout]'s list pane, the
 * selected day's [DayCard] as its detail pane. The detail pane always has content — [DayCard] itself
 * falls back to today when [MonthUiState.selected] is `null`, exactly as the compact/medium card does —
 * so there is no separate empty state to show or hide. Never navigates: [onSelectDay] only updates
 * [monthState]'s own selection, so rotating back to compact/medium sees the same selected day.
 *
 * The unit for this screen's own Compose tests (`CalendarScreenTest`), which pass plain [MonthUiState]
 * values exactly like [MonthScreen] is tested.
 *
 * @param monthState the Month pager's state.
 * @param onPageChanged invoked with the page the pager settles towards.
 * @param onTitleClick invoked with the visible page's IFC year when the app bar title is tapped.
 * @param onJumpToDate invoked with the date chosen from the jump-to-date action.
 * @param onSelectDay invoked with the tapped day; never navigates.
 * @param dayCallbacks the detail pane's own callbacks.
 * @param modifier applied to the root [TwoPaneLayout].
 * @param snackbarHostState hosts the detail pane's undo snackbar; `null` shows none.
 */
@Composable
fun MonthListDetailScreen(
    monthState: MonthUiState,
    onPageChanged: (Int) -> Unit,
    onTitleClick: (Int) -> Unit,
    onJumpToDate: (LocalDate) -> Unit,
    onSelectDay: (IfcDate) -> Unit,
    dayCallbacks: DayDetailCallbacks,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState? = null,
) {
    TwoPaneLayout(
        modifier = modifier,
        list = {
            MonthScreen(
                state = monthState,
                onPageChanged = onPageChanged,
                onDayClick = onSelectDay,
                onTitleClick = onTitleClick,
                onJumpToDate = onJumpToDate,
                showDayCard = false,
            )
        },
        detail = {
            Box(modifier = Modifier.fillMaxSize()) {
                DayCard(
                    state = monthState,
                    onEventClick = dayCallbacks.onEventClick,
                    onAddEvent = dayCallbacks.onAddEvent,
                    onOpenInConverter = dayCallbacks.onOpenInConverter,
                    onRequestDelete = dayCallbacks.onRequestDelete,
                    onConfirmDelete = dayCallbacks.onConfirmDelete,
                    onCancelDelete = dayCallbacks.onCancelDelete,
                )
                if (snackbarHostState != null) {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        },
    )
}
