package io.github.chrisjmendoza.yearal.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.github.chrisjmendoza.yearal.core.domain.settings.ColorPalette
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Test

/**
 * Enforces `docs/design-plan.md` §3.1 "Contrast test": every [ColorScheme] the app can produce — each
 * [ColorPalette]'s light and dark scheme, and each palette's dark scheme with [ColorScheme.pureBlack]
 * applied — meets WCAG 2 contrast (4.5:1 for text pairs, 3:1 for non-text pairs) on every pairing the
 * app actually draws, both the Material role pairs and the derived [YearalColors] pairs from
 * `docs/design-plan.md` §3.1's token table. Robolectric-free: [Color.luminance] and the WCAG formula
 * `(L1 + 0.05) / (L2 + 0.05)` need no Android runtime, so this is deliberately plain JUnit rather than
 * `@RunWith(AndroidJUnit4::class)` like [IfcThemeTest].
 */
class ColorSchemeContrastTest {
    /** One foreground/background pairing the app draws, and the WCAG 2 ratio it must clear. */
    private data class Pair(
        val name: String,
        val fg: Color,
        val bg: Color,
        val min: Double,
    )

    private fun contrastRatio(
        a: Color,
        b: Color,
    ): Double {
        val lighter = maxOf(a.luminance(), b.luminance()).toDouble()
        val darker = minOf(a.luminance(), b.luminance()).toDouble()
        return (lighter + LUMINANCE_OFFSET) / (darker + LUMINANCE_OFFSET)
    }

    // Every pairing named in docs/design-plan.md §3.1: the Material role pairs the screens draw text
    // or the today ring/outline in, plus the derived YearalColors pairs from the token table.
    private fun pairsFor(scheme: ColorScheme): List<Pair> {
        val yc = yearalColorsFrom(scheme)
        return listOf(
            // Material role pairs, text: 4.5:1.
            Pair("onPrimary/primary", scheme.onPrimary, scheme.primary, TEXT_MIN),
            Pair("onPrimaryContainer/primaryContainer", scheme.onPrimaryContainer, scheme.primaryContainer, TEXT_MIN),
            Pair("onSecondary/secondary", scheme.onSecondary, scheme.secondary, TEXT_MIN),
            Pair(
                "onSecondaryContainer/secondaryContainer",
                scheme.onSecondaryContainer,
                scheme.secondaryContainer,
                TEXT_MIN,
            ),
            Pair("onTertiary/tertiary", scheme.onTertiary, scheme.tertiary, TEXT_MIN),
            Pair(
                "onTertiaryContainer/tertiaryContainer",
                scheme.onTertiaryContainer,
                scheme.tertiaryContainer,
                TEXT_MIN,
            ),
            Pair("onError/error", scheme.onError, scheme.error, TEXT_MIN),
            Pair("onErrorContainer/errorContainer", scheme.onErrorContainer, scheme.errorContainer, TEXT_MIN),
            Pair("onSurface/surface", scheme.onSurface, scheme.surface, TEXT_MIN),
            Pair("onBackground/background", scheme.onBackground, scheme.background, TEXT_MIN),
            Pair("onSurfaceVariant/surfaceVariant", scheme.onSurfaceVariant, scheme.surfaceVariant, TEXT_MIN),
            Pair("inverseOnSurface/inverseSurface", scheme.inverseOnSurface, scheme.inverseSurface, TEXT_MIN),
            Pair("onSurface/surfaceContainerLowest", scheme.onSurface, scheme.surfaceContainerLowest, TEXT_MIN),
            Pair("onSurface/surfaceContainerLow", scheme.onSurface, scheme.surfaceContainerLow, TEXT_MIN),
            Pair("onSurface/surfaceContainer", scheme.onSurface, scheme.surfaceContainer, TEXT_MIN),
            Pair("onSurface/surfaceContainerHigh", scheme.onSurface, scheme.surfaceContainerHigh, TEXT_MIN),
            Pair("onSurface/surfaceContainerHighest", scheme.onSurface, scheme.surfaceContainerHighest, TEXT_MIN),
            // onSurfaceVariant against every surfaceContainer* tier and cardContainer (finding #18):
            // DayCell draws the Gregorian corner number in exactly onSurfaceVariant on exactly the
            // gridCell/gridCellMarked fills, which are surfaceContainerLow/surfaceContainerHigh — this
            // was an uncovered pairing even though onSurface's equivalent list (above) was complete.
            Pair(
                "onSurfaceVariant/surfaceContainerLowest",
                scheme.onSurfaceVariant,
                scheme.surfaceContainerLowest,
                TEXT_MIN,
            ),
            Pair(
                "onSurfaceVariant/surfaceContainerLow",
                scheme.onSurfaceVariant,
                scheme.surfaceContainerLow,
                TEXT_MIN,
            ),
            Pair("onSurfaceVariant/surfaceContainer", scheme.onSurfaceVariant, scheme.surfaceContainer, TEXT_MIN),
            Pair(
                "onSurfaceVariant/surfaceContainerHigh",
                scheme.onSurfaceVariant,
                scheme.surfaceContainerHigh,
                TEXT_MIN,
            ),
            Pair(
                "onSurfaceVariant/surfaceContainerHighest",
                scheme.onSurfaceVariant,
                scheme.surfaceContainerHighest,
                TEXT_MIN,
            ),
            Pair("onSurfaceVariant/cardContainer", scheme.onSurfaceVariant, yc.cardContainer, TEXT_MIN),
            Pair("primary/surface", scheme.primary, scheme.surface, TEXT_MIN),
            Pair("secondary/surface", scheme.secondary, scheme.surface, TEXT_MIN),
            Pair("tertiary/surface", scheme.tertiary, scheme.surface, TEXT_MIN),
            Pair("error/surface", scheme.error, scheme.surface, TEXT_MIN),
            Pair("onSurfaceVariant/surface", scheme.onSurfaceVariant, scheme.surface, TEXT_MIN),
            // YearalColors pairs, text: 4.5:1 — exactly the pairs the grid, cards and headers draw
            // text or a legible label over (design-pass fix 6; the previous list missed weekdayActualText over
            // its own header fill and the today badge's text over both cell fills it actually sits on).
            Pair("onHero/heroContainer", yc.onHero, yc.heroContainer, TEXT_MIN),
            Pair(
                "onIntercalaryContainer/intercalaryContainer",
                yc.onIntercalaryContainer,
                yc.intercalaryContainer,
                TEXT_MIN,
            ),
            Pair(
                "onWeekdayNominalContainer/weekdayNominalContainer",
                yc.onWeekdayNominalContainer,
                yc.weekdayNominalContainer,
                TEXT_MIN,
            ),
            Pair(
                "weekdayActualText/weekdayNominalContainer",
                yc.weekdayActualText,
                yc.weekdayNominalContainer,
                TEXT_MIN,
            ),
            Pair("onCard/cardContainer", yc.onCard, yc.cardContainer, TEXT_MIN),
            Pair("weekdayActualText/pageBackground", yc.weekdayActualText, yc.pageBackground, TEXT_MIN),
            Pair("todayText/gridCell", yc.todayText, yc.gridCell, TEXT_MIN),
            Pair("todayText/gridCellMarked", yc.todayText, yc.gridCellMarked, TEXT_MIN),
            // The Year overview's mini grid (fix R11): a day number in onCard on both square fills,
            // today's number in todayText on both.
            Pair("onCard/miniGridCell", yc.onCard, yc.miniGridCell, TEXT_MIN),
            Pair("onCard/miniGridCellMarked", yc.onCard, yc.miniGridCellMarked, TEXT_MIN),
            Pair("todayText/miniGridCell", yc.todayText, yc.miniGridCell, TEXT_MIN),
            Pair("todayText/miniGridCellMarked", yc.todayText, yc.miniGridCellMarked, TEXT_MIN),
            // YearalColors pairs, non-text (marks and rings on the cell fills they are actually drawn
            // on): 3:1. todayRing/gridCellWeekend and eventMark/gridCell are gone — the grid never
            // draws those combinations (the today ring and the event dot both sit on gridCellMarked
            // once a cell has a mark, not on the plain or weekend fill).
            Pair("outline/surface", scheme.outline, scheme.surface, NON_TEXT_MIN),
            Pair("todayRing/gridCell", yc.todayRing, yc.gridCell, NON_TEXT_MIN),
            Pair("todayRing/gridCellMarked", yc.todayRing, yc.gridCellMarked, NON_TEXT_MIN),
            Pair("holidayMark/gridCell", yc.holidayMark, yc.gridCell, NON_TEXT_MIN),
            Pair("holidayMark/gridCellMarked", yc.holidayMark, yc.gridCellMarked, NON_TEXT_MIN),
            Pair("eventMark/gridCellMarked", yc.eventMark, yc.gridCellMarked, NON_TEXT_MIN),
            // Mini grid: the today ring wraps a plain or a marked square; the marks only ever sit on
            // a marked square.
            Pair("todayRing/miniGridCell", yc.todayRing, yc.miniGridCell, NON_TEXT_MIN),
            Pair("todayRing/miniGridCellMarked", yc.todayRing, yc.miniGridCellMarked, NON_TEXT_MIN),
            Pair("holidayMark/miniGridCellMarked", yc.holidayMark, yc.miniGridCellMarked, NON_TEXT_MIN),
            Pair("eventMark/miniGridCellMarked", yc.eventMark, yc.miniGridCellMarked, NON_TEXT_MIN),
            Pair("intercalary/intercalaryContainer", yc.intercalary, yc.intercalaryContainer, NON_TEXT_MIN),
            Pair("intercalary/pageBackground", yc.intercalary, yc.pageBackground, NON_TEXT_MIN),
        )
    }

    @Test
    fun `every palette's light, dark and pure black scheme clears its minimum contrast on every pair`() {
        ColorPalette.entries.forEach { palette ->
            val schemes = palette.colorSchemes()
            listOf(
                "light" to schemes.light,
                "dark" to schemes.dark,
                "dark pureBlack" to schemes.dark.pureBlack(),
            ).forEach { (mode, scheme) ->
                pairsFor(scheme).forEach { pair ->
                    withClue("$palette $mode ${pair.name}") {
                        contrastRatio(pair.fg, pair.bg) shouldBeGreaterThanOrEqual pair.min
                    }
                }
            }
        }
    }

    // Fix R11: the Year overview's mini-grid squares were drawn in `gridCell`, which is the same
    // Material role as the card they sit on, so all 28 squares vanished and only the marks were left.
    // WCAG has nothing to say about two adjacent surfaces, so this pins a floor of its own: the plain
    // square must be at least two Material tonal tiers away from the card (in practice ≥ ~1.09:1 in the
    // light schemes, where the tiers are closest), and the marked square must step again from the plain
    // one, in every palette, mode and pure black.

    @Test
    fun `mini-grid squares are visibly distinct from the card and from each other in every scheme`() {
        ColorPalette.entries.forEach { palette ->
            val schemes = palette.colorSchemes()
            listOf(
                "light" to schemes.light,
                "dark" to schemes.dark,
                "dark pureBlack" to schemes.dark.pureBlack(),
            ).forEach { (mode, scheme) ->
                val yc = yearalColorsFrom(scheme)
                withClue("$palette $mode miniGridCell/cardContainer") {
                    contrastRatio(yc.miniGridCell, yc.cardContainer) shouldBeGreaterThanOrEqual ADJACENT_SURFACE_MIN
                }
                withClue("$palette $mode miniGridCellMarked/miniGridCell") {
                    yc.miniGridCellMarked shouldNotBe yc.miniGridCell
                    contrastRatio(yc.miniGridCellMarked, yc.miniGridCell) shouldBeGreaterThanOrEqual TIER_STEP_MIN
                }
            }
        }
    }

    @Test
    fun `the six palettes have six distinct light primary colours`() {
        val primaries = ColorPalette.entries.map { it.colorSchemes().light.primary }

        primaries.toSet() shouldHaveSize ColorPalette.entries.size
    }

    @Test
    fun `pureBlack overrides surface to true black and leaves onSurface unchanged`() {
        ColorPalette.entries.forEach { palette ->
            val dark = palette.colorSchemes().dark
            val pureBlack = dark.pureBlack()

            withClue("$palette") {
                pureBlack.surface shouldBe Color(0xFF000000)
                pureBlack.onSurface shouldBe dark.onSurface
            }
        }
    }

    private companion object {
        const val TEXT_MIN = 4.5
        const val NON_TEXT_MIN = 3.0

        /** Two Material tonal tiers apart (see the R11 note above); one tier is ~1.03:1 in light. */
        const val ADJACENT_SURFACE_MIN = 1.08

        /** One tonal tier apart: only has to be a real, non-identical step. */
        const val TIER_STEP_MIN = 1.02
        const val LUMINANCE_OFFSET = 0.05
    }
}
