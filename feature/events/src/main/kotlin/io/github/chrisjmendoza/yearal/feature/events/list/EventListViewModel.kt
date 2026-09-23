package io.github.chrisjmendoza.yearal.feature.events.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.chrisjmendoza.yearal.core.calendar.IfcDate
import io.github.chrisjmendoza.yearal.core.calendar.IfcYearMonth
import io.github.chrisjmendoza.yearal.core.designsystem.format.IfcDateFormatter
import io.github.chrisjmendoza.yearal.core.domain.event.Event
import io.github.chrisjmendoza.yearal.core.domain.event.EventCalendar
import io.github.chrisjmendoza.yearal.core.domain.event.EventRepository
import io.github.chrisjmendoza.yearal.core.domain.event.EventTiming
import io.github.chrisjmendoza.yearal.core.domain.event.IfcRecurrence
import io.github.chrisjmendoza.yearal.core.domain.event.IntercalaryDay
import io.github.chrisjmendoza.yearal.core.domain.event.Recurrence
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject

/**
 * State holder for the events list (`EventListKey`, `docs/ROADMAP.md` M4 T5). Joins
 * [EventRepository.observeEvents] with [EventRepository.observeCalendars] for colour and calendar
 * visibility, and applies an in-memory search over the title, notes and location (FEATURES E9;
 * `docs/contracts/Events.md` §2 "Search is an in-memory filter" and decision 12) — there is no search
 * query on the repository.
 *
 * The list keeps [Event.LIST_ORDER] (the contract's list order) unchanged; it does not reorder around
 * "today" because that needs `RecurrenceExpander`, which is out of this task's scope
 * (`docs/contracts/Events.md` §7 "T5").
 *
 * @param savedStateHandle keeps the search text across process death.
 * @param eventRepository the events and calendars to show.
 * @param formatter renders every date through `:core:calendar`, never computing one itself (CLAUDE.md rule 1).
 */
@HiltViewModel
class EventListViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        eventRepository: EventRepository,
        formatter: IfcDateFormatter,
    ) : ViewModel() {
        private val query = MutableStateFlow(savedStateHandle.get<String>(KEY_QUERY) ?: "")
        private val _selection = MutableStateFlow<EventListSelection>(EventListSelection.None)

        /** [EventListUiState.Loading] until both flows have emitted, then a [EventListUiState.Loaded] per change. */
        val uiState: StateFlow<EventListUiState> =
            combine(
                eventRepository.observeEvents(),
                eventRepository.observeCalendars(),
                query,
            ) { events, calendars, q ->
                buildEventListUiState(events, calendars, q, formatter)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), EventListUiState.Loading)

        /**
         * The expanded-width list-detail pane's own selection (docs/ROADMAP.md M3 T4;
         * docs/ARCHITECTURE.md §4 "Adaptive layouts") — ignored at compact widths, where a row or the
         * "Add event" FAB navigates to `EventEditorKey` instead. Retained across a configuration change
         * or a fold/unfold by ordinary `ViewModel` retention (`:feature:calendar`'s own Month selection
         * follows the same rule); not persisted to [savedStateHandle], so a process death restarts on
         * [EventListSelection.None] rather than reopening whatever was selected.
         */
        val selection: StateFlow<EventListSelection> = _selection.asStateFlow()

        /** Updates the search text; an empty string shows every event. */
        fun setQuery(text: String) {
            savedStateHandle[KEY_QUERY] = text
            query.value = text
        }

        /** Selects [eventId] for the expanded-width detail pane (docs/ROADMAP.md M3 T4). */
        fun selectEvent(eventId: Long) {
            _selection.value = EventListSelection.Existing(eventId)
        }

        /** Selects "new event" for the expanded-width detail pane (docs/ROADMAP.md M3 T4). */
        fun selectNewEvent() {
            _selection.value = EventListSelection.New
        }

        /** Clears the selection: the expanded-width detail pane returns to its empty state. */
        fun clearSelection() {
            _selection.value = EventListSelection.None
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
            const val KEY_QUERY = "events.list.query"
        }
    }

/**
 * What the expanded-width list-detail pane's detail side shows (docs/ROADMAP.md M3 T4;
 * docs/ARCHITECTURE.md §4 "Adaptive layouts"). Ignored at compact widths.
 */
sealed interface EventListSelection {
    /** Nothing selected: the detail pane shows its empty state. */
    data object None : EventListSelection

    /** A brand-new, unsaved event: the detail pane shows the editor with `EventEditorKey()`. */
    data object New : EventListSelection

    /** An existing event: the detail pane shows the editor with `EventEditorKey(eventId = eventId)`. */
    data class Existing(
        val eventId: Long,
    ) : EventListSelection
}

/** Builds the loaded state: filters [events] by [query], then joins each with its calendar. */
internal fun buildEventListUiState(
    events: List<Event>,
    calendars: List<EventCalendar>,
    query: String,
    formatter: IfcDateFormatter,
): EventListUiState.Loaded {
    val calendarsById = calendars.associateBy(EventCalendar::id)
    val items =
        events
            .filter { matchesQuery(it, query) }
            .map { event -> buildEventListItem(event, calendarsById[event.calendarId], formatter) }
    return EventListUiState.Loaded(
        items = items,
        query = query,
        hasAnyEvents = events.isNotEmpty(),
        // Counted from the calendars that exist, not from the ones these rows happen to use: a row's
        // name only earns its place once there is a second calendar it could have belonged to.
        showCalendarNames = calendars.size > 1,
    )
}

/** Case-insensitive substring match on the title, notes and location — the fields a user searches by. */
internal fun matchesQuery(
    event: Event,
    query: String,
): Boolean {
    if (query.isBlank()) return true
    return event.title.contains(query, ignoreCase = true) ||
        event.description.contains(query, ignoreCase = true) ||
        event.location.contains(query, ignoreCase = true)
}

private fun buildEventListItem(
    event: Event,
    calendar: EventCalendar?,
    formatter: IfcDateFormatter,
): EventListItem {
    val ifcDate = IfcDate.from(event.startDate)
    val timing = event.timing
    val (headerKey, headerLabel) = monthHeaderFor(ifcDate, formatter)
    return EventListItem(
        eventId = event.id,
        title = event.title,
        calendarName = calendar?.name.orEmpty(),
        calendarColorArgb = event.colorArgb ?: calendar?.colorArgb ?: EventCalendar.DEFAULT_COLOR_ARGB,
        calendarHidden = calendar?.visible == false,
        ifcLong = formatter.formatLong(ifcDate),
        ifcNumeric = formatter.formatNumeric(ifcDate),
        gregorianLong = formatter.formatGregorianLong(event.startDate),
        isAllDay = event.isAllDay,
        timeLabel = (timing as? EventTiming.Timed)?.let { formatMinuteOfDay(it.startMinuteOfDay) },
        zoneLabel = (timing as? EventTiming.Timed)?.zone?.id,
        recurrenceSummary = recurrenceSummaryFor(event.recurrence, ifcDate, event.startDate, formatter),
        category = event.category,
        ifcDayLabel = formatter.formatDay(ifcDate),
        gregorianWeekdayShort =
            formatter.weekdayName(event.startDate.dayOfWeek, IfcDateFormatter.WeekdayNameStyle.SHORT),
        gregorianDayLabel = formatter.formatGregorianMonthDay(event.startDate),
        monthHeaderKey = headerKey,
        monthHeaderLabel = headerLabel,
    )
}

/**
 * The IFC-month grouping header for [date] (`docs/design-plan.md` §4.5, §8 decision 5 — grouped by IFC
 * month, the app's own calendar, not Gregorian): a stable key and its localized label. A regular date
 * groups with every other date in the same IFC year and month ("Sol 2026"); Year Day and Leap Day each
 * get their own header per year, distinct from the month they follow, since neither belongs to a month
 * (CLAUDE.md rule 6).
 */
private fun monthHeaderFor(
    date: IfcDate,
    formatter: IfcDateFormatter,
): Pair<String, String> =
    when (date) {
        is IfcDate.Regular -> {
            "${date.year}-${date.month.number}" to formatter.monthTitle(IfcYearMonth(date.year, date.month))
        }

        is IfcDate.YearDay -> {
            "${date.year}-YEAR_DAY" to formatter.formatLong(date)
        }

        is IfcDate.LeapDay -> {
            "${date.year}-LEAP_DAY" to formatter.formatLong(date)
        }
    }

private fun formatMinuteOfDay(minuteOfDay: Int): String {
    val time = LocalTime.ofSecondOfDay(minuteOfDay * SECONDS_PER_MINUTE.toLong())
    return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault()).format(time)
}

/**
 * The [RecurrenceSummary] of [recurrence], anchored on [ifcDate] / [startDate]. Because
 * [IfcRecurrence.isAnchoredOn] is an [Event] invariant, an IFC rule's own position (month/day,
 * intercalary day or day of month) always matches [ifcDate] — deriving the label from the anchor
 * rather than the rule avoids reconstructing a second [IfcDate] (CLAUDE.md rule 1).
 */
internal fun recurrenceSummaryFor(
    recurrence: Recurrence,
    ifcDate: IfcDate,
    startDate: java.time.LocalDate,
    formatter: IfcDateFormatter,
): RecurrenceSummary? =
    when (recurrence) {
        Recurrence.None -> {
            null
        }

        is IfcRecurrence.YearlyOnDate -> {
            RecurrenceSummary.YearlyIfc(formatter.formatDay(ifcDate))
        }

        is IfcRecurrence.YearlyOnIntercalary -> {
            when (val day = recurrence.day) {
                is IntercalaryDay.YearDay -> RecurrenceSummary.YearlyYearDay
                is IntercalaryDay.LeapDay -> RecurrenceSummary.YearlyLeapDay(day.commonYearPolicy)
            }
        }

        is IfcRecurrence.MonthlyOnDay -> {
            RecurrenceSummary.MonthlyIfc(recurrence.day)
        }

        is Recurrence.Gregorian -> {
            when {
                recurrence.rrule.startsWith("FREQ=YEARLY") -> {
                    RecurrenceSummary.YearlyGregorian(formatter.formatGregorianMonthDay(startDate))
                }

                recurrence.rrule.startsWith("FREQ=WEEKLY") -> {
                    RecurrenceSummary.Weekly
                }

                else -> {
                    RecurrenceSummary.OtherRecurring
                }
            }
        }
    }

private const val SECONDS_PER_MINUTE = 60
