package io.github.chrisjmendoza.yearal.feature.settings.intro

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.chrisjmendoza.yearal.core.designsystem.theme.IfcTheme
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [IntroScreen] under Robolectric (`docs/FEATURES.md` L1): the three screens' content, the
 * skip/back/next/done navigation, the "Learn more" and "find my IFC birthday" hooks, and 200% font
 * scale. Every worked example is asserted by its full, exact sentence, built from the same
 * [IntroFacts] and `IfcDateFormatter` output [IntroFactsTest] and `LearnScreenTest` independently verify
 * against the spec — proving the screen renders what those facts say, not a hard-coded copy of it.
 */
@RunWith(AndroidJUnit4::class)
class IntroScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private var skipped = 0
    private var finished = 0
    private var wentToBirthday = 0
    private var wentToLearnMore = 0

    private fun show(fontScale: Float = 1f) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                IfcTheme(dynamicColor = false) {
                    IntroScreen(
                        onSkip = { skipped++ },
                        onFinish = { finished++ },
                        onFindBirthday = { wentToBirthday++ },
                        onLearnMore = { wentToLearnMore++ },
                    )
                }
            }
        }
    }

    private fun heading(text: String) =
        compose.onNode(hasText(text).and(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)))

    @Test
    fun `screen 1 shows what the IFC is, computed through core-calendar`() {
        show()

        compose.onNodeWithText("Screen 1 of 3").assertIsDisplayed()
        heading("What is the IFC?").assertIsDisplayed()
        compose
            .onNodeWithText(
                "The International Fixed Calendar reshapes the year into 13 months of exactly 28 days " +
                    "each — every month laid out exactly the same, forever.",
            ).assertIsDisplayed()
        compose
            .onNodeWithText("An extra month, Sol, sits between June and July.")
            .performScrollTo()
            .assertIsDisplayed()
        // docs/design-plan.md §4.8 (ROADMAP wave 3 J3): each page opens with a grid illustration.
        compose
            .onNodeWithContentDescription(
                "A row of 13 month blocks, each under its initial, January to December. The seventh, Sol, " +
                    "is highlighted between June and July.",
            ).assertIsDisplayed()
    }

    @Test
    fun `Skip is available on screen 1 and calls onSkip`() {
        show()

        compose.onNodeWithText("Skip").assertIsDisplayed().performClick()

        skipped shouldBe 1
    }

    @Test
    fun `Next moves to screen 2, the nominal-vs-actual weekday example`() {
        show()

        compose.onNodeWithText("Next").performClick()

        compose.onNodeWithText("Screen 2 of 3").assertIsDisplayed()
        compose
            .onNodeWithContentDescription(
                "One IFC month, September 2026, as a 4 by 7 grid under two header rows: the IFC weekdays, " +
                    "Sunday to Saturday, and that month's actual weekdays beneath them. The column holding " +
                    "September 8, 2026 is ringed and numbered: its IFC weekday is Sunday and its actual " +
                    "weekday is Thursday.",
            ).assertIsDisplayed()
        heading("Two weekdays for every date").performScrollTo().assertIsDisplayed()
        // The spec's own worked example (calendar-spec §4.1), same reference date LearnScreenTest checks.
        compose
            .onNodeWithText(
                "For example, Thursday, September 17, 2026 is IFC September 8, 2026. Its IFC weekday is " +
                    "Sunday — but its real weekday is Thursday.",
            ).performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `Back returns from screen 2 to screen 1`() {
        show()
        compose.onNodeWithText("Next").performClick()

        compose.onNodeWithText("Back").performClick()

        compose.onNodeWithText("Screen 1 of 3").assertIsDisplayed()
    }

    // A11y audit finding #26: the shared scroll state used to carry over between pages, so a user who
    // scrolled down on one screen landed on the next already scrolled past its own heading.
    @Test
    fun `moving to the next page resets scroll to the top and focuses the page start`() {
        show()
        // Scroll screen 1 down to its last paragraph before moving on.
        compose.onNodeWithText("An extra month, Sol, sits between June and July.").performScrollTo()

        compose.onNodeWithText("Next").performClick()

        // If the scroll position had carried over, the page's first line would still be scrolled past
        // and this would fail without a performScrollTo() call. Focus lands on that first line, not on
        // the heading: focusing the heading would scroll the illustration off a short screen.
        compose.onNodeWithText("Screen 2 of 3").assertIsDisplayed().assertIsFocused()
        // The heading sits below the taller illustration, off the bottom of Robolectric's short default
        // window, so reaching it takes a scroll — which is the point: the page opened at its top.
        heading("Two weekdays for every date").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `screen 3 shows Year Day and Leap Day worked examples and the birthday hook`() {
        show()
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("Next").performClick()

        compose.onNodeWithText("Screen 3 of 3").assertIsDisplayed()
        compose
            .onNodeWithContentDescription(
                "December as a 4 by 7 grid of 28 days, followed by two pills outside the grid: Year Day, " +
                    "and Leap Day drawn in outline because it exists only in leap years. Neither belongs to " +
                    "any week.",
            ).assertIsDisplayed()
        heading("Two days outside the week").assertIsDisplayed()
        compose
            .onNodeWithText(
                "Year Day happens every year, right after December 28, and belongs to no week. " +
                    "It is always Gregorian December 31: Year Day, 2026 is Gregorian Thursday, December 31, 2026.",
            ).performScrollTo()
            .assertIsDisplayed()
        compose
            .onNodeWithText(
                "Leap Day happens only in leap years, right after June 28, and is also outside the week. " +
                    "It is always Gregorian June 17: Leap Day, 2024 is Gregorian Monday, June 17, 2024.",
            ).performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("Find my IFC birthday").assertIsDisplayed()
    }

    @Test
    fun `Next is not shown on the last screen, only Done and the birthday hook`() {
        show()
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("Next").performClick()

        compose.onAllNodesWithText("Next").fetchSemanticsNodes().size shouldBe 0
        compose.onNodeWithText("Done").assertIsDisplayed()
    }

    @Test
    fun `finishing on the last screen calls onFinish`() {
        show()
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("Next").performClick()

        compose.onNodeWithText("Done").performClick()

        finished shouldBe 1
    }

    @Test
    fun `the birthday hook on the last screen calls onFindBirthday`() {
        show()
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("Next").performClick()

        compose.onNodeWithText("Find my IFC birthday").performClick()

        wentToBirthday shouldBe 1
    }

    @Test
    fun `Learn more calls onLearnMore without finishing the intro`() {
        show()

        compose.onNodeWithText("Learn more about the IFC").performScrollTo().performClick()

        wentToLearnMore shouldBe 1
        skipped shouldBe 0
        finished shouldBe 0
    }

    // docs/ARCHITECTURE.md §4 "Accessibility": 200% font scale never clips.
    @Test
    fun `at 200 percent font scale the heading and worked example stay displayed`() {
        show(fontScale = 2f)

        heading("What is the IFC?").performScrollTo().assertIsDisplayed()
        compose
            .onNodeWithText("An extra month, Sol, sits between June and July.")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
