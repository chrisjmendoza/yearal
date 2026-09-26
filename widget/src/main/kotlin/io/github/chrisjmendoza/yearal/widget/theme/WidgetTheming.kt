package io.github.chrisjmendoza.yearal.widget.theme

import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProviders
import io.github.chrisjmendoza.yearal.core.designsystem.theme.colorSchemes
import io.github.chrisjmendoza.yearal.core.designsystem.theme.pureBlack
import io.github.chrisjmendoza.yearal.core.domain.settings.ColorSource
import io.github.chrisjmendoza.yearal.core.domain.settings.ThemeMode
import io.github.chrisjmendoza.yearal.core.domain.settings.UserSettings
import io.github.chrisjmendoza.yearal.core.domain.settings.WidgetTheme
import androidx.glance.material3.ColorProviders as Material3ColorProviders

/**
 * Widgets follow the app's appearance (`docs/design-plan.md` §4.9 "Follow the app's theme", §5.6
 * "Widget appearance"; `docs/ARCHITECTURE.md` §5). This file is the pure resolution logic behind that
 * — everything a [androidx.glance.appwidget.GlanceAppWidget.provideGlance] call needs to pick colours,
 * kept free of `Context`/`LocalContext`/`GlanceTheme` so it is unit-testable without Robolectric.
 *
 * Whether a widget's own dark/light choice is forced, and to which mode, for [widgetTheme] (one
 * widget type's [UserSettings.todayWidgetTheme] or [UserSettings.monthWidgetTheme]) falling back to
 * the app's [themeMode] when [widgetTheme] is [WidgetTheme.FOLLOW_APP] (`docs/design-plan.md` §5.6):
 *
 * - [WidgetTheme.LIGHT] / [WidgetTheme.DARK] force that scheme regardless of the system.
 * - [WidgetTheme.FOLLOW_APP] defers to [themeMode]: [ThemeMode.LIGHT] / [ThemeMode.DARK] force the
 *   same way; [ThemeMode.SYSTEM] forces nothing.
 *
 * @return `true` to force the dark scheme, `false` to force light, `null` to force neither — the
 * caller then builds [androidx.glance.material3.ColorProviders] with both the day and night slot set
 * (light and dark respectively) so Glance itself follows the system UI mode at render time, exactly
 * the way [androidx.glance.color.ColorProvider]'s day/night pair already works.
 */
public fun resolveWidgetDark(
    themeMode: ThemeMode,
    widgetTheme: WidgetTheme,
): Boolean? =
    when (widgetTheme) {
        WidgetTheme.LIGHT -> {
            false
        }

        WidgetTheme.DARK -> {
            true
        }

        WidgetTheme.FOLLOW_APP -> {
            when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> null
            }
        }
    }

/**
 * The [ColorProviders] a widget should theme itself with for [settings] and its own [widgetTheme]
 * ([UserSettings.todayWidgetTheme] or [UserSettings.monthWidgetTheme]), or `null` when the caller
 * should use [androidx.glance.GlanceTheme.colors] instead (Material You dynamic colour).
 *
 * Mirrors [io.github.chrisjmendoza.yearal.core.designsystem.theme.IfcTheme]'s own resolution order
 * with one deliberate simplification for Glance: when [UserSettings.colorSource] is
 * [ColorSource.DYNAMIC] and [sdkInt] is 31 (`Build.VERSION_CODES.S`) or later, this returns `null` and
 * the caller keeps using `GlanceTheme.colors` exactly as before this task (docs/ARCHITECTURE.md §5,
 * "`GlanceTheme` uses Material You dynamic colour on API 31+") — Glance's own dynamic colour set
 * already follows the *system* dark/light mode the same way the app's dynamic path does, and building
 * an equivalent forced [ColorProviders] would need a wallpaper-derived
 * [androidx.compose.material3.ColorScheme] built from a `Context` this pure function deliberately does
 * not take. The practical effect: a widget's own light/dark override ([widgetTheme]) has no effect
 * while Material You colour is on; only the curated-palette path below honours it. Below [sdkInt] 31,
 * [ColorSource.DYNAMIC] behaves as [ColorSource.BRAND] (`ColorSource`'s own KDoc), so this always
 * returns a non-null [ColorProviders] there.
 *
 * For the curated-palette path: [UserSettings.palette]'s [colorSchemes] pair is used, with
 * [UserSettings.pureBlack] applied to the *dark* member only ([pureBlack] has no light-mode effect —
 * `docs/design-plan.md` §5.3). [resolveWidgetDark] then decides which of the two schemes actually
 * shows: forced dark uses the dark scheme for both the day and night slot, forced light uses the light
 * scheme for both, and "follow system" (`null`) puts light in the day slot and dark in the night slot
 * so Glance picks by the system UI mode.
 */
public fun resolveWidgetColors(
    settings: UserSettings,
    widgetTheme: WidgetTheme,
    sdkInt: Int,
): ColorProviders? {
    if (settings.colorSource == ColorSource.DYNAMIC && sdkInt >= Build.VERSION_CODES.S) return null
    val schemes = settings.palette.colorSchemes()
    val light = schemes.light
    val dark = if (settings.pureBlack) schemes.dark.pureBlack() else schemes.dark
    return when (resolveWidgetDark(settings.themeMode, widgetTheme)) {
        true -> Material3ColorProviders(light = dark, dark = dark)
        false -> Material3ColorProviders(light = light, dark = light)
        null -> Material3ColorProviders(light = light, dark = dark)
    }
}

/** [UserSettings.widgetBackgroundOpacity] coerced into `0..100` and converted to an alpha fraction. */
public fun widgetBackgroundAlpha(opacityPercent: Int): Float = opacityPercent.coerceIn(0, 100) / 100f

/**
 * [color] with [opacityPercent] (`0..100`, `docs/design-plan.md` §5.6) applied as its alpha — the
 * widget background colour a widget's outermost `GlanceModifier.background` uses. Never applied to a
 * day cell's or the intercalary band's own fill (`docs/design-plan.md` §5.6's "transparent widget"
 * concern is about the *outer* background; cells and the band keep their own opaque fill on purpose,
 * so their text stays legible at any opacity — see `MonthGlanceWidget`'s KDoc).
 */
public fun applyWidgetBackgroundOpacity(
    color: Color,
    opacityPercent: Int,
): Color = color.copy(alpha = widgetBackgroundAlpha(opacityPercent))

/** Below this opacity a widget's title/date text gets a solid chip behind it (`docs/design-plan.md` §5.6). */
public const val LOW_OPACITY_CHIP_THRESHOLD: Int = 50

/**
 * Whether [opacityPercent] is low enough that a widget's own text block needs the solid chip
 * [LOW_OPACITY_CHIP_THRESHOLD] describes, rather than sitting directly on the translucent background.
 * The one pure decision behind both [io.github.chrisjmendoza.yearal.widget.today.TodayGlanceWidget]'s
 * date-text chip and [io.github.chrisjmendoza.yearal.widget.month.MonthGlanceWidget]'s title/span/header
 * chip (ROADMAP M8 T1, accessibility audit finding #16) -- pulled out to a named function, rather than
 * each widget repeating its own `< LOW_OPACITY_CHIP_THRESHOLD` comparison, so the one decision both
 * widgets share is unit-tested once.
 */
public fun shouldShowLowOpacityChip(opacityPercent: Int): Boolean = opacityPercent < LOW_OPACITY_CHIP_THRESHOLD
