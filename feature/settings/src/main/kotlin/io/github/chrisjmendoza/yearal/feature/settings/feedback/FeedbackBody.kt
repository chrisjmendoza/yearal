package io.github.chrisjmendoza.yearal.feature.settings.feedback

/*
 * ROADMAP M8 T6: the feedback email body. CLAUDE.md rule 8 ("no event content in anything that leaves
 * the app") applies here more than anywhere else in the app, because this text is handed to whatever
 * email app the user picks. [buildFeedbackBody] only ever assembles the lines its parameters carry, and
 * those parameters are exactly the allow-listed diagnostics below -- there is no parameter through which
 * an event title, note, location, or holiday-pack content could reach the body. Every line's wording is
 * a string resource resolved by the caller ([io.github.chrisjmendoza.yearal.feature.settings.more]'s
 * `MoreScreen`); this function is plain Kotlin (no Android import) purely so it can be unit-tested
 * without Robolectric.
 */

/**
 * Assembles the "Send feedback" email body: an invitation to describe the problem, then a diagnostics
 * block of device, app and settings values only -- never event or holiday-pack content.
 *
 * @param promptLine invites the user to describe what happened, shown above the diagnostics.
 * @param diagnosticsHeading introduces the diagnostics block.
 * @param appVersionLine the app's version name and code, e.g. "App version: 0.1.0 (3)".
 * @param androidVersionLine the OS release and API level, e.g. "Android: 14 (SDK 34)".
 * @param deviceLine manufacturer and model, e.g. "Device: Google Pixel 8".
 * @param localeLine the app's current locale tag, e.g. "Locale: en-US".
 * @param colorSourceLine the current [io.github.chrisjmendoza.yearal.core.domain.settings.ColorSource],
 * e.g. "Colour source: BRAND".
 * @param themeModeLine the current [io.github.chrisjmendoza.yearal.core.domain.settings.ThemeMode],
 * e.g. "Theme: SYSTEM".
 * @param weekdayDisplayLine the current
 * [io.github.chrisjmendoza.yearal.core.domain.settings.WeekdayDisplay], e.g. "Weekday display: BOTH".
 * @param holidaySetsLine how many holiday packs are enabled, e.g. "Enabled holiday sets: 2".
 * @return the lines joined in a fixed order, blank line after the prompt.
 */
internal fun buildFeedbackBody(
    promptLine: String,
    diagnosticsHeading: String,
    appVersionLine: String,
    androidVersionLine: String,
    deviceLine: String,
    localeLine: String,
    colorSourceLine: String,
    themeModeLine: String,
    weekdayDisplayLine: String,
    holidaySetsLine: String,
): String =
    buildString {
        appendLine(promptLine)
        appendLine()
        appendLine(diagnosticsHeading)
        appendLine(appVersionLine)
        appendLine(androidVersionLine)
        appendLine(deviceLine)
        appendLine(localeLine)
        appendLine(colorSourceLine)
        appendLine(themeModeLine)
        appendLine(weekdayDisplayLine)
        append(holidaySetsLine)
    }
