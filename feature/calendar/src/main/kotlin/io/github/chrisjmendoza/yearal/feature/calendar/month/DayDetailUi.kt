package io.github.chrisjmendoza.yearal.feature.calendar.month

import io.github.chrisjmendoza.yearal.core.calendar.IfcDate
import io.github.chrisjmendoza.yearal.core.designsystem.format.IfcDateFormatter
import io.github.chrisjmendoza.yearal.feature.calendar.agenda.AgendaItemUi
import java.time.LocalDate

/**
 * One day, fully formatted for [DayCard] (docs/FEATURES.md C5; docs/ARCHITECTURE.md §4 "Screen
 * behaviors") so it renders text only and computes nothing itself. [MonthUiState.dayDetail] holds one
 * of these for [MonthUiState.summaryDate] — the selected day, or today when nothing is selected — or
 * `null` before the first date tick.
 *
 * @property date the day in the IFC, the source of every other property.
 * @property gregorianDate the same physical day in the Gregorian calendar.
 * @property ifcLong the long style (`September 8, 2026`, `Leap Day, 2028`, `Year Day, 2026`).
 * @property numeric the canonical numeric style with its mandatory prefix (`IFC 2026-10-08`).
 * @property gregorianLong the Gregorian date with its real weekday (`Thursday, September 17, 2026`).
 * @property nominalWeekday the labelled IFC weekday (`IFC weekday: Sunday`) or `no IFC weekday` on
 * Leap Day and Year Day. **Not the real weekday** — spec §4.1.
 * @property actualWeekday the labelled real weekday (`Actual weekday: Thursday`).
 * @property weekdaysDescription both weekdays in spoken form (`IFC Sunday, actual Thursday`).
 * @property dayAndWeek `Day 260 · Week 38 of 52`, or `Day 169 · outside the weeks` (spec §7.4).
 * @property quarter `Q3` (spec §7.5).
 * @property isToday whether [gregorianDate] is the ticker's current date.
 * @property holidays display labels of the enabled holidays on this day, in holiday-engine order
 * (`Independence Day (observed)`); empty when there are none.
 * @property agenda the day's event occurrences (FEATURES C5), all-day first then by start time
 * ([io.github.chrisjmendoza.yearal.core.domain.event.DayAgenda.ENTRY_ORDER]); empty when there
 * are none.
 * @property pendingDelete the agenda row awaiting delete confirmation ([AgendaItemUi.isRecurring]
 * decides the dialog's wording: "delete this occurrence" or a plain delete), or `null` when no
 * confirmation is open (FEATURES E1).
 */
data class DayDetailUi(
    val date: IfcDate,
    val gregorianDate: LocalDate,
    val ifcLong: String,
    val numeric: String,
    val gregorianLong: String,
    val nominalWeekday: String,
    val actualWeekday: String,
    val weekdaysDescription: String,
    val dayAndWeek: String,
    val quarter: String,
    val isToday: Boolean,
    val holidays: List<String>,
    val agenda: List<AgendaItemUi> = emptyList(),
    val pendingDelete: AgendaItemUi? = null,
)

/**
 * Builds the [DayDetailUi] for the Gregorian date [day] when the real today is [today]. The IFC date
 * comes from `:core:calendar` and every string from [formatter]; nothing here formats or computes a
 * date itself (CLAUDE.md rules 1 and 9).
 */
fun buildDayDetailUi(
    day: LocalDate,
    today: LocalDate,
    formatter: IfcDateFormatter,
    holidays: List<String>,
    agenda: List<AgendaItemUi> = emptyList(),
    pendingDelete: AgendaItemUi? = null,
): DayDetailUi {
    val date = IfcDate.from(day)
    return DayDetailUi(
        date = date,
        gregorianDate = day,
        ifcLong = formatter.formatLong(date),
        numeric = formatter.formatNumeric(date),
        gregorianLong = formatter.formatGregorianLong(day),
        nominalWeekday = formatter.nominalWeekday(date),
        actualWeekday = formatter.actualWeekday(date),
        weekdaysDescription = formatter.weekdaysDescription(date),
        dayAndWeek = formatter.dayAndWeek(date),
        quarter = formatter.quarter(date),
        isToday = day == today,
        holidays = holidays,
        agenda = agenda,
        pendingDelete = pendingDelete,
    )
}
