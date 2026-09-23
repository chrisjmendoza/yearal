package io.github.chrisjmendoza.yearal.core.domain.settings

/**
 * How a home-screen widget picks light or dark (`docs/design-plan.md` §5.6). One value per widget
 * type, stored on [UserSettings]; widgets always take their palette and colour source from the app.
 */
public enum class WidgetTheme {
    /** Light or dark as the app's [ThemeMode] resolves it, so "follow system" when the app does. */
    FOLLOW_APP,

    /** Always the light scheme, whatever the system or the app is set to. */
    LIGHT,

    /** Always the dark scheme. */
    DARK,
}
