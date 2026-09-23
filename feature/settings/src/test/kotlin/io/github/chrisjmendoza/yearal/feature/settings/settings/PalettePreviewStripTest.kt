package io.github.chrisjmendoza.yearal.feature.settings.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.chrisjmendoza.yearal.core.domain.settings.ColorPalette
import io.github.chrisjmendoza.yearal.core.domain.settings.ColorSource
import io.github.chrisjmendoza.yearal.core.domain.settings.ThemeMode
import io.github.chrisjmendoza.yearal.core.domain.settings.UserSettings
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [PalettePreviewStrip] under Robolectric (`docs/design-plan.md` §4.8, ROADMAP wave 3 J3): its content
 * description names whichever [UserSettings.palette] it is given, for every [UserSettings.themeMode]
 * and [UserSettings.colorSource] combination — the strip must render (and re-theme) on its own nested
 * [io.github.chrisjmendoza.yearal.core.designsystem.theme.IfcTheme] regardless of the settings passed.
 */
@RunWith(AndroidJUnit4::class)
class PalettePreviewStripTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(settings: UserSettings) {
        compose.setContent {
            PalettePreviewStrip(settings = settings)
        }
    }

    @Test
    fun `names the selected palette regardless of theme mode`() {
        show(UserSettings(palette = ColorPalette.TEAL, themeMode = ThemeMode.LIGHT))

        compose.onNodeWithContentDescription("Preview of the Teal palette").assertIsDisplayed()
    }

    @Test
    fun `renders under a forced dark theme mode`() {
        show(UserSettings(palette = ColorPalette.NIGHT, themeMode = ThemeMode.DARK))

        compose.onNodeWithContentDescription("Preview of the Night palette").assertIsDisplayed()
    }

    @Test
    fun `renders under Material You dynamic colour without crashing`() {
        show(UserSettings(palette = ColorPalette.ROSE, colorSource = ColorSource.DYNAMIC))

        // Below API 31 IfcTheme silently falls back to the palette scheme (docs/ARCHITECTURE.md §4
        // "Theme and design tokens"), so the strip still renders and still names the stored palette.
        compose.onNodeWithContentDescription("Preview of the Rose palette").assertIsDisplayed()
    }

    @Test
    fun `renders under pure black`() {
        show(UserSettings(palette = ColorPalette.INK, themeMode = ThemeMode.DARK, pureBlack = true))

        compose.onNodeWithContentDescription("Preview of the Ink palette").assertIsDisplayed()
    }
}
