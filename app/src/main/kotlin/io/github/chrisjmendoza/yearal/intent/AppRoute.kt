package io.github.chrisjmendoza.yearal.intent

/**
 * What [IntentRouter] decided to do with an incoming `Intent` (ROADMAP M3 T5, M4 T10;
 * `docs/ARCHITECTURE.md` §4 "Intent routing"). [ui.IfcApp][io.github.chrisjmendoza.yearal.ui.IfcApp]
 * applies one of these to [io.github.chrisjmendoza.yearal.ui.navigation.TabBackStacks] once and
 * discards it — see [io.github.chrisjmendoza.yearal.ui.navigation.applyRoute].
 *
 * [Default] is deliberately the only case with no payload to validate: every other case has already
 * passed [IntentRouter]'s validation (`docs/security-and-privacy.md` §6.3 "Validate and fail soft on
 * unknown IDs") by the time it exists, so applying a route never itself needs to fail.
 */
sealed interface AppRoute {
    /**
     * Show the app's normal start destination, unchanged: an unrecognized or missing action, or an id
     * or date that failed validation. This is the fail-soft outcome CLAUDE.md rule 8 and
     * `docs/security-and-privacy.md` §6.3 require — never a crash, never a guess.
     */
    data object Default : AppRoute

    /** Select the Today tab (the Today widget's whole-widget tap). */
    data object Today : AppRoute

    /**
     * Select the Calendar tab, showing the current month (the Month widget's whole-widget tap — its
     * title, header rows and Gregorian-span line, not one of its day cells). Resolved against the
     * app's own clock when applied, not against anything carried in the intent.
     */
    data object CurrentMonth : AppRoute

    /**
     * Select the Calendar tab, showing [epochDay]'s month and day (a Month-widget day-cell tap; a
     * future per-item tap on any other date-shaped surface).
     *
     * @property epochDay a Gregorian epoch day (`LocalDate.toEpochDay()`), already checked by
     * [IntentRouter] to lie within `:core:calendar`'s supported years.
     */
    data class Day(
        val epochDay: Long,
    ) : AppRoute

    /**
     * Select the Events tab, opening [eventId]'s editor (a reminder notification's tap).
     *
     * @property eventId the event id, structurally valid (positive) but not necessarily one that still
     * exists — [feature.events.editor.EventEditorViewModel][io.github.chrisjmendoza.yearal.feature.events.editor.EventEditorViewModel]
     * already shows its own "not found" state for that case (CLAUDE.md rule 8: ids only, so
     * [IntentRouter] cannot check existence itself without a database read).
     */
    data class EventDetail(
        val eventId: Long,
    ) : AppRoute
}
