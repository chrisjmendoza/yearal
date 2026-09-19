package io.github.chrisjmendoza.yearal.ui

import io.github.chrisjmendoza.yearal.R
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Test

/**
 * [TopLevelDestination.CONVERT]'s icon: a swap glyph, not the circular-arrow "refresh" icon it
 * replaced (ROADMAP R5, `docs/reviews/2026-09-19-astra-analysis-response.md`). A future accidental
 * revert to `Icons.Filled.Refresh` (or anything else read as "refresh") would still compile — this
 * test is the guard rail that catches it.
 */
class TopLevelDestinationTest {
    @Test
    fun `Convert uses the hand-drawn swap drawable, not a material-icons-core vector`() {
        val icon = TopLevelDestination.CONVERT.icon.shouldBeInstanceOf<TabIcon.Resource>()
        icon.id shouldBe R.drawable.ic_convert
    }

    @Test
    fun `every other top-level tab still uses a material-icons-core vector`() {
        val vectorTabs = TopLevelDestination.entries - TopLevelDestination.CONVERT
        vectorTabs.forEach { destination ->
            destination.icon.shouldBeInstanceOf<TabIcon.Vector>()
        }
    }
}
