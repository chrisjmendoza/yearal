package io.github.chrisjmendoza.yearal

import io.github.chrisjmendoza.yearal.core.domain.settings.ThemeMode
import io.kotest.matchers.shouldBe
import org.junit.Test

/**
 * [resolveDarkTheme] is the plain function [MainActivity.onCreate] calls to decide whether the app
 * shows its dark scheme, and the same value drives which [android.graphics.Color]-free `SystemBarStyle`
 * the system bars get (`docs/design-plan.md` §3.2, finding D6: the bars must follow the app's own
 * [ThemeMode], not the system's). It needs no Compose composition or Robolectric — [ThemeMode] and
 * [Boolean] are both plain values — so this is a plain JUnit test, the same pattern
 * `ui/TopLevelDestinationTest.kt` uses for another pure mapping.
 */
class ThemeModeResolutionTest {
    @Test
    fun `SYSTEM follows whatever the system reports`() {
        resolveDarkTheme(ThemeMode.SYSTEM, systemInDarkTheme = true) shouldBe true
        resolveDarkTheme(ThemeMode.SYSTEM, systemInDarkTheme = false) shouldBe false
    }

    @Test
    fun `LIGHT is always false, regardless of the system`() {
        resolveDarkTheme(ThemeMode.LIGHT, systemInDarkTheme = true) shouldBe false
        resolveDarkTheme(ThemeMode.LIGHT, systemInDarkTheme = false) shouldBe false
    }

    @Test
    fun `DARK is always true, regardless of the system`() {
        resolveDarkTheme(ThemeMode.DARK, systemInDarkTheme = true) shouldBe true
        resolveDarkTheme(ThemeMode.DARK, systemInDarkTheme = false) shouldBe true
    }
}
