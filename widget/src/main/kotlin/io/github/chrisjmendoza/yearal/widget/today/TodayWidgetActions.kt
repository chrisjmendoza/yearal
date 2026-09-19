package io.github.chrisjmendoza.yearal.widget.today

import android.content.Context
import android.content.Intent
import io.github.chrisjmendoza.yearal.widget.WidgetIntents

/**
 * The explicit intent every widget tap action is built from (FEATURES S5): the app's own launcher
 * activity, resolved by [android.content.pm.PackageManager] rather than by naming `MainActivity`
 * directly.
 *
 * `:widget` is depended on only by `:app` (docs/ARCHITECTURE.md §2 "Dependency direction"), so it
 * cannot import an `:app` class at compile time. `getLaunchIntentForPackage` asks the platform to
 * resolve this package's `MAIN`/`LAUNCHER` activity and returns an intent whose component is already
 * set to it — that is what makes the result **explicit** (`docs/security-and-privacy.md` §6.4) without
 * this module knowing the activity's class name. Carries no action or extras of its own; callers such
 * as [todayLaunchIntent], [monthLaunchIntent] and [dayLaunchIntent] add [WidgetIntents]'s action and,
 * for a specific day, its one `Long` extra.
 *
 * @return an explicit intent with no extras, or `null` only if the package manager cannot resolve a
 *   launcher activity for this app's own package — a state the widget cannot recover from, so the
 *   caller should treat it as "there is nothing to launch" rather than substitute a guessed intent.
 */
fun launchAppIntent(context: Context): Intent? =
    context.packageManager
        .getLaunchIntentForPackage(context.packageName)
        // The launcher activity is `singleTask` (app/src/main/AndroidManifest.xml); FLAG_ACTIVITY_NEW_TASK
        // is required to start an activity from outside an activity context (the widget's own process).
        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

/**
 * The Today widget's whole-widget tap target (ROADMAP M3 T5): [launchAppIntent] plus
 * [WidgetIntents.ACTION_OPEN_TODAY], so `:app`'s `IntentRouter` selects the Today tab explicitly
 * instead of relying on it already being the default start destination.
 */
fun todayLaunchIntent(context: Context): Intent? = launchAppIntent(context)?.setAction(WidgetIntents.ACTION_OPEN_TODAY)

/**
 * The Month widget's whole-widget tap target (ROADMAP M3 T5) — everywhere on the widget that is not
 * one of its own day cells or its intercalary band (the title, the weekday header rows, the Gregorian
 * span line): [launchAppIntent] plus [WidgetIntents.ACTION_OPEN_MONTH], which opens the current month
 * on the Calendar tab. Carries no epoch day; the app resolves "current" itself.
 */
fun monthLaunchIntent(context: Context): Intent? = launchAppIntent(context)?.setAction(WidgetIntents.ACTION_OPEN_MONTH)

/**
 * One Month-widget day cell's, or its intercalary band's, tap target (ROADMAP M3 T5; FEATURES S5):
 * [launchAppIntent] plus [WidgetIntents.ACTION_OPEN_DAY] and [epochDay] under
 * [WidgetIntents.EXTRA_EPOCH_DAY] — an id only, never the day's content (CLAUDE.md rule 8). `:app`'s
 * `IntentRouter` validates it before use and fails soft (falls back to the normal start destination)
 * for an epoch day outside `:core:calendar`'s supported years, so an out-of-range value here is never
 * a crash risk.
 *
 * @param epochDay the Gregorian epoch day (`LocalDate.toEpochDay()`) to open.
 */
fun dayLaunchIntent(
    context: Context,
    epochDay: Long,
): Intent? =
    launchAppIntent(context)
        ?.setAction(WidgetIntents.ACTION_OPEN_DAY)
        ?.putExtra(WidgetIntents.EXTRA_EPOCH_DAY, epochDay)
