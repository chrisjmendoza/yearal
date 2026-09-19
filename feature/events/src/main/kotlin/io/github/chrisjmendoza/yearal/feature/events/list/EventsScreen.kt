package io.github.chrisjmendoza.yearal.feature.events.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.chrisjmendoza.yearal.core.designsystem.adaptive.TwoPaneLayout
import io.github.chrisjmendoza.yearal.core.designsystem.adaptive.WindowWidthClass
import io.github.chrisjmendoza.yearal.feature.events.R
import io.github.chrisjmendoza.yearal.feature.events.editor.EventEditorCallbacks
import io.github.chrisjmendoza.yearal.feature.events.editor.EventEditorScreen
import io.github.chrisjmendoza.yearal.feature.events.editor.EventEditorUiState

private val EmptyStatePadding = 32.dp
private val EmptyStateSpacing = 8.dp

/**
 * Switches Events between the single-pane [EventListScreen] (compact and medium widths — the editor
 * arrives full-screen through `EventEditorKey`) and [EventListDetailScreen] (expanded widths — the
 * list and the editor side by side), per docs/ARCHITECTURE.md §4 "Adaptive layouts". Unlike
 * [EventListRoute], this takes [widthClass] as a plain parameter instead of reading it live, so a test
 * can drive both branches deterministically without a real window — mirrors
 * `io.github.chrisjmendoza.yearal.feature.calendar.month.CalendarScreen`.
 *
 * @param widthClass which branch to render.
 * @param listState the events list's state, shared by both branches.
 * @param onQueryChange invoked with the search field's text after every edit.
 * @param onAddEvent the "create event" action (a FAB in both layouts).
 * @param onOpenEvent invoked with the tapped row's event id.
 * @param editorState the selected event's editor state, or `null` for the empty state; ignored at
 * compact/medium widths.
 * @param editorCallbacks the editor's own callbacks; ignored when [editorState] is `null`.
 * @param modifier applied to whichever branch renders.
 */
@Composable
fun EventsScreen(
    widthClass: WindowWidthClass,
    listState: EventListUiState,
    onQueryChange: (String) -> Unit,
    onAddEvent: () -> Unit,
    onOpenEvent: (Long) -> Unit,
    editorState: EventEditorUiState?,
    editorCallbacks: EventEditorCallbacks?,
    modifier: Modifier = Modifier,
) {
    when (widthClass) {
        WindowWidthClass.COMPACT, WindowWidthClass.MEDIUM -> {
            EventListScreen(
                state = listState,
                onQueryChange = onQueryChange,
                onAddEvent = onAddEvent,
                onOpenEvent = onOpenEvent,
                modifier = modifier,
            )
        }

        WindowWidthClass.EXPANDED -> {
            EventListDetailScreen(
                listState = listState,
                onQueryChange = onQueryChange,
                onAddEvent = onAddEvent,
                onOpenEvent = onOpenEvent,
                editorState = editorState,
                editorCallbacks = editorCallbacks,
                modifier = modifier,
            )
        }
    }
}

/**
 * The expanded-width Events list-detail Scene (docs/ARCHITECTURE.md §4 "Adaptive layouts";
 * docs/ROADMAP.md M3 T4): [EventListScreen] as [TwoPaneLayout]'s list pane, the selected event's
 * [EventEditorScreen] — or [EventsDetailEmptyState] before anything is selected — as its detail pane.
 * Never navigates: [onOpenEvent] and [onAddEvent] only change which event is selected; the editor's
 * own unsaved-changes guard and Save/Delete in-flight guard are unaffected (they live in
 * [io.github.chrisjmendoza.yearal.feature.events.editor.EventEditorViewModel], not in this screen).
 *
 * The unit for this screen's own Compose tests (`EventListDetailScreenTest`), all of which pass plain
 * [EventListUiState] / [EventEditorUiState] values, exactly like [EventListScreen] and
 * [EventEditorScreen] are tested.
 *
 * @param listState the events list's state.
 * @param onQueryChange invoked with the search field's text after every edit.
 * @param onAddEvent the "create event" action.
 * @param onOpenEvent invoked with the tapped row's event id.
 * @param editorState the selected event's editor state, or `null` for the empty state.
 * @param editorCallbacks the editor's own callbacks; ignored when [editorState] is `null`.
 * @param modifier applied to the root [TwoPaneLayout].
 */
@Composable
fun EventListDetailScreen(
    listState: EventListUiState,
    onQueryChange: (String) -> Unit,
    onAddEvent: () -> Unit,
    onOpenEvent: (Long) -> Unit,
    editorState: EventEditorUiState?,
    editorCallbacks: EventEditorCallbacks?,
    modifier: Modifier = Modifier,
) {
    TwoPaneLayout(
        modifier = modifier,
        list = {
            EventListScreen(
                state = listState,
                onQueryChange = onQueryChange,
                onAddEvent = onAddEvent,
                onOpenEvent = onOpenEvent,
            )
        },
        detail = {
            Box(modifier = Modifier.fillMaxSize()) {
                if (editorState == null || editorCallbacks == null) {
                    EventsDetailEmptyState(modifier = Modifier.align(Alignment.Center))
                } else {
                    EventEditorScreen(state = editorState, callbacks = editorCallbacks)
                }
            }
        },
    )
}

/**
 * Shown in the detail pane before any event is selected (docs/ROADMAP.md M3 T4). Not a dead control —
 * it states a fact, nothing here is tappable.
 */
@Composable
private fun EventsDetailEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(EmptyStatePadding),
        verticalArrangement = Arrangement.spacedBy(EmptyStateSpacing),
    ) {
        Text(
            text = stringResource(R.string.events_detail_empty_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.events_detail_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
