package io.github.chrisjmendoza.yearal.feature.holidays

import io.github.chrisjmendoza.yearal.core.designsystem.picker.DatePickerRange
import io.github.chrisjmendoza.yearal.core.domain.holiday.HolidaySet

/**
 * One bundled holiday set as the Holidays screen lists it (FEATURES H1, H2, H3, H5; ROADMAP M6 T2),
 * already resolved for the current locale so the screen renders text only.
 *
 * @property id the set's [HolidaySet.id], the value stored in
 * [io.github.chrisjmendoza.yearal.core.domain.settings.UserSettings.enabledHolidaySets] (`"ifc"`,
 * `"us"`, …).
 * @property name the display name for the device language, falling back to English.
 * @property region the localized country name for a regional set (`"United States"`), or `null` for a
 * region-independent set such as the IFC observances and the Easter family.
 * @property holidayCount how many holidays [HolidaySet.holidays] defines, irrespective of whether any
 * of them has an occurrence in the shown year (`since`/`until`/`yearFilter` can make that count vary
 * by year; this is the size of the definition list, not of a particular year's occurrences).
 * @property sources where the set's data came from (`docs/holidays-and-import.md` §2.4), already
 * joined into one display line, or `null` when the pack carries none.
 * @property enabled whether this set is in
 * [io.github.chrisjmendoza.yearal.core.domain.settings.UserSettings.enabledHolidaySets] right now; the
 * screen's switch reflects this and nothing else.
 */
data class HolidaySetRow(
    val id: String,
    val name: String,
    val region: String?,
    val holidayCount: Int,
    val sources: String?,
    val enabled: Boolean,
)

/**
 * One holiday occurrence on the chosen year's list (ROADMAP M6 T2 "a per-year list with both dates").
 *
 * @property epochDay the Gregorian date of this occurrence, as `LocalDate.toEpochDay()` (CLAUDE.md rule
 * 4); tapping the row navigates to `DayKey(epochDay)`.
 * @property name the holiday's display name, with an "(observed)" suffix when
 * [io.github.chrisjmendoza.yearal.core.domain.holiday.HolidayOccurrence.observed] is `true`. Holiday
 * names come from the bundled packs, never from this module's string resources (CLAUDE.md rule 9).
 * @property ifcLong the IFC date in long form (`September 8, 2026`, `Leap Day, 2028`, `Year Day, 2026`).
 * @property ifcNumeric the canonical numeric form with its mandatory `IFC` prefix (`IFC 2026-10-08`;
 * CLAUDE.md rule 5).
 * @property gregorianLong the Gregorian date with its real weekday (`Thursday, September 17, 2026`) —
 * the app's one unlabelled-weekday exception, because the sentence is unmistakably Gregorian
 * (CLAUDE.md rule 3; see [io.github.chrisjmendoza.yearal.core.designsystem.format.IfcDateFormatter.formatGregorianLong]).
 * @property description the merged TalkBack description of the whole row.
 * @property isIntercalary whether this occurrence falls on Year Day or Leap Day (`ifcDate.isIntercalary`
 * from `:core:calendar`), so the row can show the intercalary icon and container instead of the plain
 * holiday diamond (docs/design-plan.md section 4.7; CLAUDE.md rule 6). Defaults to `false` for every ordinary
 * holiday.
 */
data class HolidayOccurrenceRow(
    val epochDay: Long,
    val name: String,
    val ifcLong: String,
    val ifcNumeric: String,
    val gregorianLong: String,
    val description: String,
    val isIntercalary: Boolean = false,
)

/**
 * [rows] grouped under the IFC month they belong to, in calendar order (`docs/ARCHITECTURE.md` §4
 * "Screen behaviors"). An intercalary day belongs to the month it follows — Leap Day groups under June,
 * Year Day under December — the same attachment [io.github.chrisjmendoza.yearal.core.calendar.IfcDate.monthNumber]
 * already encodes, so grouping by it needs no special case for the floating days (CLAUDE.md rule 6).
 *
 * @property monthLabel the month's display name (`January`, `Sol`, `December`).
 * @property rows the month's occurrences, in date order.
 */
data class HolidayMonthGroup(
    val monthLabel: String,
    val rows: List<HolidayOccurrenceRow>,
)

/**
 * What the Holidays screen shows (ROADMAP M6 T2; FEATURES H1, H2, H3, H5, H7).
 */
sealed interface HolidaysUiState {
    /** Before the stored settings and the first date tick have both been read. */
    data object Loading : HolidaysUiState

    /**
     * The bundled sets with their on/off state, and [year]'s holidays from the enabled sets.
     *
     * @property sets every bundled set, in catalogue order ([io.github.chrisjmendoza.yearal.core.holidays.BundledHolidayPacks.all]).
     * @property year the IFC year shown, always inside [DatePickerRange] (1583..9999); the ViewModel
     * clamps it, so the screen never has to. Defaults to the current year from
     * [io.github.chrisjmendoza.yearal.core.domain.DateTicker] until the user pages away from it.
     * @property canGoPreviousYear whether `year - 1` is still inside [DatePickerRange].
     * @property canGoNextYear whether `year + 1` is still inside [DatePickerRange].
     * @property groups [year]'s holidays from every enabled set, grouped by IFC month in calendar
     * order; empty when every set is disabled (the screen shows an explanatory empty state, not a bug).
     */
    data class Loaded(
        val sets: List<HolidaySetRow>,
        val year: Int,
        val groups: List<HolidayMonthGroup>,
        val canGoPreviousYear: Boolean = year > DatePickerRange.MIN_YEAR,
        val canGoNextYear: Boolean = year < DatePickerRange.MAX_YEAR,
    ) : HolidaysUiState
}
