package io.github.chrisjmendoza.yearal.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colour tokens layered on top of Material 3's [ColorScheme] (`docs/design-plan.md` §3.1
 * "Semantic colours"). Every property is *derived* from a Material role rather than a brand hex
 * value, so the same tokens work under a dynamic (wallpaper) scheme and under every curated
 * [ColorPalette][io.github.chrisjmendoza.yearal.core.domain.settings.ColorPalette] — see
 * [yearalColorsFrom] for the exact mapping. Read it through [YearalTheme] inside [IfcTheme], the same
 * way screens read `MaterialTheme.colorScheme`.
 *
 * @property todayRing the ring drawn around today's cell or band.
 * @property todayText the colour of today's day number and any "Today" badge.
 * @property heroContainer the fill of a hero card — the Today date card, the converter result card.
 * @property onHero content colour on [heroContainer].
 * @property intercalary the colour of the intercalary (Year Day / Leap Day) icon and accents.
 * @property intercalaryContainer the fill of the intercalary band, chip and tile.
 * @property onIntercalaryContainer content colour on [intercalaryContainer].
 * @property holidayMark the colour of the holiday diamond mark.
 * @property eventMark the colour of an event dot or agenda swatch with no colour of its own.
 * @property weekdayNominalContainer the fill of the "IFC weekday" header row and block.
 * @property onWeekdayNominalContainer content colour on [weekdayNominalContainer].
 * @property weekdayActualText the colour of the "Actual weekday" row's text, kept legible rather
 * than a plain grey (design-plan §4.8 "Decide the actual weekday label").
 * @property gridCell the fill of an ordinary day cell.
 * @property gridCellWeekend the fill of a day cell in the IFC week's Saturday/Sunday columns.
 * @property gridCellMarked the fill a marked cell (holiday or event) steps up to, one container tier
 * above [gridCell], so colour, shape and fill all say "something is here".
 * @property cardContainer the fill of an ordinary card (agenda rows, mini-months, list rows).
 * @property onCard content colour on [cardContainer].
 * @property pageBackground the fill of a screen behind its cards.
 */
@Immutable
public class YearalColors(
    public val todayRing: Color,
    public val todayText: Color,
    public val heroContainer: Color,
    public val onHero: Color,
    public val intercalary: Color,
    public val intercalaryContainer: Color,
    public val onIntercalaryContainer: Color,
    public val holidayMark: Color,
    public val eventMark: Color,
    public val weekdayNominalContainer: Color,
    public val onWeekdayNominalContainer: Color,
    public val weekdayActualText: Color,
    public val gridCell: Color,
    public val gridCellWeekend: Color,
    public val gridCellMarked: Color,
    public val cardContainer: Color,
    public val onCard: Color,
    public val pageBackground: Color,
)

/**
 * Derives [YearalColors] from [scheme]'s Material roles (`docs/design-plan.md` §3.1, the token
 * table). Every field comes from a role, never a brand constant, so a dynamic (wallpaper) scheme
 * produces a coherent [YearalColors] too:
 *
 * | Token | Role |
 * |---|---|
 * | `todayRing`, `todayText` | `primary` |
 * | `heroContainer`, `onHero` | `primaryContainer`, `onPrimaryContainer` |
 * | `intercalary` | `tertiary` |
 * | `intercalaryContainer`, `onIntercalaryContainer` | `tertiaryContainer`, `onTertiaryContainer` |
 * | `holidayMark` | `tertiary` |
 * | `eventMark` | `primary` |
 * | `weekdayNominalContainer`, `onWeekdayNominalContainer` | `secondaryContainer`, `onSecondaryContainer` |
 * | `weekdayActualText` | `onSurfaceVariant` (≥4.5:1 on `surface` in every scheme this app ships) |
 * | `gridCell` | `surfaceContainerLow` |
 * | `gridCellWeekend` | `surfaceContainer` |
 * | `gridCellMarked` | `surfaceContainerHigh` |
 * | `cardContainer`, `onCard` | `surfaceContainerLow`, `onSurface` |
 * | `pageBackground` | `surface` |
 */
public fun yearalColorsFrom(scheme: ColorScheme): YearalColors =
    YearalColors(
        todayRing = scheme.primary,
        todayText = scheme.primary,
        heroContainer = scheme.primaryContainer,
        onHero = scheme.onPrimaryContainer,
        intercalary = scheme.tertiary,
        intercalaryContainer = scheme.tertiaryContainer,
        onIntercalaryContainer = scheme.onTertiaryContainer,
        holidayMark = scheme.tertiary,
        eventMark = scheme.primary,
        weekdayNominalContainer = scheme.secondaryContainer,
        onWeekdayNominalContainer = scheme.onSecondaryContainer,
        weekdayActualText = scheme.onSurfaceVariant,
        gridCell = scheme.surfaceContainerLow,
        gridCellWeekend = scheme.surfaceContainer,
        gridCellMarked = scheme.surfaceContainerHigh,
        cardContainer = scheme.surfaceContainerLow,
        onCard = scheme.onSurface,
        pageBackground = scheme.surface,
    )

/**
 * The [YearalColors] in scope, provided by [IfcTheme]. Read it through [YearalTheme.colors] rather
 * than this local directly, the same way code reads `MaterialTheme.colorScheme` instead of a raw
 * `LocalColorScheme`.
 */
public val LocalYearalColors: ProvidableCompositionLocal<YearalColors> =
    compositionLocalOf { yearalColorsFrom(BrandLightColorScheme) }

/**
 * Accessor for the semantic colour tokens [IfcTheme] provides, mirroring `MaterialTheme.colorScheme`.
 * Using this instead of [LocalYearalColors] directly keeps every call site symmetric with the rest of
 * the Material theme API.
 */
public object YearalTheme {
    /** The [YearalColors] provided by the nearest [IfcTheme]. */
    public val colors: YearalColors
        @Composable
        @ReadOnlyComposable
        get() = LocalYearalColors.current
}
