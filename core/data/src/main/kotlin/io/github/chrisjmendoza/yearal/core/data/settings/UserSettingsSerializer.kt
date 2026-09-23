package io.github.chrisjmendoza.yearal.core.data.settings

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import io.github.chrisjmendoza.yearal.core.domain.settings.ColorPalette
import io.github.chrisjmendoza.yearal.core.domain.settings.ColorSource
import io.github.chrisjmendoza.yearal.core.domain.settings.ThemeMode
import io.github.chrisjmendoza.yearal.core.domain.settings.UserSettings
import io.github.chrisjmendoza.yearal.core.domain.settings.WeekdayDisplay
import io.github.chrisjmendoza.yearal.core.domain.settings.WidgetTheme
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

/**
 * On-disk shape of [UserSettings]. The domain type stays free of serialization annotations, so this
 * DTO mirrors it field for field. **Every field has a default** so that a file written by an older
 * app version (missing newer fields) still reads; unknown keys from a newer version are ignored by
 * [UserSettingsSerializer.json]. Renaming a field here changes the file format: add `@SerialName`
 * with the old name instead.
 *
 * **The visual-design-pass migration (`docs/design-plan.md` §5.1, §8.1):** this DTO deliberately has
 * no `dynamicColor` field any more. A file written before this change still carries that boolean key;
 * `ignoreUnknownKeys` (below) drops it silently, so the document decodes as if the key were never
 * there and [colorSource] takes its declared default, [ColorSource.BRAND] — the one-time, intentional
 * pre-1.0 switch to the brand palette. Do not re-add a `dynamicColor` field to reverse this.
 */
@Serializable
internal data class UserSettingsDto(
    val weekdayDisplay: WeekdayDisplay = UserSettings.DEFAULT.weekdayDisplay,
    val themeMode: ThemeMode = UserSettings.DEFAULT.themeMode,
    val colorSource: ColorSource = UserSettings.DEFAULT.colorSource,
    val palette: ColorPalette = UserSettings.DEFAULT.palette,
    val pureBlack: Boolean = UserSettings.DEFAULT.pureBlack,
    val enabledHolidaySets: Set<String> = UserSettings.DEFAULT.enabledHolidaySets,
    // Added for the first-run intro (docs/FEATURES.md L1). Defaulting to false is what makes a file
    // written before this field existed read as "intro not seen" instead of failing to parse — see
    // UserSettings.hasSeenIntro's KDoc for why that default is correct for both a fresh install and a
    // pre-intro install alike.
    val hasSeenIntro: Boolean = UserSettings.DEFAULT.hasSeenIntro,
    val todayWidgetTheme: WidgetTheme = UserSettings.DEFAULT.todayWidgetTheme,
    val monthWidgetTheme: WidgetTheme = UserSettings.DEFAULT.monthWidgetTheme,
    val widgetBackgroundOpacity: Int = UserSettings.DEFAULT.widgetBackgroundOpacity,
) {
    /**
     * This DTO's fields as a [UserSettings]. [widgetBackgroundOpacity] is coerced into `0..100`
     * rather than passed through: a value outside that range (a hand-edited or foreign file) must not
     * trip [UserSettingsSerializer]'s corruption handler and wipe every other setting along with it.
     */
    fun toDomain(): UserSettings =
        UserSettings(
            weekdayDisplay = weekdayDisplay,
            themeMode = themeMode,
            colorSource = colorSource,
            palette = palette,
            pureBlack = pureBlack,
            enabledHolidaySets = enabledHolidaySets,
            hasSeenIntro = hasSeenIntro,
            todayWidgetTheme = todayWidgetTheme,
            monthWidgetTheme = monthWidgetTheme,
            widgetBackgroundOpacity = widgetBackgroundOpacity.coerceIn(0, 100),
        )

    companion object {
        /** [settings]'s fields as the DTO shape [UserSettingsSerializer.writeTo] encodes. */
        fun fromDomain(settings: UserSettings): UserSettingsDto =
            UserSettingsDto(
                weekdayDisplay = settings.weekdayDisplay,
                themeMode = settings.themeMode,
                colorSource = settings.colorSource,
                palette = settings.palette,
                pureBlack = settings.pureBlack,
                enabledHolidaySets = settings.enabledHolidaySets,
                hasSeenIntro = settings.hasSeenIntro,
                todayWidgetTheme = settings.todayWidgetTheme,
                monthWidgetTheme = settings.monthWidgetTheme,
                widgetBackgroundOpacity = settings.widgetBackgroundOpacity,
            )
    }
}

/**
 * DataStore [Serializer] for [UserSettings] as a UTF-8 JSON document (`docs/ARCHITECTURE.md` §1
 * "datastore": kotlinx-serialization JSON, no protobuf).
 *
 * Compatibility guarantees, both tested in `UserSettingsSerializerTest`:
 * - **Backward:** a document missing a field (written before that field existed) reads with the
 *   [UserSettings.DEFAULT] value for it.
 * - **Forward:** a document with a key this version does not know (written by a newer version) reads
 *   normally; the unknown key is dropped on the next write.
 *
 * Anything that is not a valid document — malformed JSON, an unknown enum constant, a wrong value
 * type, an empty file — is reported as [CorruptionException] and nothing else, so DataStore's
 * `ReplaceFileCorruptionHandler` (wired in `SettingsModule`) can replace the file with the defaults.
 * An empty file counts as corruption rather than "no settings yet": DataStore never calls [readFrom]
 * for a file that does not exist, so a zero-length file means an interrupted or foreign write.
 */
public object UserSettingsSerializer : Serializer<UserSettings> {
    /**
     * `ignoreUnknownKeys` gives forward compatibility; `encodeDefaults` writes every field so the file
     * is self-describing and a later change of a default does not silently alter stored settings.
     */
    internal val json: Json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    /** The settings of a fresh install; what DataStore yields while no file exists. */
    override val defaultValue: UserSettings = UserSettings.DEFAULT

    /**
     * Decodes one JSON document from [input].
     *
     * @throws CorruptionException for any input that is not a valid settings document; never any
     * other exception type for bad content.
     */
    override suspend fun readFrom(input: InputStream): UserSettings {
        val text = input.readBytes().decodeToString()
        return try {
            json.decodeFromString(UserSettingsDto.serializer(), text).toDomain()
        } catch (e: SerializationException) {
            throw CorruptionException("User settings file is not a valid settings document", e)
        } catch (e: IllegalArgumentException) {
            throw CorruptionException("User settings file holds an invalid value", e)
        }
    }

    /** Encodes [t] as one JSON document with every field present, including defaults. */
    override suspend fun writeTo(
        t: UserSettings,
        output: OutputStream,
    ) {
        output.write(
            json.encodeToString(UserSettingsDto.serializer(), UserSettingsDto.fromDomain(t)).encodeToByteArray(),
        )
    }
}
