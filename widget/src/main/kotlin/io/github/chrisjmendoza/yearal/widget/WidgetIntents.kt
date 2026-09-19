package io.github.chrisjmendoza.yearal.widget

/**
 * The typed contract a widget tap's launch intent carries (ROADMAP M3 T5, M5 T1/T3; FEATURES S5).
 * `:app`'s `IntentRouter` reads these exact actions and the one extra name to open the Today tab, the
 * current month, or a specific tapped day instead of the app's normal start destination
 * (`docs/ARCHITECTURE.md` §4 "Intent routing").
 *
 * **Where this lives, and why.** `:widget` cannot depend on `:core:navigation` or `:app`
 * (dependency direction, `docs/ARCHITECTURE.md` §2), and `:core:navigation`'s `NavKey` types require
 * `androidx.navigation3:navigation3-runtime` as an `api` dependency, which transitively pulls in
 * `androidx.compose.runtime`/`runtime-saveable` (verified from that artifact's own POM) — a UI
 * back-stack model this Glance-only module has no other reason to carry. This task judged pulling
 * `androidx.navigation3` into a producer module not acceptable for that reason (the same call
 * [io.github.chrisjmendoza.yearal.core.scheduling.reminder.ReminderIntent] makes for
 * `:core:scheduling`), so this contract stays local: a plain set of action strings plus one `Long`
 * extra name, no Nav3 types. `:app` already depends on `:widget`
 * (`docs/ARCHITECTURE.md` §2: "Only `:app` depends on ... `:widget`"), so it references these
 * constants directly — one source of truth, verified by `IntentRouterTest` and this module's own
 * launch-intent tests rather than by a shared module.
 */
public object WidgetIntents {
    /** The Today widget's whole-widget tap: open the Today tab. */
    public const val ACTION_OPEN_TODAY: String = "io.github.chrisjmendoza.yearal.action.OPEN_TODAY"

    /**
     * The Month widget's whole-widget tap (its title, header rows and Gregorian-span line, i.e.
     * anywhere not covered by a day cell or the intercalary band): open the current month on the
     * Calendar tab. No extra — the app resolves "current" from its own clock, exactly as the widget
     * itself does (CLAUDE.md rule 2).
     */
    public const val ACTION_OPEN_MONTH: String = "io.github.chrisjmendoza.yearal.action.OPEN_MONTH"

    /**
     * A tap on one of the Month widget's 28 day cells, or its trailing Leap Day / Year Day band: open
     * that specific day. Carries [EXTRA_EPOCH_DAY] and nothing else (CLAUDE.md rule 8).
     */
    public const val ACTION_OPEN_DAY: String = "io.github.chrisjmendoza.yearal.action.OPEN_DAY"

    /**
     * Extra name for the Gregorian epoch day (`Long`, `LocalDate.toEpochDay()`; CLAUDE.md rule 4) to
     * open with [ACTION_OPEN_DAY].
     */
    public const val EXTRA_EPOCH_DAY: String = "io.github.chrisjmendoza.yearal.extra.EPOCH_DAY"
}
