package io.github.chrisjmendoza.yearal.widget.theme

import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.glance.color.DayNightColorProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.chrisjmendoza.yearal.core.designsystem.theme.BrandDarkColorScheme
import io.github.chrisjmendoza.yearal.core.designsystem.theme.BrandLightColorScheme
import io.github.chrisjmendoza.yearal.core.designsystem.theme.pureBlack
import io.github.chrisjmendoza.yearal.core.domain.settings.ColorPalette
import io.github.chrisjmendoza.yearal.core.domain.settings.ColorSource
import io.github.chrisjmendoza.yearal.core.domain.settings.ThemeMode
import io.github.chrisjmendoza.yearal.core.domain.settings.UserSettings
import io.github.chrisjmendoza.yearal.core.domain.settings.WidgetTheme
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [resolveWidgetDark], [resolveWidgetColors] and the opacity helpers are the pure logic behind
 * `docs/design-plan.md` §4.9/§5.6 "widgets follow the app's appearance" -- oracle tests written from
 * that doc, not from [io.github.chrisjmendoza.yearal.widget.today.TodayGlanceWidget]'s or
 * [io.github.chrisjmendoza.yearal.widget.month.MonthGlanceWidget]'s call sites. Every
 * [androidx.glance.unit.ColorProvider] [ColorProviders(light, dark)][androidx.glance.material3.ColorProviders]
 * builds is a [DayNightColorProvider] with its `day`/`night` fields directly readable, so the day/night
 * pair is asserted without resolving through a `Context` -- but building one at all still calls into
 * `android.graphics.Color` (Glance's own `widgetBackground` derivation), which is only a real
 * implementation under Robolectric, not the plain unit-test stub jar.
 */
@RunWith(AndroidJUnit4::class)
class WidgetThemingTest {
    /** The [DayNightColorProvider.day] member of `colors.background`, whichever scheme it came from. */
    private fun day(colors: androidx.glance.color.ColorProviders): Color =
        (colors.background as DayNightColorProvider).day

    /** The [DayNightColorProvider.night] member of `colors.background`. */
    private fun night(colors: androidx.glance.color.ColorProviders): Color =
        (colors.background as DayNightColorProvider).night

    // region resolveWidgetDark

    @Test
    fun `FOLLOW_APP plus SYSTEM forces neither -- Glance follows the system`() {
        resolveWidgetDark(ThemeMode.SYSTEM, WidgetTheme.FOLLOW_APP).shouldBeNull()
    }

    @Test
    fun `FOLLOW_APP plus DARK forces dark`() {
        resolveWidgetDark(ThemeMode.DARK, WidgetTheme.FOLLOW_APP) shouldBe true
    }

    @Test
    fun `FOLLOW_APP plus LIGHT forces light`() {
        resolveWidgetDark(ThemeMode.LIGHT, WidgetTheme.FOLLOW_APP) shouldBe false
    }

    @Test
    fun `a widget-level LIGHT override forces light regardless of the app's theme mode`() {
        resolveWidgetDark(ThemeMode.DARK, WidgetTheme.LIGHT) shouldBe false
        resolveWidgetDark(ThemeMode.SYSTEM, WidgetTheme.LIGHT) shouldBe false
    }

    @Test
    fun `a widget-level DARK override forces dark regardless of the app's theme mode`() {
        resolveWidgetDark(ThemeMode.LIGHT, WidgetTheme.DARK) shouldBe true
        resolveWidgetDark(ThemeMode.SYSTEM, WidgetTheme.DARK) shouldBe true
    }

    // endregion

    // region resolveWidgetColors

    @Test
    fun `DYNAMIC on API 31 and later defers to GlanceTheme colors`() {
        val settings = UserSettings.DEFAULT.copy(colorSource = ColorSource.DYNAMIC)

        resolveWidgetColors(settings, WidgetTheme.FOLLOW_APP, Build.VERSION_CODES.S).shouldBeNull()
    }

    @Test
    fun `DYNAMIC below API 31 behaves as BRAND, matching ColorSource's own contract`() {
        val settings = UserSettings.DEFAULT.copy(colorSource = ColorSource.DYNAMIC, palette = ColorPalette.TEAL)

        val colors = resolveWidgetColors(settings, WidgetTheme.FOLLOW_APP, Build.VERSION_CODES.R)

        colors.shouldNotBeNull()
        day(colors) shouldBe BrandLightColorScheme.background
        night(colors) shouldBe BrandDarkColorScheme.background
    }

    @Test
    fun `BRAND with FOLLOW_APP and SYSTEM leaves the light scheme by day and dark by night`() {
        val settings = UserSettings.DEFAULT.copy(colorSource = ColorSource.BRAND, themeMode = ThemeMode.SYSTEM)

        val colors = resolveWidgetColors(settings, WidgetTheme.FOLLOW_APP, Build.VERSION_CODES.S)!!

        day(colors) shouldBe BrandLightColorScheme.background
        night(colors) shouldBe BrandDarkColorScheme.background
    }

    @Test
    fun `a LIGHT override forces the light scheme in both slots`() {
        val settings = UserSettings.DEFAULT.copy(colorSource = ColorSource.BRAND, themeMode = ThemeMode.DARK)

        val colors = resolveWidgetColors(settings, WidgetTheme.LIGHT, Build.VERSION_CODES.S)!!

        day(colors) shouldBe BrandLightColorScheme.background
        night(colors) shouldBe BrandLightColorScheme.background
    }

    @Test
    fun `a DARK override forces the dark scheme in both slots`() {
        val settings = UserSettings.DEFAULT.copy(colorSource = ColorSource.BRAND, themeMode = ThemeMode.LIGHT)

        val colors = resolveWidgetColors(settings, WidgetTheme.DARK, Build.VERSION_CODES.S)!!

        day(colors) shouldBe BrandDarkColorScheme.background
        night(colors) shouldBe BrandDarkColorScheme.background
    }

    @Test
    fun `pure black is applied only to the dark scheme, never the light one`() {
        val settings =
            UserSettings.DEFAULT.copy(
                colorSource = ColorSource.BRAND,
                themeMode = ThemeMode.SYSTEM,
                pureBlack = true,
            )

        val colors = resolveWidgetColors(settings, WidgetTheme.FOLLOW_APP, Build.VERSION_CODES.S)!!

        day(colors) shouldBe BrandLightColorScheme.background
        night(colors) shouldBe BrandDarkColorScheme.pureBlack().background
        night(colors) shouldBe Color.Black
    }

    @Test
    fun `pure black is ignored while a light scheme is forced`() {
        val settings = UserSettings.DEFAULT.copy(colorSource = ColorSource.BRAND, pureBlack = true)

        val colors = resolveWidgetColors(settings, WidgetTheme.LIGHT, Build.VERSION_CODES.S)!!

        day(colors) shouldBe BrandLightColorScheme.background
        night(colors) shouldBe BrandLightColorScheme.background
    }

    // endregion

    // region opacity

    @Test
    fun `opacity 100 is fully opaque`() {
        widgetBackgroundAlpha(100) shouldBe 1f
    }

    @Test
    fun `opacity 0 is fully transparent`() {
        widgetBackgroundAlpha(0) shouldBe 0f
    }

    @Test
    fun `opacity 50 is half alpha`() {
        widgetBackgroundAlpha(50) shouldBe 0.5f
    }

    @Test
    fun `an out-of-range opacity is coerced into 0 to 100`() {
        widgetBackgroundAlpha(150) shouldBe 1f
        widgetBackgroundAlpha(-10) shouldBe 0f
    }

    @Test
    fun `applying opacity keeps the colour's channels and only changes alpha`() {
        val opaque = Color(red = 0.2f, green = 0.4f, blue = 0.6f, alpha = 1f)

        val translucent = applyWidgetBackgroundOpacity(opaque, 40)

        translucent.red shouldBe opaque.red
        translucent.green shouldBe opaque.green
        translucent.blue shouldBe opaque.blue
        translucent.alpha shouldBe 0.4f
    }

    // endregion
}
