package io.github.chrisjmendoza.yearal.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.github.chrisjmendoza.yearal.core.domain.settings.ColorPalette
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
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
            Pair("primary/surface", scheme.primary, scheme.surface, TEXT_MIN),
            Pair("secondary/surface", scheme.secondary, scheme.surface, TEXT_MIN),
            Pair("tertiary/surface", scheme.tertiary, scheme.surface, TEXT_MIN),
            Pair("error/surface", scheme.error, scheme.surface, TEXT_MIN),
            Pair("onSurfaceVariant/surface", scheme.onSurfaceVariant, scheme.surface, TEXT_MIN),
            // Material role pair, non-text: 3:1.
            Pair("outline/surface", scheme.outline, scheme.surface, NON_TEXT_MIN),
            // YearalColors pairs, text: 4.5:1.
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
            Pair("onCard/cardContainer", yc.onCard, yc.cardContainer, TEXT_MIN),
            Pair("weekdayActualText/pageBackground", yc.weekdayActualText, yc.pageBackground, TEXT_MIN),
            // YearalColors pairs, non-text (marks on cell fills): 3:1.
            Pair("todayRing/gridCell", yc.todayRing, yc.gridCell, NON_TEXT_MIN),
            Pair("holidayMark/gridCell", yc.holidayMark, yc.gridCell, NON_TEXT_MIN),
            Pair("eventMark/gridCell", yc.eventMark, yc.gridCell, NON_TEXT_MIN),
            Pair("todayRing/gridCellWeekend", yc.todayRing, yc.gridCellWeekend, NON_TEXT_MIN),
            Pair("holidayMark/gridCellMarked", yc.holidayMark, yc.gridCellMarked, NON_TEXT_MIN),
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
        const val LUMINANCE_OFFSET = 0.05
    }
}
