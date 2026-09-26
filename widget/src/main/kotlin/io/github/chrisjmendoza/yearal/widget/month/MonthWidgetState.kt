package io.github.chrisjmendoza.yearal.widget.month

import io.github.chrisjmendoza.yearal.core.calendar.IfcDate
import io.github.chrisjmendoza.yearal.core.calendar.IfcMonth
import io.github.chrisjmendoza.yearal.core.calendar.IfcYearMonth
import io.github.chrisjmendoza.yearal.core.designsystem.calendar.GRID_COLUMNS
import io.github.chrisjmendoza.yearal.core.designsystem.format.IfcDateFormatter
import io.github.chrisjmendoza.yearal.core.designsystem.format.IfcDateFormatter.WeekdayNameStyle
import io.github.chrisjmendoza.yearal.widget.today.TodayDate
import java.time.LocalDate

/**
 * One of the 28 regular-day cells in the month grid, in row-major order (CLAUDE.md rule 1: the day
 * itself is `:core:calendar`'s [IfcDate.Regular]; this only carries what the widget renders).
 *
 * @property dayOfMonth 1..28.
 * @property gregorianDate the real, Gregorian date of this cell (CLAUDE.md rule 4). It builds the
 *   cell's tap target ([io.github.chrisjmendoza.yearal.widget.today.dayLaunchIntent], ROADMAP M3 T5),
 *   and at the widget's larger responsive size its day of month is also shown under the IFC number,
 *   the same pairing the app's own `MonthGrid` cell uses (FEATURES C1).
 * @property isToday whether this cell is the real today, matched by **Gregorian** date (CLAUDE.md
 *   rule 4 -- events, "today", and everything else tied to real life compare Gregorian dates, never
 *   IFC numeric fields).
 * @property hasEvent whether at least one event occurrence falls on this day (ROADMAP M5 T6,
 *   `docs/contracts/Events.md` §5 "`ObserveAgendaUseCase.presence`"). Holidays are never counted, per
 *   the same contract. Carries no event content (CLAUDE.md rule 8) -- a plain boolean, never a count or
 *   a title.
 * @property hasHoliday whether at least one holiday of an enabled set falls on this day
 *   ([fetchMonthHolidays]). Separate from [hasEvent] because the two are drawn as different marks, the
 *   same distinction the app's own `DayMarks` makes (FEATURES C4): shape, never colour alone
 *   (CLAUDE.md rule 3). Also a plain boolean and never a name -- a holiday's label is screen content,
 *   not home-screen content.
 * @property contentDescription this cell's own short TalkBack description (ROADMAP M8 T1, accessibility
 *   audit finding #1; [dayCellContentDescription]), carried on the cell's own clickable node so a
 *   screen reader announces what the cell is rather than a bare digit. Presence only, like [hasEvent]
 *   and [hasHoliday] themselves -- never a holiday name or an event count or title (CLAUDE.md rule 8).
 */
data class MonthDayCellState(
    val dayOfMonth: Int,
    val gregorianDate: LocalDate,
    val isToday: Boolean,
    val hasEvent: Boolean = false,
    val hasHoliday: Boolean = false,
    val contentDescription: String = "",
)

/**
 * The full-width band beneath the grid for the month's trailing intercalary day (CLAUDE.md rule 6):
 * [IfcDate.LeapDay] after June in leap years, or [IfcDate.YearDay] after December. `null` on
 * [MonthWidgetState.intercalary] for every other month, which has neither.
 *
 * @property label the day's name without a year (`Leap Day`, `Year Day`; [IfcDateFormatter.formatDay]).
 * @property subtitle the Gregorian date, its real weekday, and the "no IFC weekday" note
 *   ([IfcDateFormatter.intercalarySubtitle]) -- the day belongs to no week (spec §2.4), so it is never
 *   given a nominal weekday.
 * @property gregorianDate the real, Gregorian date of this band (CLAUDE.md rule 4), used to build its
 *   tap target ([io.github.chrisjmendoza.yearal.widget.today.dayLaunchIntent], ROADMAP M3 T5) --
 *   Leap Day and Year Day are ordinary dates and get their own tap target exactly like a grid cell.
 * @property isToday whether the real today is this intercalary day; when true the band itself carries
 *   the today highlight, since it is not part of the 4x7 grid.
 * @property hasEvent whether at least one event occurrence falls on this intercalary day (ROADMAP M5
 *   T6) -- Leap Day and Year Day are ordinary dates to the events contract (`docs/contracts/Events.md`
 *   §1.2) and so can carry events like any other day, and CLAUDE.md rule 6 requires this case be
 *   handled explicitly rather than silently dropped because the day is not one of the 28 grid cells.
 */
data class MonthIntercalaryState(
    val label: String,
    val subtitle: String,
    val gregorianDate: LocalDate,
    val isToday: Boolean,
    val hasEvent: Boolean = false,
    val hasHoliday: Boolean = false,
)

/**
 * What the Month-grid widget shows for one composition (docs/ARCHITECTURE.md §5 "Widget types" item 2;
 * FEATURES S2-S5; ROADMAP M5 T3). Built fresh from a [TodayDate] on every render
 * ([io.github.chrisjmendoza.yearal.widget.month.MonthGlanceWidget] recomputes "today" from the injected
 * `Clock`/`ZoneProvider` every time, CLAUDE.md rule 2): nothing here is a cached date, and rebuilding
 * this state is what makes the shown month self-correct across a midnight or year rollover.
 *
 * @property monthTitle the month name and year (`Sol 2028`).
 * @property nominalWeekdayHeaders the seven nominal IFC weekday short names, Sunday first -- identical
 *   in every month and year (spec §2.3, R6).
 * @property actualWeekdayHeaders the seven real-world weekday short names of this month's columns
 *   ([IfcYearMonth.actualDayOfWeek]); differs month to month and shifts by one after Leap Day
 *   (calendar-spec §4.1). Never derived from [nominalWeekdayHeaders].
 * @property days the 28 regular-day cells, row-major, [GRID_COLUMNS] per row.
 * @property intercalary the trailing Leap Day / Year Day band, or `null` for a month with neither.
 * @property gregorianSpanLabel the month's Gregorian date range (`Jun 18 – Jul 15`,
 *   [IfcDateFormatter.gregorianSpan]), shown only at the widget's larger responsive size.
 * @property contentDescription the merged TalkBack description for the whole tappable widget outside
 *   the grid (the title, header rows and Gregorian span): the month title, today's IFC date with both
 *   labelled weekdays and its Gregorian equivalent ([IfcDateFormatter.dayDescription]), whether today
 *   has an event (ROADMAP M5 T6, present only as a plain "has events" hint -- never a count or a title,
 *   CLAUDE.md rule 8), and the tap hint. Each of the 28 day cells also carries its own short
 *   description ([MonthDayCellState.contentDescription], [dayCellContentDescription]) since ROADMAP
 *   M8 T1 (accessibility audit finding #1) -- see that function's KDoc for why this supersedes the
 *   single-description-only design docs/ARCHITECTURE.md §5 originally recorded.
 */
data class MonthWidgetState(
    val monthTitle: String,
    val nominalWeekdayHeaders: List<String>,
    val actualWeekdayHeaders: List<String>,
    val days: List<MonthDayCellState>,
    val intercalary: MonthIntercalaryState?,
    val gregorianSpanLabel: String,
    val contentDescription: String,
)

/**
 * Builds the [MonthWidgetState] for the IFC month containing [today] -- the month the widget always
 * shows, since it is perpetual rather than paged. [IfcYearMonth.from] attaches an intercalary [today]
 * to the month it follows (Leap Day -> June, Year Day -> December, calendar-spec §2.4 R10), so a today
 * that lands on Leap Day or Year Day shows that month with its band highlighted, not a page for the day
 * alone.
 *
 * Every date and weekday comes from `:core:calendar` (CLAUDE.md rule 1); this function only shapes and
 * formats what [today] and [IfcYearMonth] already computed.
 *
 * @param eventDates the Gregorian dates with at least one event occurrence in this month
 *   (`ObserveAgendaUseCase.presence`, ROADMAP M5 T6), from [fetchMonthEventPresence]. Defaults to empty
 *   for callers that do not care about event dots (most tests, and any render where the snapshot timed
 *   out or failed -- see [fetchMonthEventPresence]'s KDoc).
 * @param hasEventsLabel the localized "has events" hint appended to [MonthWidgetState.contentDescription]
 *   when today has an event; the empty default appends nothing, which also keeps every existing caller
 *   that does not pass one byte-for-byte unchanged.
 * @param holidayDates the Gregorian dates in this month carrying a holiday from an enabled set
 *   ([fetchMonthHolidays]). Defaults to empty, which renders every day without a holiday mark -- the
 *   same "absent rather than wrong" fallback a timed-out snapshot produces.
 * @param cellTodayLabel the localized "today" qualifier appended to a cell's own
 *   [MonthDayCellState.contentDescription] ([dayCellContentDescription], ROADMAP M8 T1). Empty default
 *   omits the qualifier, matching [hasEventsLabel]'s own empty-default behaviour above.
 * @param cellHolidayLabel the localized "holiday" qualifier, presence only and never the holiday's name
 *   (CLAUDE.md rule 8). Empty default omits it.
 * @param cellEventLabel the localized "has events" qualifier, presence only and never a count or title
 *   (CLAUDE.md rule 8). Empty default omits it.
 */
fun buildMonthWidgetState(
    today: TodayDate,
    formatter: IfcDateFormatter,
    tapHint: String,
    eventDates: Set<LocalDate> = emptySet(),
    hasEventsLabel: String = "",
    holidayDates: Set<LocalDate> = emptySet(),
    cellTodayLabel: String = "",
    cellHolidayLabel: String = "",
    cellEventLabel: String = "",
): MonthWidgetState {
    val month = IfcYearMonth.from(today.ifcDate)

    val days =
        (1..IfcMonth.DAYS_PER_MONTH).map { day ->
            val date = IfcDate.Regular(month.year, month.month, day)
            val gregorianDate = date.toLocalDate()
            val isToday = gregorianDate == today.gregorianDate
            val hasEvent = gregorianDate in eventDates
            val hasHoliday = gregorianDate in holidayDates
            MonthDayCellState(
                dayOfMonth = day,
                gregorianDate = gregorianDate,
                isToday = isToday,
                hasEvent = hasEvent,
                hasHoliday = hasHoliday,
                contentDescription =
                    dayCellContentDescription(
                        formatter = formatter,
                        date = date,
                        isToday = isToday,
                        hasHoliday = hasHoliday,
                        hasEvent = hasEvent,
                        todayLabel = cellTodayLabel,
                        holidayLabel = cellHolidayLabel,
                        eventLabel = cellEventLabel,
                    ),
            )
        }

    val intercalary =
        month.trailingIntercalary?.let { day ->
            val gregorianDate = day.toLocalDate()
            MonthIntercalaryState(
                label = formatter.formatDay(day),
                subtitle = formatter.intercalarySubtitle(day),
                gregorianDate = gregorianDate,
                isToday = gregorianDate == today.gregorianDate,
                hasEvent = gregorianDate in eventDates,
                hasHoliday = gregorianDate in holidayDates,
            )
        }

    val nominalHeaders =
        List(GRID_COLUMNS) { column ->
            // The 1st..7th of any month are the nominal Sunday..Saturday in every year (spec §2.3, R6).
            formatter.weekdayName(
                IfcDate.Regular(month.year, month.month, column + 1).nominalDayOfWeek,
                WeekdayNameStyle.SHORT,
            )
        }
    val actualHeaders =
        List(GRID_COLUMNS) { column ->
            formatter.weekdayName(month.actualDayOfWeek(column), WeekdayNameStyle.SHORT)
        }

    val monthTitle = formatter.monthTitle(month)
    val todayDescription = formatter.dayDescription(today.ifcDate, isToday = true)
    val todayHasEvent = today.gregorianDate in eventDates
    val contentDescription =
        buildString {
            append(monthTitle)
            append(". ")
            append(todayDescription)
            if (todayHasEvent && hasEventsLabel.isNotEmpty()) {
                append(' ')
                append(hasEventsLabel)
            }
            append(' ')
            append(tapHint)
        }
    return MonthWidgetState(
        monthTitle = monthTitle,
        nominalWeekdayHeaders = nominalHeaders,
        actualWeekdayHeaders = actualHeaders,
        days = days,
        intercalary = intercalary,
        gregorianSpanLabel = formatter.gregorianSpan(month.gregorianRange),
        contentDescription = contentDescription,
    )
}

/**
 * The short TalkBack description for one grid cell (ROADMAP M8 T1, accessibility audit finding #1):
 * [date]'s day name alone ([IfcDateFormatter.formatDay], `Sol 13`) -- never a weekday, which is the
 * simplest way to satisfy calendar-spec §4.1 item 7's "any weekday shown must say which kind" (a cell
 * that names none cannot mislabel one) -- with [todayLabel], [holidayLabel] and [eventLabel] appended
 * after a comma, only for the qualifiers that actually apply to this cell, e.g. `Sol 13, today` or
 * `Sol 21, holiday, has events`. Each label is presence only: never a holiday's name or an event's
 * count or title (CLAUDE.md rule 8), the same restriction [buildMonthWidgetState]'s whole-widget
 * [MonthWidgetState.contentDescription] already observes for events. An empty label (the default, and
 * every existing test's default) omits that qualifier entirely rather than appending an empty word.
 *
 * **Supersedes the previous docs/ARCHITECTURE.md §5 ruling** that a single whole-widget description was
 * the only one this widget would carry, on the reasoning that 28 extra TalkBack stops were the greater
 * harm: a clickable cell with no description of its own is announced as a bare, context-free digit
 * (confirmed against Glance 1.2.0's actual `GlanceModifier.semantics`/`contentDescription` behaviour),
 * which is the worse outcome for a screen-reader user who can already tap that cell individually
 * (ROADMAP M3 T5's per-day routing) -- they can act on the cell but could not previously hear what it
 * was. Glance 1.2.0 still has no modifier to mark a *child* unimportant for accessibility, but that gap
 * does not block this fix: the day number, the Gregorian day and the mark glyphs beneath it are plain,
 * non-clickable `Text`s with no semantics of their own, so this description on the cell's own clickable
 * node is what TalkBack reports for the cell, exactly as the whole-widget description already works for
 * the title/header block above it.
 */
fun dayCellContentDescription(
    formatter: IfcDateFormatter,
    date: IfcDate,
    isToday: Boolean,
    hasHoliday: Boolean,
    hasEvent: Boolean,
    todayLabel: String = "",
    holidayLabel: String = "",
    eventLabel: String = "",
): String {
    val qualifiers =
        listOfNotNull(
            todayLabel.takeIf { isToday && it.isNotEmpty() },
            holidayLabel.takeIf { hasHoliday && it.isNotEmpty() },
            eventLabel.takeIf { hasEvent && it.isNotEmpty() },
        )
    val dayName = formatter.formatDay(date)
    return if (qualifiers.isEmpty()) dayName else "$dayName, ${qualifiers.joinToString(", ")}"
}
