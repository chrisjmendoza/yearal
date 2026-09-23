package io.github.chrisjmendoza.yearal.feature.events.list

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.chrisjmendoza.yearal.core.designsystem.adaptive.WindowWidthClass
import io.github.chrisjmendoza.yearal.core.designsystem.adaptive.currentWindowWidthClass
import io.github.chrisjmendoza.yearal.core.designsystem.theme.Dimens
import io.github.chrisjmendoza.yearal.core.designsystem.theme.IfcTheme
import io.github.chrisjmendoza.yearal.core.designsystem.theme.YearalTheme
import io.github.chrisjmendoza.yearal.core.designsystem.theme.yearalTopAppBarColors
import io.github.chrisjmendoza.yearal.core.domain.event.EventCalendar
import io.github.chrisjmendoza.yearal.core.domain.event.EventCategory
import io.github.chrisjmendoza.yearal.core.domain.event.LeapDayPolicy
import io.github.chrisjmendoza.yearal.core.navigation.EventEditorKey
import io.github.chrisjmendoza.yearal.core.navigation.Navigator
import io.github.chrisjmendoza.yearal.feature.events.R
import io.github.chrisjmendoza.yearal.feature.events.editor.EventEditorViewModel
import io.github.chrisjmendoza.yearal.feature.events.editor.rememberEventEditorState

private val RowMinHeight = 48.dp
private val ScreenPadding = 16.dp
private val RowSpacing = Dimens.SpaceXs
private val LeadingBarWidth = Dimens.SpaceXs
private val SectionSpacing = Dimens.SpaceS
private val ChipSpacing = Dimens.SpaceS
private val EmptyStateIconSize = 48.dp

/**
 * The events list (`EventListKey`, `docs/FEATURES.md` E1, E3, E5–E7, E9; docs/ARCHITECTURE.md §4
 * "Adaptive layouts"): collects [EventListViewModel.uiState] and renders it through the stateless
 * [EventsScreen]. This is the composable `:app` places behind `EventListKey`.
 *
 * [currentWindowWidthClass] decides the layout (docs/ROADMAP.md M3 T4):
 *
 * - **Compact and medium:** a row tap or the "Add event" FAB pushes `EventEditorKey` full-screen,
 *   unchanged from before this task.
 * - **Expanded:** a row tap or the FAB only calls [EventListViewModel.selectEvent] /
 *   [EventListViewModel.selectNewEvent]; no navigation happens. The selection feeds a second,
 *   [EventEditorViewModel] — created only while there is a selection, keyed by it so switching the
 *   selected event creates a fresh instance rather than reusing a stale one — whose
 *   [rememberEventEditorState] output goes straight to the list-detail pane. [BackHandler] delegates
 *   to the editor's own `requestBack`, so the unsaved-changes guard still shows before the selection is
 *   cleared (`EventEditorViewModel.requestBack`'s existing dirty check, unchanged).
 *
 * @param navigator receives `EventEditorKey` at compact/medium widths.
 * @param modifier applied to the screen's root.
 */
@Composable
fun EventListRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: EventListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selection by viewModel.selection.collectAsStateWithLifecycle()
    val widthClass = currentWindowWidthClass()
    val editorKey = if (widthClass == WindowWidthClass.EXPANDED) selection.toEditorKeyOrNull() else null

    val editorViewModel =
        editorKey?.let { key ->
            hiltViewModel<EventEditorViewModel, EventEditorViewModel.Factory>(
                key = selection.editorViewModelKey(),
                creationCallback = { factory -> factory.create(key) },
            )
        }
    val editor = editorViewModel?.let { rememberEventEditorState(it, onLeave = viewModel::clearSelection) }

    if (editorViewModel != null) {
        BackHandler(onBack = editorViewModel::requestBack)
    }

    EventsScreen(
        widthClass = widthClass,
        listState = state,
        onQueryChange = viewModel::setQuery,
        onAddEvent = {
            if (widthClass == WindowWidthClass.EXPANDED) {
                viewModel.selectNewEvent()
            } else {
                navigator.navigate(EventEditorKey())
            }
        },
        onOpenEvent = { id ->
            if (widthClass == WindowWidthClass.EXPANDED) {
                viewModel.selectEvent(id)
            } else {
                navigator.navigate(EventEditorKey(eventId = id))
            }
        },
        editorState = editor?.uiState,
        editorCallbacks = editor?.callbacks,
        modifier = modifier,
    )
}

/** The `EventEditorKey` the expanded-width detail pane should show, or `null` for its empty state. */
private fun EventListSelection.toEditorKeyOrNull(): EventEditorKey? =
    when (this) {
        EventListSelection.None -> null
        EventListSelection.New -> EventEditorKey()
        is EventListSelection.Existing -> EventEditorKey(eventId = eventId)
    }

/**
 * A stable [hiltViewModel] key for this selection, distinct per event id (and for "new"), so switching
 * the expanded-width pane's selection creates a fresh [EventEditorViewModel] instead of reusing a
 * stale one. `null` for [EventListSelection.None], where no ViewModel is created at all.
 */
private fun EventListSelection.editorViewModelKey(): String? =
    when (this) {
        EventListSelection.None -> null
        EventListSelection.New -> "event-list-editor-new"
        is EventListSelection.Existing -> "event-list-editor-$eventId"
    }

/**
 * The stateless events list — the unit for previews and Compose tests. A search field, then either an
 * empty state or the rows grouped under IFC month headers (`docs/design-plan.md` §4.5, §8 decision 5):
 * a 4dp leading bar in the row's resolved colour, the title, one combined date line (IFC day · short
 * Gregorian date), the time or "all day", a recurrence chip and a category chip (`EventCategory.EVENT`
 * gets none), and a "hidden calendar" note; each row is tappable to edit. A FAB creates a new event.
 *
 * @param onQueryChange receives the search field's text after every edit.
 * @param onAddEvent the FAB's action.
 * @param onOpenEvent receives the tapped row's event id.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListScreen(
    state: EventListUiState,
    onQueryChange: (String) -> Unit,
    onAddEvent: () -> Unit,
    onOpenEvent: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val addDescription = stringResource(R.string.events_add)
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.events_list_title)) },
                colors = yearalTopAppBarColors(),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddEvent,
                modifier = Modifier.semantics { contentDescription = addDescription },
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SearchField(
                query = if (state is EventListUiState.Loaded) state.query else "",
                onQueryChange = onQueryChange,
            )
            when (state) {
                EventListUiState.Loading -> {
                    Unit
                }

                is EventListUiState.Loaded -> {
                    if (state.items.isEmpty()) {
                        EmptyState(hasAnyEvents = state.hasAnyEvents, hasQuery = state.query.isNotBlank())
                    } else {
                        // Grouped under IFC month headers (docs/design-plan.md §4.5, §8 decision 5): the
                        // items are already sorted by start date, so a header is inserted every time
                        // monthHeaderKey changes as the list is walked once. `stickyHeader` would need
                        // `ExperimentalFoundationApi` in this foundation version, so these are plain
                        // header items instead (the task's documented fallback).
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            var previousHeaderKey: String? = null
                            for (item in state.items) {
                                if (item.monthHeaderKey != previousHeaderKey) {
                                    item(key = "header-${item.monthHeaderKey}") {
                                        MonthHeader(text = item.monthHeaderLabel)
                                    }
                                    previousHeaderKey = item.monthHeaderKey
                                }
                                item(key = item.eventId) {
                                    EventRow(
                                        item = item,
                                        showCalendarName = state.showCalendarNames,
                                        onClick = { onOpenEvent(item.eventId) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenPadding, vertical = SectionSpacing),
        label = { Text(stringResource(R.string.events_search_label)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.events_search_clear))
                }
            }
        },
        singleLine = true,
    )
}

@Composable
private fun EmptyState(
    hasAnyEvents: Boolean,
    hasQuery: Boolean,
) {
    val message =
        if (hasAnyEvents && hasQuery) {
            stringResource(R.string.events_empty_no_matches)
        } else {
            stringResource(R.string.events_empty_no_events)
        }
    Column(
        modifier = Modifier.fillMaxSize().padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(SectionSpacing, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.DateRange,
            contentDescription = null,
            modifier = Modifier.height(EmptyStateIconSize),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A section header naming the IFC month (or intercalary day) the rows below it belong to. */
@Composable
private fun MonthHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = ScreenPadding, vertical = SectionSpacing)
                .semantics { heading() },
    )
}

@Composable
private fun EventRow(
    item: EventListItem,
    showCalendarName: Boolean,
    onClick: () -> Unit,
) {
    val title = item.title.ifBlank { stringResource(R.string.events_no_title) }
    val calendarName = item.calendarName.ifBlank { stringResource(R.string.events_calendar_default_name) }
    val allDayLabel = stringResource(R.string.events_all_day)
    val zoneLabel = item.zoneLabel
    val timeText =
        when {
            item.isAllDay -> allDayLabel
            zoneLabel != null -> stringResource(R.string.events_time_with_zone, item.timeLabel.orEmpty(), zoneLabel)
            else -> item.timeLabel.orEmpty()
        }
    val recurrenceText = item.recurrenceSummary?.let { recurrenceSummaryText(it) }
    val categoryText = categoryChipText(item.category)
    val hiddenCalendarText = stringResource(R.string.events_calendar_hidden)
    val gregorianShort =
        stringResource(R.string.events_row_date_gregorian_short, item.gregorianWeekdayShort, item.gregorianDayLabel)
    val dateLine = stringResource(R.string.events_row_date_line, item.ifcDayLabel, gregorianShort)
    val rowDescription =
        stringResource(
            R.string.events_row_description,
            title,
            item.ifcLong,
            item.ifcNumeric,
            item.gregorianLong,
            timeText,
        )
    val description =
        buildString {
            append(rowDescription)
            if (recurrenceText != null) {
                append(' ')
                append(recurrenceText)
            }
            if (categoryText != null) {
                append(' ')
                append(categoryText)
            }
            if (item.calendarHidden) {
                append(' ')
                append(hiddenCalendarText)
            }
        }
    Surface(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = RowMinHeight)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = description
                },
        shape = MaterialTheme.shapes.medium,
        color = YearalTheme.colors.cardContainer,
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .width(LeadingBarWidth)
                        .background(color = Color(item.calendarColorArgb)),
            )
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = ScreenPadding, vertical = SectionSpacing),
                verticalArrangement = Arrangement.spacedBy(RowSpacing),
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                if (showCalendarName) {
                    Text(text = calendarName, style = MaterialTheme.typography.bodySmall)
                }
                Text(text = dateLine, style = MaterialTheme.typography.bodyMedium)
                Text(text = timeText, style = MaterialTheme.typography.bodyMedium)
                if (recurrenceText != null || categoryText != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(ChipSpacing)) {
                        if (recurrenceText != null) {
                            ChipLabel(text = recurrenceText)
                        }
                        if (categoryText != null) {
                            ChipLabel(text = categoryText)
                        }
                    }
                }
                if (item.calendarHidden) {
                    Text(
                        text = hiddenCalendarText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

/**
 * A non-interactive pill for the recurrence and category labels on an [EventRow] (design-pass fix 2): the row
 * itself is the tap target ([Role.Button] on the whole [Surface]), so these carry no click semantics —
 * a clickable [androidx.compose.material3.AssistChip] here swallowed the row's tap and read to
 * TalkBack as a dead button.
 */
@Composable
private fun ChipLabel(text: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = Dimens.SpaceS, vertical = Dimens.SpaceXs),
        )
    }
}

/** Turns an [EventCategory] into its chip label, or `null` for [EventCategory.EVENT] (no chip). */
@Composable
private fun categoryChipText(category: EventCategory): String? =
    when (category) {
        EventCategory.EVENT -> null
        EventCategory.OBSERVANCE -> stringResource(R.string.events_category_observance)
        EventCategory.BIRTHDAY -> stringResource(R.string.events_category_birthday)
    }

/** Turns a [RecurrenceSummary] into localized text — the only place that maps its branches to strings. */
@Composable
internal fun recurrenceSummaryText(summary: RecurrenceSummary): String =
    when (summary) {
        is RecurrenceSummary.YearlyIfc -> {
            stringResource(R.string.events_recurrence_yearly_ifc, summary.dayLabel)
        }

        RecurrenceSummary.YearlyYearDay -> {
            stringResource(R.string.events_recurrence_year_day)
        }

        is RecurrenceSummary.YearlyLeapDay -> {
            when (summary.policy) {
                LeapDayPolicy.JUNE_28 -> stringResource(R.string.events_recurrence_leap_day_june28)
                LeapDayPolicy.SKIP -> stringResource(R.string.events_recurrence_leap_day_skip)
                LeapDayPolicy.SOL_1 -> stringResource(R.string.events_recurrence_leap_day_sol1)
            }
        }

        is RecurrenceSummary.MonthlyIfc -> {
            stringResource(R.string.events_recurrence_monthly_ifc, summary.day)
        }

        is RecurrenceSummary.YearlyGregorian -> {
            stringResource(R.string.events_recurrence_yearly_gregorian, summary.dayLabel)
        }

        RecurrenceSummary.Weekly -> {
            stringResource(R.string.events_recurrence_weekly)
        }

        RecurrenceSummary.OtherRecurring -> {
            stringResource(R.string.events_recurrence_other)
        }
    }

// Previews — a populated list, the empty state, and 200% font (docs/ARCHITECTURE.md §4 "Accessibility").

private val previewItems =
    listOf(
        EventListItem(
            eventId = 1,
            title = "Sol 13 picnic",
            calendarName = "",
            calendarColorArgb = EventCalendar.DEFAULT_COLOR_ARGB,
            calendarHidden = false,
            ifcLong = "Sol 13, 2026",
            ifcNumeric = "IFC 2026-07-13",
            gregorianLong = "Tuesday, June 30, 2026",
            isAllDay = true,
            timeLabel = null,
            zoneLabel = null,
            recurrenceSummary = RecurrenceSummary.YearlyIfc("Sol 13"),
            category = EventCategory.EVENT,
            ifcDayLabel = "Sol 13",
            gregorianWeekdayShort = "Tue",
            gregorianDayLabel = "Jun 30",
            monthHeaderKey = "2026-7",
            monthHeaderLabel = "Sol 2026",
        ),
        // A Year Day event, an OBSERVANCE (its own chip), under its own header, distinct from December's.
        EventListItem(
            eventId = 2,
            title = "",
            calendarName = "Work",
            calendarColorArgb = 0xFF5B8DEF.toInt(),
            calendarHidden = true,
            ifcLong = "Year Day, 2026",
            ifcNumeric = "IFC 2026-13-29",
            gregorianLong = "Thursday, December 31, 2026",
            isAllDay = false,
            timeLabel = "9:30 AM",
            zoneLabel = "America/New_York",
            recurrenceSummary = null,
            category = EventCategory.OBSERVANCE,
            ifcDayLabel = "Year Day",
            gregorianWeekdayShort = "Thu",
            gregorianDayLabel = "Dec 31",
            monthHeaderKey = "2026-YEAR_DAY",
            monthHeaderLabel = "Year Day, 2026",
        ),
    )

@Preview(name = "Events list", showBackground = true)
@Preview(name = "Events list, dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Events list, font 2.0", showBackground = true, fontScale = 2f, heightDp = 1000)
@Composable
internal fun EventListScreenPreview() {
    IfcTheme(dynamicColor = false) {
        EventListScreen(
            state = EventListUiState.Loaded(items = previewItems, query = "", hasAnyEvents = true),
            onQueryChange = {},
            onAddEvent = {},
            onOpenEvent = {},
        )
    }
}

@Preview(name = "Events list, empty", showBackground = true)
@Composable
internal fun EventListScreenEmptyPreview() {
    IfcTheme(dynamicColor = false) {
        EventListScreen(
            state = EventListUiState.Loaded(items = emptyList(), query = "", hasAnyEvents = false),
            onQueryChange = {},
            onAddEvent = {},
            onOpenEvent = {},
        )
    }
}
