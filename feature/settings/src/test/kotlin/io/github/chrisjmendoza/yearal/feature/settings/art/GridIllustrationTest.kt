package io.github.chrisjmendoza.yearal.feature.settings.art

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.chrisjmendoza.yearal.core.designsystem.theme.IfcTheme
import io.kotest.assertions.throwables.shouldThrow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [GridIllustration] under Robolectric: each variant is described to TalkBack by exactly the caller's
 * [contentDescription] (`docs/design-plan.md` §4.8, ROADMAP wave 3 J3), and
 * [GridIllustrationVariant.NOMINAL_VS_ACTUAL] refuses to render without both weekday labels rather
 * than silently showing a blank caption.
 */
@RunWith(AndroidJUnit4::class)
class GridIllustrationTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `THIRTEEN_MONTHS shows exactly the caller's content description`() {
        compose.setContent {
            IfcTheme(dynamicColor = false) {
                GridIllustration(
                    variant = GridIllustrationVariant.THIRTEEN_MONTHS,
                    contentDescription = "Thirteen months, Sol highlighted",
                )
            }
        }

        compose.onNodeWithContentDescription("Thirteen months, Sol highlighted").assertIsDisplayed()
    }

    @Test
    fun `YEAR_DAY shows exactly the caller's content description`() {
        compose.setContent {
            IfcTheme(dynamicColor = false) {
                GridIllustration(
                    variant = GridIllustrationVariant.YEAR_DAY,
                    contentDescription = "Year Day pill outside the grid",
                )
            }
        }

        compose.onNodeWithContentDescription("Year Day pill outside the grid").assertIsDisplayed()
    }

    @Test
    fun `NOMINAL_VS_ACTUAL shows exactly the caller's content description and its own caption text is hidden`() {
        compose.setContent {
            IfcTheme(dynamicColor = false) {
                GridIllustration(
                    variant = GridIllustrationVariant.NOMINAL_VS_ACTUAL,
                    contentDescription = "Ringed weekday column, Sunday vs Thursday",
                    nominalWeekdayLabel = "Sunday",
                    actualWeekdayLabel = "Thursday",
                )
            }
        }

        compose.onNodeWithContentDescription("Ringed weekday column, Sunday vs Thursday").assertIsDisplayed()
    }

    @Test
    fun `NOMINAL_VS_ACTUAL without both weekday labels throws`() {
        shouldThrow<IllegalArgumentException> {
            compose.setContent {
                IfcTheme(dynamicColor = false) {
                    GridIllustration(
                        variant = GridIllustrationVariant.NOMINAL_VS_ACTUAL,
                        contentDescription = "Missing labels",
                    )
                }
            }
        }
    }
}
