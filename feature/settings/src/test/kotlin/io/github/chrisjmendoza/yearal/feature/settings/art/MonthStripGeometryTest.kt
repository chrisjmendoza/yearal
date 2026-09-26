package io.github.chrisjmendoza.yearal.feature.settings.art

import io.github.chrisjmendoza.yearal.core.calendar.IfcMonth
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Test

/**
 * [monthStripCornerRadius] is plain Kotlin (no [androidx.compose.ui.graphics.drawscope.Canvas] or
 * Robolectric needed): the pure geometry [MonthStrip] draws from. A11y audit finding #17 — the class
 * KDoc on [GridIllustration] promises every highlighted element "differ[s] in shape from the rest", but
 * `MonthStrip`'s Sol block used to draw with the exact same [CornerRadius][androidx.compose.ui.geometry
 * .CornerRadius] as every other block, differing only by colour. This proves Sol's corner radius (a full
 * capsule/pill, half its own height) is now distinct from a non-Sol block's (the small, fixed
 * [MonthBlockCorner]) for every block height a real layout could produce.
 */
class MonthStripGeometryTest {
    private val solIndex = IfcMonth.SOL.number - 1
    private val nonSolIndex = 0
    private val defaultCornerRadiusPx = 12f

    @Test
    fun `Sol's corner radius differs from a non-Sol block's at a typical block height`() {
        val blockHeightPx = 120f

        val solRadius = monthStripCornerRadius(solIndex, blockHeightPx, defaultCornerRadiusPx)
        val otherRadius = monthStripCornerRadius(nonSolIndex, blockHeightPx, defaultCornerRadiusPx)

        solRadius shouldNotBe otherRadius
    }

    @Test
    fun `Sol's corner radius is half its own block height, a full capsule`() {
        val blockHeightPx = 84f

        val solRadius = monthStripCornerRadius(solIndex, blockHeightPx, defaultCornerRadiusPx)

        solRadius.x shouldBe blockHeightPx / 2f
        solRadius.y shouldBe blockHeightPx / 2f
    }

    @Test
    fun `every non-Sol block keeps the fixed default corner radius regardless of height`() {
        val shortBlock = monthStripCornerRadius(nonSolIndex, blockHeightPx = 40f, defaultCornerRadiusPx)
        val tallBlock = monthStripCornerRadius(nonSolIndex, blockHeightPx = 200f, defaultCornerRadiusPx)

        shortBlock.x shouldBe defaultCornerRadiusPx
        tallBlock.x shouldBe defaultCornerRadiusPx
        shortBlock shouldBe tallBlock
    }

    @Test
    fun `Sol's radius still differs from the default even when the strip is unusually short`() {
        val blockHeightPx = 30f

        val solRadius = monthStripCornerRadius(solIndex, blockHeightPx, defaultCornerRadiusPx)
        val otherRadius = monthStripCornerRadius(nonSolIndex, blockHeightPx, defaultCornerRadiusPx)

        solRadius shouldNotBe otherRadius
    }
}
