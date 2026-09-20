package io.github.chrisjmendoza.yearal.feature.settings.intro

import io.github.chrisjmendoza.yearal.core.calendar.IfcMonth
import io.github.chrisjmendoza.yearal.feature.settings.learn.LearnFacts

/**
 * The worked-example facts shown on the first-run intro (`docs/FEATURES.md` L1), computed from
 * `:core:calendar` rather than typed as string literals (CLAUDE.md rule 1), exactly like [LearnFacts] —
 * which this reuses directly for the nominal-vs-actual weekday example and the two intercalary days, so
 * the intro and the Learn screen can never state the fact differently. [IntroFactsTest] checks the
 * values this object adds on its own (the month count, day count and Sol's position) against
 * `docs/calendar-spec.md` §2.2.
 */
internal object IntroFacts {
    /** There are exactly 13 IFC months (calendar-spec §2.2 R4), read from the enum rather than hard-coded. */
    val monthCount: Int = IfcMonth.entries.size

    /** Every month has exactly 28 days (calendar-spec §2.2 R4, §2.3 R5). */
    val daysPerMonth: Int = IfcMonth.DAYS_PER_MONTH

    /** Sol is IFC month number 7 (calendar-spec §2.2 R4): it sits between June (6) and July (8). */
    val solNumber: Int = IfcMonth.SOL.number

    /** The month immediately before Sol (calendar-spec §2.2 R4). */
    val monthBeforeSol: IfcMonth = IfcMonth.JUNE

    /** The month immediately after Sol (calendar-spec §2.2 R4). */
    val monthAfterSol: IfcMonth = IfcMonth.JULY

    /**
     * The spec's own nominal-vs-actual weekday illustration (calendar-spec §4.1), reused unchanged from
     * [LearnFacts] so the intro's "why weekdays differ" screen and the Learn screen's own section always
     * agree.
     */
    val weekdayExampleGregorian = LearnFacts.weekdayExampleGregorian
    val weekdayExampleIfc = LearnFacts.weekdayExampleIfc

    /** Year Day of the common-year example (calendar-spec §2.4 R8), reused from [LearnFacts]. */
    val yearDayExample = LearnFacts.yearDayExample

    /** Leap Day of the leap-year example (calendar-spec §2.4 R9), reused from [LearnFacts]. */
    val leapDayExample = LearnFacts.leapDayExample
}
