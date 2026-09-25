package io.github.chrisjmendoza.yearal.intent

import android.content.Intent
import io.github.chrisjmendoza.yearal.core.calendar.IfcDate
import io.github.chrisjmendoza.yearal.core.scheduling.reminder.ReminderIntent
import io.github.chrisjmendoza.yearal.widget.WidgetIntents
import java.time.LocalDate

/**
 * Turns an incoming `Intent` — from a widget tap or a reminder notification tap — into an [AppRoute]
 * (ROADMAP M3 T5, M4 T10; `docs/ARCHITECTURE.md` §4 "Intent routing"). This is the one place in the
 * app that reads [WidgetIntents] and [ReminderIntent], the two producer-owned contracts; see their own
 * KDoc for why each stays local to its producer module rather than living in a module the other could
 * also reach.
 *
 * **Validation is the point** (`docs/security-and-privacy.md` §6.3). [resolve] reads only:
 * - `intent.action`, compared by exact string equality against the four known actions;
 * - [WidgetIntents.EXTRA_EPOCH_DAY] and [ReminderIntent.EXTRA_EVENT_ID], each read through
 *   [longExtraOrNull], which requires the stored value to actually **be** a `Long` — an `as? Long`
 *   cast, not a `getLongExtra` default — so a value of any other runtime type is indistinguishable
 *   from the extra being absent, never coerced into some default number and routed anyway.
 *
 * No other field of the intent is ever read: no `intent.data`, no `getParcelableExtra`, no
 * `getSerializableExtra`, no reflection on any extra's class or on the intent's component. An intent
 * carrying a URI, a file path, a class name, or a nested `Intent` — under any key, including the exact
 * extra names above — therefore always resolves to a missing extra and falls back to [AppRoute.Default],
 * never an exception, never a route built from it.
 *
 * An unrecognized or missing action, or an extra that fails its own check (see [dayRoute],
 * [eventRoute]), also resolves to [AppRoute.Default] — "ignore and show the normal start destination"
 * (`docs/security-and-privacy.md` §6.3's "Main activity (launcher)" row).
 */
object IntentRouter {
    /** Resolves [intent] to the [AppRoute] `:app` should apply. Pure; never throws. */
    fun resolve(intent: Intent): AppRoute =
        when (intent.action) {
            WidgetIntents.ACTION_OPEN_TODAY -> AppRoute.Today
            WidgetIntents.ACTION_OPEN_MONTH -> AppRoute.CurrentMonth
            WidgetIntents.ACTION_OPEN_DAY -> dayRoute(intent)
            ReminderIntent.ACTION_OPEN_EVENT -> eventRoute(intent)
            else -> AppRoute.Default
        }

    /**
     * [WidgetIntents.ACTION_OPEN_DAY]: the epoch day must be present as an actual `Long` and must
     * convert to a date within `:core:calendar`'s supported years,
     * [IfcDate.MIN_YEAR]..[IfcDate.MAX_YEAR] — the same fail-soft check `MonthPages.selectedDateOf`
     * and `EventEditorViewModel.validEpochDayToDate` already apply to their own untrusted epoch-day
     * inputs, kept here rather than shared because both are private to their own module.
     * `Long.MIN_VALUE`/`Long.MAX_VALUE` fail at `LocalDate.ofEpochDay` itself; a year 0 or year 10000
     * epoch day parses fine but fails the year-range check.
     */
    private fun dayRoute(intent: Intent): AppRoute {
        val epochDay = longExtraOrNull(intent, WidgetIntents.EXTRA_EPOCH_DAY) ?: return AppRoute.Default
        val date = runCatching { LocalDate.ofEpochDay(epochDay) }.getOrNull() ?: return AppRoute.Default
        return if (date.year in IfcDate.MIN_YEAR..IfcDate.MAX_YEAR) AppRoute.Day(epochDay) else AppRoute.Default
    }

    /**
     * [ReminderIntent.ACTION_OPEN_EVENT]: the event id must be present as an actual `Long` and
     * structurally valid (a positive Room row id). Whether it still names an existing event cannot be
     * checked here without a database read (CLAUDE.md rule 8 keeps this router to ids only); a
     * since-deleted event's id still resolves to [AppRoute.EventDetail], and the editor's own "not
     * found" state handles it (`EventEditorViewModel`'s `notFound` flag) rather than a second,
     * router-level check.
     */
    private fun eventRoute(intent: Intent): AppRoute {
        val eventId = longExtraOrNull(intent, ReminderIntent.EXTRA_EVENT_ID) ?: return AppRoute.Default
        return if (eventId > 0) AppRoute.EventDetail(eventId) else AppRoute.Default
    }

    /**
     * [key]'s extra, only if it is actually stored as a `Long` — distinguished from
     * [Intent.getLongExtra]'s ordinary type-mismatch-to-default behaviour (which would make a `Uri`, a
     * nested `Intent`, or any other wrong-typed value under this exact key resolve to a plain number
     * indistinguishable from a real one) by reading it twice with two different, arbitrary defaults:
     * a genuine stored `Long` comes back identically both times regardless of either default, while a
     * missing or wrong-typed extra comes back as each call's own distinct default, so the two reads
     * disagree. `null` for a missing extra and for one of any other type alike. (`Bundle.get(String)`
     * would answer this directly but is deprecated; this uses only the typed, non-deprecated getter.)
     */
    private fun longExtraOrNull(
        intent: Intent,
        key: String,
    ): Long? {
        val readWithDefaultA = intent.getLongExtra(key, ABSENT_SENTINEL_A)
        val readWithDefaultB = intent.getLongExtra(key, ABSENT_SENTINEL_B)
        return readWithDefaultA.takeIf { it == readWithDefaultB }
    }

    private const val ABSENT_SENTINEL_A = Long.MIN_VALUE
    private const val ABSENT_SENTINEL_B = Long.MIN_VALUE + 1
}
