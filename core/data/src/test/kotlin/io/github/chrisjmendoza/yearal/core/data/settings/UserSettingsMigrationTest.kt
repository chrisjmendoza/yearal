package io.github.chrisjmendoza.yearal.core.data.settings

import io.github.chrisjmendoza.yearal.core.domain.settings.ColorPalette
import io.github.chrisjmendoza.yearal.core.domain.settings.ColorSource
import io.github.chrisjmendoza.yearal.core.domain.settings.ThemeMode
import io.github.chrisjmendoza.yearal.core.domain.settings.UserSettings
import io.github.chrisjmendoza.yearal.core.domain.settings.WeekdayDisplay
import io.github.chrisjmendoza.yearal.core.domain.settings.WidgetTheme
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Enforces the one-time settings migration `docs/design-plan.md` §5.1 calls for: the old `dynamicColor`
 * boolean key is replaced by [ColorSource] (default [ColorSource.BRAND]) and dropped from the file on
 * the next write, so a pre-design-pass install lands on the brand palette exactly once rather than
 * staying on the wallpaper scheme it never chose (`docs/design-plan.md` §8 decision 1). Also covers the
 * other fields the design pass adds to [UserSettings] — [UserSettings.palette], [UserSettings.pureBlack],
 * the per-widget [WidgetTheme] choices and [UserSettings.widgetBackgroundOpacity] — using the same
 * serializer entry points as `UserSettingsSerializerTest`; corruption and empty-stream behaviour are
 * already covered there and are not repeated here.
 */
class UserSettingsMigrationTest {
    private suspend fun write(settings: UserSettings): ByteArray =
        ByteArrayOutputStream().also { UserSettingsSerializer.writeTo(settings, it) }.toByteArray()

    private suspend fun read(text: String): UserSettings =
        UserSettingsSerializer.readFrom(ByteArrayInputStream(text.encodeToByteArray()))

    @Test
    fun `a pre-design-pass file with dynamicColor true lands on the brand palette`() =
        runTest {
            val preDesignPassDocument =
                """{"weekdayDisplay":"ACTUAL","themeMode":"DARK","dynamicColor":true,""" +
                    """"enabledHolidaySets":["ifc"],"hasSeenIntro":true}"""

            val settings = read(preDesignPassDocument)

            settings shouldBe
                UserSettings(
                    weekdayDisplay = WeekdayDisplay.ACTUAL,
                    themeMode = ThemeMode.DARK,
                    colorSource = ColorSource.BRAND,
                    palette = ColorPalette.TEAL,
                    pureBlack = false,
                    enabledHolidaySets = setOf("ifc"),
                    hasSeenIntro = true,
                    todayWidgetTheme = WidgetTheme.FOLLOW_APP,
                    monthWidgetTheme = WidgetTheme.FOLLOW_APP,
                    widgetBackgroundOpacity = 100,
                )
        }

    @Test
    fun `a pre-design-pass file with dynamicColor false also lands on the brand palette`() =
        runTest {
            val preDesignPassDocument =
                """{"weekdayDisplay":"ACTUAL","themeMode":"DARK","dynamicColor":false,""" +
                    """"enabledHolidaySets":["ifc"],"hasSeenIntro":true}"""

            val settings = read(preDesignPassDocument)

            settings shouldBe
                UserSettings(
                    weekdayDisplay = WeekdayDisplay.ACTUAL,
                    themeMode = ThemeMode.DARK,
                    colorSource = ColorSource.BRAND,
                    palette = ColorPalette.TEAL,
                    pureBlack = false,
                    enabledHolidaySets = setOf("ifc"),
                    hasSeenIntro = true,
                    todayWidgetTheme = WidgetTheme.FOLLOW_APP,
                    monthWidgetTheme = WidgetTheme.FOLLOW_APP,
                    widgetBackgroundOpacity = 100,
                )
        }

    @Test
    fun `a file that already carries colorSource reads that value instead of migrating`() =
        runTest {
            read("""{"colorSource":"DYNAMIC"}""").colorSource shouldBe ColorSource.DYNAMIC
        }

    @Test
    fun `widgetBackgroundOpacity is coerced into 0 to 100 on read`() =
        runTest {
            read("""{"widgetBackgroundOpacity":250}""").widgetBackgroundOpacity shouldBe 100
            read("""{"widgetBackgroundOpacity":-5}""").widgetBackgroundOpacity shouldBe 0
        }

    @Test
    fun `a non-default appearance round-trips exactly and drops the dynamicColor key`() =
        runTest {
            val settings =
                UserSettings(
                    colorSource = ColorSource.DYNAMIC,
                    palette = ColorPalette.ROSE,
                    pureBlack = true,
                    todayWidgetTheme = WidgetTheme.DARK,
                    monthWidgetTheme = WidgetTheme.LIGHT,
                    widgetBackgroundOpacity = 40,
                )

            val bytes = write(settings)

            UserSettingsSerializer.readFrom(ByteArrayInputStream(bytes)) shouldBe settings
            bytes.decodeToString() shouldNotContain "\"dynamicColor\""
        }
}
