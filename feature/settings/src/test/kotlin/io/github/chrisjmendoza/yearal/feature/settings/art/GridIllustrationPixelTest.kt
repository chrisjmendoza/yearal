package io.github.chrisjmendoza.yearal.feature.settings.art

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.chrisjmendoza.yearal.core.calendar.IfcDate
import io.github.chrisjmendoza.yearal.core.calendar.IfcMonth
import io.github.chrisjmendoza.yearal.core.designsystem.theme.IfcTheme
import io.github.chrisjmendoza.yearal.core.designsystem.theme.YearalTheme
import io.kotest.matchers.floats.shouldBeGreaterThan
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/**
 * [GridIllustration]'s plain dots and month blocks must be visible against the card they sit on.
 *
 * Owner device report, 2026-09-27: in dark mode the intro's illustrations showed a lone amber bar, a
 * column of four rings and an empty card, because the plain elements were drawn in `gridCell`, the same
 * colour tier as `cardContainer` — the trap the Year overview's mini-grid fell into first (R11). A
 * semantics test cannot see that; only pixels can. The illustration clears its children's semantics
 * (one description for TalkBack), so each test captures the whole card and measures how much of it is
 * drawn in the plain-element colour rather than the card colour.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-xxhdpi")
class GridIllustrationPixelTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the twelve plain month blocks are visible, light`() =
        assertPlainElementsVisible(GridIllustrationVariant.THIRTEEN_MONTHS, darkTheme = false)

    @Test
    fun `the twelve plain month blocks are visible, dark`() =
        assertPlainElementsVisible(GridIllustrationVariant.THIRTEEN_MONTHS, darkTheme = true)

    @Test
    fun `the weekday grid's dots are visible, light`() =
        assertPlainElementsVisible(GridIllustrationVariant.NOMINAL_VS_ACTUAL, darkTheme = false)

    @Test
    fun `the weekday grid's dots are visible, dark`() =
        assertPlainElementsVisible(GridIllustrationVariant.NOMINAL_VS_ACTUAL, darkTheme = true)

    @Test
    fun `the Year Day grid's dots are visible, light`() =
        assertPlainElementsVisible(GridIllustrationVariant.YEAR_DAY, darkTheme = false)

    @Test
    fun `the Year Day grid's dots are visible, dark`() =
        assertPlainElementsVisible(GridIllustrationVariant.YEAR_DAY, darkTheme = true)

    private fun assertPlainElementsVisible(
        variant: GridIllustrationVariant,
        darkTheme: Boolean,
    ) {
        var fill = Color.Unspecified
        var card = Color.Unspecified
        compose.setContent {
            IfcTheme(darkTheme = darkTheme, dynamicColor = false) {
                fill = YearalTheme.colors.miniGridCell
                card = YearalTheme.colors.cardContainer
                GridIllustration(
                    variant = variant,
                    contentDescription = DESCRIPTION,
                    example = IfcDate.Regular(2026, IfcMonth.SEPTEMBER, 8),
                )
            }
        }

        val pixels = compose.onNodeWithContentDescription(DESCRIPTION).captureToImage().toPixelMap()

        // Twelve blocks, or 27–28 dots, cover well over 5% of the card; with the old gridCell fill they
        // matched the card exactly and this share was 0%. "Closer to the fill than to the card" rather
        // than a plain tolerance check, because in the light scheme the two tiers are only a few percent
        // apart per channel (ColorSchemeContrastTest pins that they differ).
        pixels.shareDrawnIn(fill, card) shouldBeGreaterThan MIN_PLAIN_ELEMENT_SHARE
    }

    /** The fraction of pixels near [fill] and strictly closer to it than to [card]. */
    private fun PixelMap.shareDrawnIn(
        fill: Color,
        card: Color,
    ): Float {
        var count = 0
        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = this[x, y]
                if (pixel.isNear(fill) && pixel.distanceTo(fill) < pixel.distanceTo(card)) count++
            }
        }
        return count.toFloat() / (width * height)
    }

    private fun Color.distanceTo(target: Color): Float =
        abs(red - target.red) + abs(green - target.green) + abs(blue - target.blue)

    private fun Color.isNear(
        target: Color,
        tolerance: Float = 0.08f,
    ): Boolean =
        abs(red - target.red) < tolerance &&
            abs(green - target.green) < tolerance &&
            abs(blue - target.blue) < tolerance

    private companion object {
        const val DESCRIPTION = "Illustration"
        const val MIN_PLAIN_ELEMENT_SHARE = 0.05f
    }
}
