package io.github.chrisjmendoza.yearal.core.data.settings

import androidx.datastore.core.CorruptionException
import io.github.chrisjmendoza.yearal.core.domain.settings.ColorPalette
import io.github.chrisjmendoza.yearal.core.domain.settings.ColorSource
import io.github.chrisjmendoza.yearal.core.domain.settings.ThemeMode
import io.github.chrisjmendoza.yearal.core.domain.settings.UserSettings
import io.github.chrisjmendoza.yearal.core.domain.settings.WeekdayDisplay
import io.github.chrisjmendoza.yearal.core.domain.settings.WidgetTheme
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Plain-JVM tests for [UserSettingsSerializer]: round trips, the compatibility guarantees in its KDoc,
 * and that every kind of bad input surfaces as [CorruptionException] and nothing else
 * (`docs/ARCHITECTURE.md` §6, ":core:data — DataStore serializer round trip and corruption fallback").
 */
class UserSettingsSerializerTest {
    private val nonDefault =
        UserSettings(
            weekdayDisplay = WeekdayDisplay.ACTUAL,
            themeMode = ThemeMode.DARK,
            colorSource = ColorSource.DYNAMIC,
            palette = ColorPalette.SOL,
            pureBlack = true,
            enabledHolidaySets = setOf("ifc", "us", "gb"),
            hasSeenIntro = true,
            todayWidgetTheme = WidgetTheme.DARK,
            monthWidgetTheme = WidgetTheme.LIGHT,
            widgetBackgroundOpacity = 50,
        )

    private suspend fun write(settings: UserSettings): ByteArray =
        ByteArrayOutputStream().also { UserSettingsSerializer.writeTo(settings, it) }.toByteArray()

    private suspend fun read(text: String): UserSettings =
        UserSettingsSerializer.readFrom(ByteArrayInputStream(text.encodeToByteArray()))

    @Test
    fun `default value is the fresh-install settings`() {
        UserSettingsSerializer.defaultValue shouldBe UserSettings.DEFAULT
    }

    @Test
    fun `a non-default value round-trips exactly`() =
        runTest {
            val bytes = write(nonDefault)
            UserSettingsSerializer.readFrom(ByteArrayInputStream(bytes)) shouldBe nonDefault
        }

    @Test
    fun `the defaults round-trip and every field is written explicitly`() =
        runTest {
            val bytes = write(UserSettings.DEFAULT)
            UserSettingsSerializer.readFrom(ByteArrayInputStream(bytes)) shouldBe UserSettings.DEFAULT
            val text = bytes.decodeToString()
            listOf(
                "weekdayDisplay",
                "themeMode",
                "colorSource",
                "palette",
                "pureBlack",
                "enabledHolidaySets",
                "hasSeenIntro",
                "todayWidgetTheme",
                "monthWidgetTheme",
                "widgetBackgroundOpacity",
            ).forEach {
                text shouldContain
                    "\"$it\""
            }
        }

    @Test
    fun `holiday set order is preserved through the file`() =
        runTest {
            val ordered = UserSettings(enabledHolidaySets = setOf("zz", "aa", "mm"))
            UserSettingsSerializer
                .readFrom(
                    ByteArrayInputStream(write(ordered)),
                ).enabledHolidaySets
                .toList() shouldContainExactly
                listOf("zz", "aa", "mm")
        }

    @Test
    fun `forward compatibility - an unknown key from a newer version is ignored`() =
        runTest {
            val text =
                """{"weekdayDisplay":"NOMINAL","themeMode":"LIGHT","dynamicColor":true,""" +
                    """"enabledHolidaySets":["ifc"],"futureField":{"nested":1}}"""
            read(text) shouldBe
                UserSettings(
                    weekdayDisplay = WeekdayDisplay.NOMINAL,
                    themeMode = ThemeMode.LIGHT,
                    enabledHolidaySets = setOf("ifc"),
                )
        }

    @Test
    fun `backward compatibility - a missing field takes its default`() =
        runTest {
            read("""{"themeMode":"DARK"}""") shouldBe UserSettings.DEFAULT.copy(themeMode = ThemeMode.DARK)
            read("{}") shouldBe UserSettings.DEFAULT
        }

    @Test
    fun `migration - a file written before the intro flag existed reads without crashing and as not seen`() =
        runTest {
            // The exact document shape UserSettingsSerializer wrote before `hasSeenIntro` was added:
            // every field that already existed then, and nothing else.
            val preIntroDocument =
                """{"weekdayDisplay":"BOTH","themeMode":"SYSTEM","dynamicColor":true,""" +
                    """"enabledHolidaySets":["ifc","us"]}"""

            val settings = read(preIntroDocument)

            settings shouldBe UserSettings.DEFAULT
            settings.hasSeenIntro shouldBe false
        }

    @Test
    fun `malformed JSON is reported as corruption`() =
        runTest {
            shouldThrow<CorruptionException> { read("{not json") }
        }

    // Design-pass fix 7: an unknown enum value — a downgrade after a newer version wrote a constant this version
    // does not know, or a hand-edited file — used to throw SerializationException, which readFrom could
    // not tell apart from real corruption, so ReplaceFileCorruptionHandler reset every other setting
    // along with it. coerceInputValues now falls back to that one field's default and keeps the rest of
    // the document intact.

    @Test
    fun `an unknown enum constant falls back to that field's default instead of corrupting the file`() =
        runTest {
            read("""{"themeMode":"SEPIA"}""") shouldBe UserSettings.DEFAULT.copy(themeMode = ThemeMode.SYSTEM)
        }

    @Test
    fun `a downgrade - an unknown palette reads as the default palette with every other field intact`() =
        runTest {
            val text =
                """{"weekdayDisplay":"ACTUAL","palette":"NEON","pureBlack":true,""" +
                    """"enabledHolidaySets":["ifc","us"],"hasSeenIntro":true}"""

            val settings = read(text)

            settings.palette shouldBe ColorPalette.TEAL
            settings shouldBe
                UserSettings.DEFAULT.copy(
                    weekdayDisplay = WeekdayDisplay.ACTUAL,
                    pureBlack = true,
                    enabledHolidaySets = setOf("ifc", "us"),
                    hasSeenIntro = true,
                )
        }

    @Test
    fun `a wrongly typed field is reported as corruption`() =
        runTest {
            shouldThrow<CorruptionException> { read("""{"pureBlack":"yes"}""") }
            shouldThrow<CorruptionException> { read("""{"enabledHolidaySets":"ifc"}""") }
        }

    @Test
    fun `widgetBackgroundOpacity outside 0 to 100 is coerced into range, not corruption`() =
        runTest {
            read("""{"widgetBackgroundOpacity":-20}""").widgetBackgroundOpacity shouldBe 0
            read("""{"widgetBackgroundOpacity":150}""").widgetBackgroundOpacity shouldBe 100
            read("""{"widgetBackgroundOpacity":40}""").widgetBackgroundOpacity shouldBe 40
        }

    @Test
    fun `an empty file is reported as corruption`() =
        runTest {
            shouldThrow<CorruptionException> { read("") }
            shouldThrow<CorruptionException> { read("   \n") }
        }

    @Test
    fun `binary garbage is reported as corruption`() =
        runTest {
            val garbage = ByteArray(64) { (it * 37 + 11).toByte() }
            shouldThrow<CorruptionException> { UserSettingsSerializer.readFrom(ByteArrayInputStream(garbage)) }
        }
}
