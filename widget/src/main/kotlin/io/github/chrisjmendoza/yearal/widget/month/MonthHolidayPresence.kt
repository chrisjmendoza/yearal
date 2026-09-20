package io.github.chrisjmendoza.yearal.widget.month

import io.github.chrisjmendoza.yearal.core.domain.holiday.HolidayEngine
import io.github.chrisjmendoza.yearal.core.domain.holiday.HolidaySetProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate

/**
 * A bounded-time, best-effort snapshot of which dates in [range] carry a holiday, for the Month
 * widget's holiday marks (FEATURES C4, S2; docs/ARCHITECTURE.md §5 "Widget types" item 2, "Data").
 * The same shape and the same guarantees as [fetchMonthEventPresence] beside it, for the same reasons:
 * **one query per render**, never a continuous collection, and a render that **must never hang or
 * crash** because a pack is slow to parse or the settings store is unreadable — the month simply draws
 * without holiday marks, exactly as if no set were enabled. [CancellationException] is rethrown so a
 * cancelled `provideGlance` still cancels promptly.
 *
 * Only *presence* is computed, never a holiday's name: the widget draws a mark, and a name would be
 * event-like content on a home screen (the app's own grid cell is the place for labels, FEATURES C5).
 * That is also why this needs no `HolidayPackLoader`, no resources and no `:core:holidays` dependency —
 * [HolidaySetProvider] already resolves "which packs are on" from the user's settings, and
 * [HolidayEngine] turns those sets into dates.
 *
 * @param provider the enabled holiday sets, normally resolved through
 *   [io.github.chrisjmendoza.yearal.widget.di.WidgetEntryPoint].
 * @param engine the shared memoising evaluator, so this costs a map lookup once a month has been seen.
 * @param range the shown IFC month's Gregorian span, `IfcYearMonth.gregorianRange` (28 or 29 days, the
 *   trailing Leap Day / Year Day band included — those days carry holidays like any other, CLAUDE.md
 *   rule 6).
 */
internal suspend fun fetchMonthHolidays(
    provider: HolidaySetProvider,
    engine: HolidayEngine,
    range: ClosedRange<LocalDate>,
): Set<LocalDate> =
    try {
        withTimeoutOrNull(PRESENCE_TIMEOUT_MILLIS) {
            val sets = provider.enabledSets().first()
            if (sets.isEmpty()) emptySet() else engine.occurrences(sets, range).mapTo(HashSet()) { it.date }
        } ?: emptySet()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Any other failure (an unreadable settings store, a malformed pack) renders without holiday
        // marks rather than crashing the widget's background render.
        emptySet()
    }
