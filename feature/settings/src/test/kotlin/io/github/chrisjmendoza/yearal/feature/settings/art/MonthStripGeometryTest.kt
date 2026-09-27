package io.github.chrisjmendoza.yearal.feature.settings.art

import io.github.chrisjmendoza.yearal.core.calendar.IfcMonth
import io.github.chrisjmendoza.yearal.core.designsystem.theme.PillShape
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Test

/**
 * [monthBlockShape], the pure shape rule behind the intro's month strip (no Robolectric needed).
 * A11y audit finding #17: the highlighted Sol block must differ from its neighbours in shape, not only
 * colour, so a colour-blind or greyscale-display user can still tell it apart (design-plan §2).
 */
class MonthStripGeometryTest {
    private val solIndex = IfcMonth.SOL.number - 1

    @Test
    fun `Sol's block is the pill shape`() {
        monthBlockShape(solIndex) shouldBe PillShape
    }

    @Test
    fun `every other month keeps the squared-off bar, so Sol differs in shape from all twelve`() {
        val sol = monthBlockShape(solIndex)
        IfcMonth.entries.indices.filter { it != solIndex }.forEach { index ->
            monthBlockShape(index) shouldNotBe sol
        }
    }

    @Test
    fun `the twelve namesake months share one shape`() {
        val shapes =
            IfcMonth.entries.indices
                .filter { it != solIndex }
                .map { monthBlockShape(it) }
                .toSet()
        shapes.size shouldBe 1
    }
}
