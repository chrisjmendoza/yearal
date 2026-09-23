package io.github.chrisjmendoza.yearal.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import io.github.chrisjmendoza.yearal.core.domain.settings.ColorPalette

/**
 * The app's Material 3 theme (`docs/design-plan.md` §3.1, §5.1–§5.3): dynamic (wallpaper) colour on
 * API 31 and later when [dynamicColor] is on, otherwise [palette]'s curated
 * [ColorPalette.colorSchemes] pair — [ColorPalette.TEAL] resolves to the launcher-icon schemes,
 * [BrandLightColorScheme] and [BrandDarkColorScheme] (docs/ROADMAP.md decision #10). Below API 31
 * [dynamicColor] is ignored and a palette scheme is always used. When [darkTheme] and [pureBlack] are
 * both true, the resolved dark scheme is further transformed by [ColorScheme.pureBlack] (design-plan
 * §5.3, the AMOLED option) — [pureBlack] has no effect in light mode.
 *
 * Besides `MaterialTheme.colorScheme`, this also provides [YearalShapes] as `MaterialTheme.shapes`,
 * [YearalTypography] as `MaterialTheme.typography`, and the derived [YearalColors] token set through
 * [LocalYearalColors] (read via [YearalTheme]).
 *
 * **Trap:** [dynamicColor] now defaults to `false` (design-plan §5.1/§8.1 decision 1) — the *brand*
 * palette, not the wallpaper, is the app's default look. This reverses the previous default; callers
 * that relied on it showing dynamic colour out of the box must pass `dynamicColor = true` explicitly.
 *
 * @param darkTheme whether to use the dark scheme; defaults to the system setting.
 * @param dynamicColor whether to derive the scheme from the wallpaper where the platform supports it
 * (the `ColorSource.DYNAMIC` user setting). Previews and screenshot tests pass `false` so goldens are
 * deterministic and show a palette scheme.
 * @param palette which curated scheme to use when [dynamicColor] is off (or unsupported); ignored
 * when dynamic colour is actually applied.
 * @param pureBlack whether to force pure black surfaces in dark mode (the AMOLED option); ignored in
 * light mode.
 */
@Composable
fun IfcTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    palette: ColorPalette = ColorPalette.TEAL,
    pureBlack: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> {
                palette.colorSchemes().dark
            }

            else -> {
                palette.colorSchemes().light
            }
        }
    val resolvedScheme = if (darkTheme && pureBlack) colorScheme.pureBlack() else colorScheme
    CompositionLocalProvider(LocalYearalColors provides yearalColorsFrom(resolvedScheme)) {
        MaterialTheme(
            colorScheme = resolvedScheme,
            shapes = YearalShapes,
            typography = YearalTypography,
            content = content,
        )
    }
}
