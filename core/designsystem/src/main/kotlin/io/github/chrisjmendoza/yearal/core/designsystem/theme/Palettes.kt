package io.github.chrisjmendoza.yearal.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import io.github.chrisjmendoza.yearal.core.domain.settings.ColorPalette

/**
 * A curated [ColorPalette]'s light and dark [ColorScheme] pair (`docs/design-plan.md` §5.2). Both
 * members are covered by the app's contrast test, so every role pairing the app draws is guaranteed
 * to meet WCAG AA rather than merely likely to.
 *
 * @property light the scheme used when the palette is active and the theme resolves to light.
 * @property dark the scheme used when the palette is active and the theme resolves to dark.
 */
public class PaletteSchemes(
    public val light: ColorScheme,
    public val dark: ColorScheme,
)

/**
 * The [PaletteSchemes] a [ColorPalette] resolves to (`docs/design-plan.md` §5.2). [ColorPalette.TEAL]
 * returns the existing brand schemes seeded from the launcher icon
 * ([BrandLightColorScheme]/[BrandDarkColorScheme]); the other five are hand-tuned pairs declared
 * alongside this file (`PaletteSol.kt`, `PaletteNight.kt`, `PaletteMoss.kt`, `PaletteRose.kt`,
 * `PaletteInk.kt`).
 */
public fun ColorPalette.colorSchemes(): PaletteSchemes =
    when (this) {
        ColorPalette.TEAL -> PaletteSchemes(BrandLightColorScheme, BrandDarkColorScheme)
        ColorPalette.SOL -> PaletteSchemes(SolLightColorScheme, SolDarkColorScheme)
        ColorPalette.NIGHT -> PaletteSchemes(NightLightColorScheme, NightDarkColorScheme)
        ColorPalette.MOSS -> PaletteSchemes(MossLightColorScheme, MossDarkColorScheme)
        ColorPalette.ROSE -> PaletteSchemes(RoseLightColorScheme, RoseDarkColorScheme)
        ColorPalette.INK -> PaletteSchemes(InkLightColorScheme, InkDarkColorScheme)
    }
