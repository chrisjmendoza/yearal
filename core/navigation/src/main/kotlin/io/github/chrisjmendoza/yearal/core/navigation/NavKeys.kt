package io.github.chrisjmendoza.yearal.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/*
 * Every navigation destination in the app, as Navigation 3 keys (docs/ARCHITECTURE.md §4 "Screens and
 * navigation"). Features navigate to each other only through these keys, never by depending on one
 * another (CLAUDE.md rule 10). Keys are serializable so a back stack survives process death and can be
 * synthesized from a widget or notification intent.
 *
 * Dates travel as Gregorian epoch days (CLAUDE.md rule 4); the receiving screen converts through
 * `:core:calendar`.
 */

/** The Today screen: hero IFC date, Gregorian equivalent, both weekdays, agenda. */
@Serializable
data object TodayKey : NavKey

/**
 * The first-run intro (`docs/FEATURES.md` L1): at most three screens covering what the IFC is, why the
 * nominal and actual weekdays differ, and the two intercalary days, ending with a "find my IFC birthday"
 * hook into [ConverterKey]. `:app` pushes this onto the Today tab's stack, over [TodayKey], the first
 * time [io.github.chrisjmendoza.yearal.core.domain.settings.UserSettings.hasSeenIntro] is confirmed
 * `false` from the store (never before it has loaded, to avoid a returning user seeing a flash of the
 * intro). Skipping or finishing marks it seen and pops it back to whatever it was pushed over; it carries
 * no data so it can also be pushed again later from the Learn screen, without affecting the seen flag.
 */
@Serializable
data object IntroKey : NavKey

/**
 * One month of the calendar grid, its Month pager page and the day card below it (docs/FEATURES.md
 * C1, C3, C5, C7; the Day detail popup this key used to open alongside is gone — the card is now the
 * whole detail, so a "day" destination is just this key with [selectedEpochDay] set).
 *
 * @property year IFC year, 1583..9999 in the UI.
 * @property month IFC month number 1..13 (7 = Sol). **Not** a Gregorian month number.
 * @property selectedEpochDay a Gregorian date (`LocalDate.toEpochDay()`) to select when the pager opens
 * on this month, so the day card below the grid shows it instead of today; `null` selects nothing.
 * **Not validated by this key** — like a stale [year]/[month], the receiving screen does the
 * validating (`MonthPages.selectedDateOf`) and fails soft: a value that does not convert with
 * `LocalDate.ofEpochDay`, or that converts but falls outside the pager's own UI year range,
 * 1583..9999 — narrower than `:core:calendar`'s own 1..9999
 * ([io.github.chrisjmendoza.yearal.core.calendar.IfcDate.MIN_YEAR]..[io.github.chrisjmendoza.yearal.core.calendar.IfcDate.MAX_YEAR])
 * — is ignored rather than surfaced as an error. A value built from untrusted input (a synthesized
 * widget or notification intent) is therefore always safe to pass through unchecked.
 */
@Serializable
data class MonthKey(
    val year: Int,
    val month: Int,
    val selectedEpochDay: Long? = null,
) : NavKey

/**
 * The Year overview: 13 mini-months plus the intercalary days.
 *
 * @property year IFC year, 1583..9999 in the UI. **Not validated by this key itself** — the receiving
 * `YearViewModel` clamps an out-of-range value into that range rather than rejecting it, so a key
 * built from untrusted input (e.g. a synthesized intent) can never render a year the pickers would
 * refuse.
 */
@Serializable
data class YearKey(
    val year: Int,
) : NavKey

/**
 * The Gregorian ↔ IFC converter.
 *
 * @property prefillEpochDay a Gregorian epoch day (`LocalDate.toEpochDay()`) to open the converter on,
 * or `null` to default to today (from `DateTicker`). **Ignored, not an error,** when it falls outside
 * the UI's supported year range (1583..9999) or does not parse as a date — the converter falls back to
 * its default instead of surfacing a failure.
 */
@Serializable
data class ConverterKey(
    val prefillEpochDay: Long? = null,
) : NavKey

/** The events list and search. */
@Serializable
data object EventListKey : NavKey

/**
 * The event editor.
 *
 * @property eventId the event to edit, or `null` to create one. IDs only, never event content
 * (CLAUDE.md rule 8).
 * @property prefillEpochDay the Gregorian epoch day (`LocalDate.toEpochDay()`) a new event should start
 * on, or `null` to default to today. **Only takes effect while creating a new event** — once [eventId]
 * names an existing event, that event's own stored start date replaces this value once it loads. An
 * epoch day that fails to parse or falls outside `:core:calendar`'s supported years (1..9999) is
 * dropped silently rather than surfaced as an error.
 */
@Serializable
data class EventEditorKey(
    val eventId: Long? = null,
    val prefillEpochDay: Long? = null,
) : NavKey

/** The "More" tab hub, which links to Holidays, Settings and Learn. */
@Serializable
data object MoreKey : NavKey

/** Holiday sets: browse, toggle, per-year list. */
@Serializable
data object HolidaysKey : NavKey

/** Settings. */
@Serializable
data object SettingsKey : NavKey

/** Learn / About: the IFC rules and why the weekdays differ. */
@Serializable
data object LearnKey : NavKey

/** Privacy: what the app stores, what its permissions are for, and what it never does. */
@Serializable
data object PrivacyKey : NavKey
