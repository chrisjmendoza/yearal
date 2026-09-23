package io.github.chrisjmendoza.yearal.feature.calendar.month

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.chrisjmendoza.yearal.core.calendar.IfcDate
import io.github.chrisjmendoza.yearal.core.designsystem.adaptive.TwoPaneLayout
import io.github.chrisjmendoza.yearal.core.designsystem.adaptive.WindowWidthClass
import io.github.chrisjmendoza.yearal.core.designsystem.theme.Dimens
import io.github.chrisjmendoza.yearal.feature.calendar.R
import io.github.chrisjmendoza.yearal.feature.calendar.agenda.AgendaItemUi
import io.github.chrisjmendoza.yearal.feature.calendar.day.DayDetail
import io.github.chrisjmendoza.yearal.feature.calendar.day.DayUiState
import java.time.LocalDate

private val EmptyStatePadding = 32.dp
private val EmptyStateSpacing = 8.dp
private val EmptyStateGlyphSize = 48.dp

/**
 * Every callback the expanded-width detail pane reports (docs/ROADMAP.md M3 T4), grouped like
 * [io.github.chrisjmendoza.yearal.feature.events.editor.EventEditorCallbacks] to keep
 * [MonthListDetailScreen]'s own signature short.
 *
 * @property onEventClick invoked with an agenda row's event id.
 * @property onAddEvent invoked by the detail pane's "Add event" action.
 * @property onOpenInConverter invoked by the detail pane's "Open in converter" action.
 * @property onRequestDelete invoked with an agenda row to open its delete confirmation.
 * @property onConfirmDelete invoked when the delete confirmation is accepted.
 * @property onCancelDelete invoked when the delete confirmation is dismissed.
 * @property onClose invoked by the detail pane's close action — clears the selection rather than
 * navigating anywhere (there is nowhere to go back to: the pane is not a `DayKey` entry).
 */
data class DayDetailCallbacks(
    val onEventClick: (Long) -> Unit,
    val onAddEvent: () -> Unit,
    val onOpenInConverter: () -> Unit,
    val onRequestDelete: (AgendaItemUi) -> Unit,
    val onConfirmDelete: () -> Unit,
    val onCancelDelete: () -> Unit,
    val onClose: () -> Unit,
)

/**
 * Switches Calendar between the single-pane [MonthScreen] (compact and medium widths — Day detail
 * arrives through [io.github.chrisjmendoza.yearal.feature.calendar.day.DayRoute]'s sheet) and
 * [MonthListDetailScreen] (expanded widths — Month and Day detail side by side), per
 * docs/ARCHITECTURE.md §4 "Adaptive layouts". Unlike [MonthRoute], this takes [widthClass] as a plain
 * parameter instead of reading it live, so a test can drive both branches deterministically without a
 * real window — [MonthRoute] is the only caller that reads
 * [io.github.chrisjmendoza.yearal.core.designsystem.adaptive.currentWindowWidthClass].
 *
 * @param widthClass which branch to render.
 * @param monthState the Month pager's state, shared by both branches.
 * @param onPageChanged invoked with the page the pager settles towards.
 * @param onTitleClick invoked with the visible page's IFC year when the app bar title is tapped.
 * @param onJumpToDate invoked with the date chosen from the jump-to-date action.
 * @param onDayClick invoked on a tap at compact/medium widths, where a day push a `DayKey` sheet.
 * @param onSelectDay invoked on a tap at expanded widths, where a day only updates the detail pane.
 * @param dayState the selected day's detail, or `null` before anything is selected or at compact/medium
 * widths (ignored there). Only rendered by the expanded branch.
 * @param dayCallbacks the expanded detail pane's own callbacks; ignored at compact/medium widths.
 * @param modifier applied to whichever branch renders.
 * @param snackbarHostState hosts the expanded detail pane's undo snackbar; `null` when there is no
 * selection, so nothing is shown.
 */
@Composable
fun CalendarScreen(
    widthClass: WindowWidthClass,
    monthState: MonthUiState,
    onPageChanged: (Int) -> Unit,
    onTitleClick: (Int) -> Unit,
    onJumpToDate: (LocalDate) -> Unit,
    onDayClick: (IfcDate) -> Unit,
    onSelectDay: (IfcDate) -> Unit,
    dayState: DayUiState?,
    dayCallbacks: DayDetailCallbacks,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState? = null,
) {
    when (widthClass) {
        WindowWidthClass.COMPACT, WindowWidthClass.MEDIUM -> {
            MonthScreen(
                state = monthState,
                onPageChanged = onPageChanged,
                onDayClick = onDayClick,
                onTitleClick = onTitleClick,
                onJumpToDate = onJumpToDate,
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
                dayState = dayState,
                dayCallbacks = dayCallbacks,
                modifier = modifier,
                snackbarHostState = snackbarHostState,
            )
        }
    }
}

/**
 * The expanded-width Calendar list-detail Scene (docs/ARCHITECTURE.md §4 "Adaptive layouts";
 * docs/ROADMAP.md M3 T4): [MonthScreen] as [TwoPaneLayout]'s list pane, the selected day's
 * [DayDetail] — or [MonthDetailEmptyState] before anything is selected — as its detail pane. Never
 * navigates: [onSelectDay] only updates [monthState]'s own selection, so rotating back to
 * compact/medium and pushing `DayKey` sees the same selected day.
 *
 * The unit for this screen's own Compose tests (`MonthListDetailScreenTest`), all of which pass plain
 * [MonthUiState] / [DayUiState] values, exactly like [MonthScreen] and
 * [io.github.chrisjmendoza.yearal.feature.calendar.day.DayScreen] are tested.
 *
 * @param monthState the Month pager's state.
 * @param onPageChanged invoked with the page the pager settles towards.
 * @param onTitleClick invoked with the visible page's IFC year when the app bar title is tapped.
 * @param onJumpToDate invoked with the date chosen from the jump-to-date action.
 * @param onSelectDay invoked with the tapped day; never navigates.
 * @param dayState the selected day's detail, or `null` for the empty state.
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
    dayState: DayUiState?,
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
            )
        },
        detail = {
            Box(modifier = Modifier.fillMaxSize()) {
                if (dayState == null) {
                    MonthDetailEmptyState(modifier = Modifier.align(Alignment.Center))
                } else {
                    DayDetail(
                        state = dayState,
                        onClose = dayCallbacks.onClose,
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
            }
        },
    )
}

/**
 * Shown in the detail pane before any day is selected (docs/ROADMAP.md M3 T4). Not a dead control —
 * it states a fact, nothing here is tappable. A muted calendar glyph (`docs/design-plan.md` §4.4) sits
 * above the text so the empty pane reads as "nothing chosen yet" rather than a blank space.
 */
@Composable
private fun MonthDetailEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(EmptyStatePadding),
        verticalArrangement = Arrangement.spacedBy(EmptyStateSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.DateRange,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Dimens.SpaceS).size(EmptyStateGlyphSize),
        )
        Text(
            text = stringResource(R.string.month_detail_empty_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.month_detail_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
