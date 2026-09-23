package io.github.chrisjmendoza.yearal.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em

/** The OpenType tabular-figure feature tag (`docs/design-plan.md` §3.1 "Typography"). */
private const val TABULAR_FIGURES = "tnum"

/** 0.1em, the eyebrow caption's letter spacing (design-plan §3.1). */
private val EYEBROW_LETTER_SPACING = 0.1.em

/**
 * The app's Material 3 type scale (`docs/design-plan.md` §3.1 "Typography"), built from the
 * Material defaults on the platform's default font family. The owner chose Roboto over a bundled
 * display face (design-plan §8 decision 3): no new font asset, so this object is the only typography
 * decision this pass makes.
 *
 * - Tabular figures ([TABULAR_FIGURES]) on `titleLarge`, `titleMedium`, `bodyLarge`, `bodyMedium` and
 *   `labelLarge`, so a column of IFC or Gregorian day numbers never shifts width digit to digit — the
 *   month grid, the hero date and list rows all use one of these five styles for a numeral.
 * - `displayMedium` gains [FontWeight.SemiBold] for the Today hero date, heavier than Material's
 *   default `Normal` so the hero reads as the page's one big number.
 * - `labelSmall` gains [EYEBROW_LETTER_SPACING] for the "IFC" / "Gregorian" eyebrow captions the
 *   review asked to make visibly secondary (design-plan §4.1).
 *
 * [IfcTheme] passes this as `MaterialTheme.typography`.
 */
public val YearalTypography: Typography =
    Typography().let { base ->
        base.copy(
            titleLarge = base.titleLarge.withTabularFigures(),
            titleMedium = base.titleMedium.withTabularFigures(),
            bodyLarge = base.bodyLarge.withTabularFigures(),
            bodyMedium = base.bodyMedium.withTabularFigures(),
            labelLarge = base.labelLarge.withTabularFigures(),
            displayMedium = base.displayMedium.copy(fontWeight = FontWeight.SemiBold),
            labelSmall = base.labelSmall.copy(letterSpacing = EYEBROW_LETTER_SPACING),
        )
    }

private fun TextStyle.withTabularFigures(): TextStyle = copy(fontFeatureSettings = TABULAR_FIGURES)
