package io.github.chrisjmendoza.yearal.core.scheduling.reminder

/**
 * The typed contract a reminder notification's tap intent carries (ROADMAP M4 T10; FEATURES E4).
 * `:app`'s `IntentRouter` reads this exact action and extra name to open the tapped event's own
 * editor instead of the app's normal start destination (`docs/ARCHITECTURE.md` §4 "Intent routing").
 *
 * **Where this lives, and why.** `:core:scheduling` cannot depend on `:core:navigation` or `:app`
 * (dependency direction, `docs/ARCHITECTURE.md` §2), and `:core:navigation`'s `NavKey` types require
 * `androidx.navigation3:navigation3-runtime` as an `api` dependency, which transitively pulls in
 * `androidx.compose.runtime` and `androidx.compose.runtime:runtime-saveable` (verified from that
 * artifact's own POM) — heavy, Compose-shaped dependencies with no place in a headless alarm/
 * notification module that has never needed the Compose compiler. Pulling `androidx.navigation3` into
 * `:core:scheduling` was judged **not acceptable** for that reason. `:core:domain` is the other module
 * both a producer and `:app` already share, but this task is scoped to `:app`, `:core:navigation`,
 * `:core:scheduling` and `:widget` only. So this contract stays local to the one producer that needs
 * it: a plain action string plus one `Long` extra name, no Nav3 types, no Android `Intent`-building
 * logic beyond what [ReminderNotifier] already does. `:app` already depends on `:core:scheduling`
 * (`docs/ARCHITECTURE.md` §2: "Only `:app` depends on `:core:data`, `:core:scheduling`, ... and
 * `:widget`"), so it references these constants directly — one source of truth, verified by
 * `IntentRouterTest` and `ReminderNotifierTest` rather than by a shared module.
 */
public object ReminderIntent {
    /** The action of the explicit intent a reminder notification's tap opens. */
    public const val ACTION_OPEN_EVENT: String = "io.github.chrisjmendoza.yearal.action.OPEN_EVENT"

    /**
     * Extra name for the event id (`Long`) to open. IDs only, never event content (CLAUDE.md rule 8) —
     * the title, time and every other detail are recomputed by the app from the id, never carried here.
     */
    public const val EXTRA_EVENT_ID: String = "io.github.chrisjmendoza.yearal.extra.EVENT_ID"
}
