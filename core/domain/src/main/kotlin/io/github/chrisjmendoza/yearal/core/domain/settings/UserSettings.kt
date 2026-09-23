package io.github.chrisjmendoza.yearal.core.domain.settings

/**
 * How weekday headers are shown on IFC grids (`docs/ARCHITECTURE.md` §4 "Intercalary days in a
 * 7-column grid"; FEATURES W1). The IFC week always starts on Sunday, so headers never depend on the
 * device's first-day-of-week setting.
 *
 * **Trap:** the nominal IFC weekday is not the real weekday (calendar-spec §4.1). Anything tied to
 * real life — today highlight, events, reminders — uses the actual weekday whatever this setting says.
 */
public enum class WeekdayDisplay {
    /** Only the perpetual IFC weekday names (Sunday … Saturday, identical for every month). */
    NOMINAL,

    /** Only the real weekdays of that month's seven columns. */
    ACTUAL,

    /** Both: nominal names with the actual weekdays in a second header row. The default. */
    BOTH,
}

/** The app's colour theme (FEATURES W2). */
public enum class ThemeMode {
    /** Follow the system dark/light setting. */
    SYSTEM,
    LIGHT,
    DARK,
}

/**
 * Every user preference, as one immutable value stored in DataStore (`docs/ARCHITECTURE.md` §1
 * "datastore"; FEATURES W1, W2, H5, W6; the colour fields are `docs/design-plan.md` §5). Defaults are
 * the values of a fresh install and are also what a corrupt or missing store falls back to.
 *
 * @property weekdayDisplay the grid header mode; default [WeekdayDisplay.BOTH] (ROADMAP decision #5).
 * @property themeMode dark/light/system; default [ThemeMode.SYSTEM].
 * @property colorSource where the colour scheme comes from — the curated Yearal palette or Material
 * You wallpaper colour on API 31+ (`docs/design-plan.md` §5.1). Default [ColorSource.BRAND]. Replaces
 * the earlier `dynamicColor` boolean: because the on-disk JSON ignores unknown keys, a file still
 * carrying that key simply reads with this property's default, `BRAND`, on every install — the
 * one-time pre-1.0 migration `docs/design-plan.md` §5.1 and §8.1 call for.
 * @property palette which curated scheme to draw from when [colorSource] is [ColorSource.BRAND]
 * (`docs/design-plan.md` §5.2). Default [ColorPalette.TEAL], the launcher icon's scheme. Ignored when
 * [colorSource] is [ColorSource.DYNAMIC].
 * @property pureBlack true forces `surface`, `background` and the container tiers of whichever dark
 * scheme is active (brand or Material You) to true black and tight greys (`docs/design-plan.md` §5.3,
 * the AMOLED option). No visible effect while a light scheme is shown. Default `false`.
 * @property enabledHolidaySets ids of the holiday packs shown on grids and agendas (the pack `id`
 * field, e.g. `"ifc"`, `"us"`), default the IFC observances and the US pack (ROADMAP decision #8).
 * @property hasSeenIntro whether the first-run intro (`docs/FEATURES.md` L1) has been shown and
 * dismissed (skipped or finished) at least once. `false` for a fresh install **and** for an install
 * whose stored file predates this field (`UserSettingsSerializer`'s backward-compatibility default) —
 * both cases are indistinguishable and both mean "show the intro", which is the sensible fallback for
 * either. Never set back to `false` by the app itself; only a fresh or pre-intro install starts `false`.
 * @property todayWidgetTheme light/dark override for the Today home-screen widget, independent of
 * [themeMode] (`docs/design-plan.md` §5.6). Default [WidgetTheme.FOLLOW_APP]. Global for the widget
 * type, not per placed instance (`docs/design-plan.md` §5.6 explains why).
 * @property monthWidgetTheme light/dark override for the Month home-screen widget
 * (`docs/design-plan.md` §5.6). Default [WidgetTheme.FOLLOW_APP].
 * @property widgetBackgroundOpacity widget background opacity as a percentage, **`0..100`**
 * (`docs/design-plan.md` §5.6). Default `100` (fully opaque). A stored value outside this range is
 * coerced into it when the settings file is read, rather than being treated as a corrupt file — see
 * `UserSettingsDto.toDomain`.
 */
public data class UserSettings(
    val weekdayDisplay: WeekdayDisplay = WeekdayDisplay.BOTH,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val colorSource: ColorSource = ColorSource.BRAND,
    val palette: ColorPalette = ColorPalette.TEAL,
    val pureBlack: Boolean = false,
    val enabledHolidaySets: Set<String> = setOf("ifc", "us"),
    val hasSeenIntro: Boolean = false,
    val todayWidgetTheme: WidgetTheme = WidgetTheme.FOLLOW_APP,
    val monthWidgetTheme: WidgetTheme = WidgetTheme.FOLLOW_APP,
    val widgetBackgroundOpacity: Int = 100,
) {
    /** Well-known values. */
    public companion object {
        /** The settings of a fresh install. */
        public val DEFAULT: UserSettings = UserSettings()
    }
}
