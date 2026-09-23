package io.github.chrisjmendoza.yearal.widget.today

import android.content.res.Resources
import io.github.chrisjmendoza.yearal.core.calendar.IfcDate
import io.github.chrisjmendoza.yearal.core.calendar.IfcMonth
import io.github.chrisjmendoza.yearal.widget.R
import java.time.Year
import java.util.Locale

/**
 * The extra content [TodayGlanceWidget]'s [TodayGlanceWidget.LARGE] breakpoint shows: the year-progress
 * line and the next intercalary countdown (`docs/design-plan.md` §4.9, "large widgets should show
 * more"). `:widget` cannot depend on `:feature:calendar` (CLAUDE.md rule 10), so this is a from-scratch
 * port of `TodayUiState`'s own computation there, built on the same `:core:calendar` APIs
 * ([IfcDate.dayOfYear], [IfcDate.toLocalDate], [IfcDate.daysUntil]) rather than on that module's
 * internal `nextIntercalaryDay` function. Every word is read from `:widget`'s own string resources
 * (CLAUDE.md rule 9), not `:core:designsystem`'s.
 *
 * How far through [date]'s year it is, as text, `Day 260 of 365 · 71%`.
 */
internal fun yearProgressLabel(
    date: IfcDate,
    resources: Resources,
    locale: Locale,
): String {
    val dayOfYear = date.dayOfYear
    val length = date.toLocalDate().lengthOfYear()
    val percent = Math.round(dayOfYear.toFloat() / length * PERCENT_SCALE)
    return String.format(locale, resources.getString(R.string.today_widget_year_progress), dayOfYear, length, percent)
}

/**
 * The countdown to [nextIntercalaryDay], as text, `Year Day in 105 days` / `Leap Day in 1 day`; `null`
 * only when [date] is past the last supported year ([IfcDate.MAX_YEAR]).
 */
internal fun countdownLabel(
    date: IfcDate,
    resources: Resources,
    locale: Locale,
): String? {
    val target = nextIntercalaryDay(date) ?: return null
    val days = date.daysUntil(target).toInt()
    val targetName =
        when (target) {
            is IfcDate.LeapDay -> resources.getString(R.string.today_widget_leap_day)
            is IfcDate.YearDay -> resources.getString(R.string.today_widget_year_day)
            is IfcDate.Regular -> error("nextIntercalaryDay never returns a Regular date: $target")
        }
    return String.format(
        locale,
        resources.getQuantityString(R.plurals.today_widget_intercalary_countdown, days),
        targetName,
        days,
    )
}

/**
 * The first intercalary day strictly after [date]: Leap Day if [date] is in a leap year and before it
 * (June 28 or earlier), else Year Day of the same year; after Year Day, the first intercalary day of
 * the next year. `null` only past [IfcDate.MAX_YEAR].
 *
 * Spec: `docs/calendar-spec.md` §2.4 (Leap Day exists only in leap years, after June 28). Deliberately
 * the same shape as `feature/calendar`'s internal `nextIntercalaryDay` -- ported, not shared, since a
 * module may not depend on a feature (CLAUDE.md rule 10).
 */
internal fun nextIntercalaryDay(date: IfcDate): IfcDate? =
    when (date) {
        is IfcDate.Regular -> {
            val beforeLeapDay = date.month <= IfcMonth.JUNE
            (if (beforeLeapDay) leapDayOf(date.year) else null) ?: IfcDate.YearDay(date.year)
        }

        is IfcDate.LeapDay -> {
            IfcDate.YearDay(date.year)
        }

        is IfcDate.YearDay -> {
            val next = date.year + 1
            if (next > IfcDate.MAX_YEAR) null else leapDayOf(next) ?: IfcDate.YearDay(next)
        }
    }

/** Leap Day of [year], or `null` in a common year -- the only year a Leap Day exists in (CLAUDE.md rule 6). */
private fun leapDayOf(year: Int): IfcDate.LeapDay? = if (Year.isLeap(year.toLong())) IfcDate.LeapDay(year) else null

/** Percent is out of 100. */
private const val PERCENT_SCALE = 100
