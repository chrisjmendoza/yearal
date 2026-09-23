package io.github.chrisjmendoza.yearal.feature.events.list

import io.github.chrisjmendoza.yearal.core.domain.event.EventCategory
import io.github.chrisjmendoza.yearal.core.domain.event.LeapDayPolicy

/**
 * What the events list (`EventListKey`, `docs/FEATURES.md` E1, E3, E5–E7, E9) shows
 * (`docs/ARCHITECTURE.md` §4 "State management"). Immutable; a new value is built for every change of
 * [io.github.chrisjmendoza.yearal.core.domain.event.EventRepository.observeEvents],
 * [io.github.chrisjmendoza.yearal.core.domain.event.EventRepository.observeCalendars] or the search text.
 */
sealed interface EventListUiState {
    /** Before the repository's flows have emitted their first value. */
    data object Loading : EventListUiState

    /**
     * @property items the rows to show, already filtered by [query] and in
     * [io.github.chrisjmendoza.yearal.core.domain.event.Event.LIST_ORDER] (the contract's list order).
     * @property query the search text as typed; empty means "no filter".
     * @property hasAnyEvents `false` only when the repository has no events at all — distinguishes the
     * "create your first event" empty state from "no matches" for [query].
     * @property showCalendarNames whether a row names the calendar it belongs to. `false` while the
     * database holds a single calendar, which is every install until M7 adds one per imported `.ics`
     * file or device calendar: naming "Default calendar" on every row then is a label that can only ever
     * say one thing, and it reads as though the user is supposed to have set calendars up (owner
     * feedback, 2026-09-19). It turns itself on as soon as a second calendar exists, because that is
     * when the name starts distinguishing rows. The name is *only* suppressed in this row; the colour
     * swatch, the hidden-calendar warning and the spoken description are unchanged.
     */
    data class Loaded(
        val items: List<EventListItem>,
        val query: String,
        val hasAnyEvents: Boolean,
        val showCalendarNames: Boolean = false,
    ) : EventListUiState
}

/**
 * One row of the events list: an event joined with its calendar's colour and visibility, and the
 * date/time/recurrence text pre-formatted through
 * [io.github.chrisjmendoza.yearal.core.designsystem.format.IfcDateFormatter] so the screen renders text
 * only (CLAUDE.md rules 1, 5).
 *
 * @property eventId the event's id, for navigating to the editor (CLAUDE.md rule 8: ids only).
 * @property title the event's own title; blank when the user left it blank — the screen shows a
 * localized placeholder (`docs/contracts/Events.md` T5 guidance).
 * @property calendarName the event's calendar name; blank for the built-in calendar until renamed —
 * the screen shows a localized placeholder, matching [io.github.chrisjmendoza.yearal.core.domain.event.EventCalendar].
 * @property calendarColorArgb the event's own colour, or its calendar's, as `0xAARRGGBB`.
 * @property calendarHidden `true` when the event's calendar is not visible ([calendarColorArgb] and
 * the rest are still shown — the events list is a management view, unlike the agenda).
 * @property ifcLong the start date in the IFC long style, e.g. `September 8, 2026`.
 * @property ifcNumeric the same start date in the canonical numeric style with its mandatory `IFC`
 * prefix, e.g. `IFC 2026-10-08` ([io.github.chrisjmendoza.yearal.core.calendar.IfcDate.toPrefixedString], CLAUDE.md rule 5).
 * @property gregorianLong the same start date in the Gregorian long style, weekday included.
 * @property isAllDay `true` for an all-day event; the screen shows the "all day" label instead of [timeLabel].
 * @property timeLabel the start time in the locale's short style (e.g. `9:30 AM`), or `null` for an
 * all-day event.
 * @property zoneLabel the fixed zone id, or `null` when floating or all-day; shown next to [timeLabel].
 * @property recurrenceSummary how the event repeats, or `null` for a one-off event.
 * @property category what kind of entry this is (`docs/design-plan.md` §5.4); [EventCategory.EVENT]
 * gets no chip, [EventCategory.OBSERVANCE] and [EventCategory.BIRTHDAY] each get their own.
 * @property ifcDayLabel the start date named within its month, no year (`Sol 12`, `Year Day`,
 * `Leap Day`) — half of the row's single combined date line (`docs/design-plan.md` §4.5).
 * @property gregorianWeekdayShort the start date's actual weekday, short style (`Tue`) — the other half
 * of the combined date line, alongside [gregorianDayLabel].
 * @property gregorianDayLabel the start date's Gregorian month and day, no year (`Jun 18`).
 * @property monthHeaderKey a stable key, constant across every row of the same IFC month header
 * (`docs/design-plan.md` §4.5, §8 decision 5: grouped by IFC month, not Gregorian) — distinct for Year
 * Day and Leap Day so each intercalary day gets its own header rather than joining December or June.
 * Consecutive rows sharing this key sit under one header; [items] is already in date order, so the
 * screen only has to watch for the key changing as it walks the list.
 * @property monthHeaderLabel the header's text: the month and year (`Sol 2026`) for a regular month, or
 * the intercalary day's name and year (`Year Day, 2026`) for Year Day or Leap Day.
 */
data class EventListItem(
    val eventId: Long,
    val title: String,
    val calendarName: String,
    val calendarColorArgb: Int,
    val calendarHidden: Boolean,
    val ifcLong: String,
    val ifcNumeric: String,
    val gregorianLong: String,
    val isAllDay: Boolean,
    val timeLabel: String?,
    val zoneLabel: String?,
    val recurrenceSummary: RecurrenceSummary?,
    val category: EventCategory = EventCategory.EVENT,
    val ifcDayLabel: String = ifcLong,
    val gregorianWeekdayShort: String = "",
    val gregorianDayLabel: String = gregorianLong,
    val monthHeaderKey: String = "",
    val monthHeaderLabel: String = "",
)

/**
 * How an event repeats, in the shape the screen turns into localized text (CLAUDE.md rule 9): every
 * branch carries only the data a string resource needs, never English text (CLAUDE.md rule 6 — every
 * [io.github.chrisjmendoza.yearal.core.domain.event.Recurrence] and
 * [io.github.chrisjmendoza.yearal.core.domain.event.IntercalaryDay] shape has one).
 */
sealed interface RecurrenceSummary {
    /** Yearly on a regular IFC date. [dayLabel] is the day named in its month, e.g. `Sol 13`. */
    data class YearlyIfc(
        val dayLabel: String,
    ) : RecurrenceSummary

    /** Yearly on Year Day. */
    data object YearlyYearDay : RecurrenceSummary

    /** Yearly on Leap Day, with the policy that applies in a common rule year. */
    data class YearlyLeapDay(
        val policy: LeapDayPolicy,
    ) : RecurrenceSummary

    /** Monthly on the same IFC day of every month. */
    data class MonthlyIfc(
        val day: Int,
    ) : RecurrenceSummary

    /** Yearly on a Gregorian date. [dayLabel] is the Gregorian month and day, e.g. `Jun 18`. */
    data class YearlyGregorian(
        val dayLabel: String,
    ) : RecurrenceSummary

    /** Weekly, the real seven-day week. */
    data object Weekly : RecurrenceSummary

    /** A Gregorian rule this app's editor did not create (an import, in a later release). */
    data object OtherRecurring : RecurrenceSummary
}
