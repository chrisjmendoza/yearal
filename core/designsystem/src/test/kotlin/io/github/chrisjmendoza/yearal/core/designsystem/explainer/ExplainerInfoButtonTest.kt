package io.github.chrisjmendoza.yearal.core.designsystem.explainer

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertTouchWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.chrisjmendoza.yearal.core.designsystem.theme.IfcTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [ExplainerInfoButton] under Robolectric (`docs/FEATURES.md` L3): the button meets the 48dp touch
 * target (Q4), opens and dismisses its explanation, labels itself for TalkBack with the caller's title,
 * and survives 200% font scale without losing content (`docs/ARCHITECTURE.md` §4 "Accessibility").
 */
@RunWith(AndroidJUnit4::class)
class ExplainerInfoButtonTest {
    @get:Rule
    val compose = createComposeRule()

    private val title = "Why weekdays differ"
    private val explanation = "The IFC weekday and the real weekday are not the same thing."
    private val buttonDescription = "More information: $title"

    private fun show(fontScale: Float = 1f) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                IfcTheme(dynamicColor = false) {
                    ExplainerInfoButton(title = title, explanation = explanation)
                }
            }
        }
    }

    @Test
    fun `the info button meets the 48dp minimum touch target in both dimensions`() {
        show()

        compose
            .onNodeWithContentDescription(buttonDescription)
            .assertTouchWidthIsEqualTo(48.dp)
            .assertTouchHeightIsEqualTo(48.dp)
    }

    private fun assertExplanationAbsent() {
        compose.onAllNodesWithText(explanation).assertCountEquals(0)
    }

    @Test
    fun `the explanation is not shown until the button is tapped, then it opens`() {
        show()
        assertExplanationAbsent()

        compose.onNodeWithContentDescription(buttonDescription).assertHasClickAction().performClick()

        compose.onNodeWithText(title).assertIsDisplayed()
        compose.onNodeWithText(explanation).assertIsDisplayed()
    }

    @Test
    fun `dismissing closes the explanation, and it can be reopened`() {
        show()
        compose.onNodeWithContentDescription(buttonDescription).performClick()

        compose.onNodeWithText("Got it").assertHasClickAction().performClick()
        assertExplanationAbsent()

        compose.onNodeWithContentDescription(buttonDescription).performClick()
        compose.onNodeWithText(explanation).assertIsDisplayed()
    }

    // docs/ARCHITECTURE.md §4 "Accessibility": 200% font scale never clips.
    @Test
    fun `at 200 percent font scale the explanation still renders and can be dismissed`() {
        show(fontScale = 2f)

        compose.onNodeWithContentDescription(buttonDescription).performClick()

        compose.onNodeWithText(explanation).assertIsDisplayed()
        compose.onNodeWithText("Got it").assertHasClickAction().performClick()
        assertExplanationAbsent()
    }
}
