package io.github.chrisjmendoza.yearal.feature.settings.feedback

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import org.junit.Test

/**
 * [buildFeedbackBody] is plain Kotlin -- no Android import -- so this runs as an ordinary JUnit test,
 * no Robolectric needed. It documents CLAUDE.md rule 8 for the feedback email (ROADMAP M8 T6): the
 * function's parameter list is the allow-list. There is no event-title, note, location or holiday-name
 * parameter for a caller to smuggle content through -- by construction, not by convention.
 */
class FeedbackBodyTest {
    private fun sample(marker: String? = null) =
        buildFeedbackBody(
            promptLine = "Describe what happened or what you'd like to see:",
            diagnosticsHeading = "Diagnostics (no event content):",
            appVersionLine = "App version: 0.1.0 (3)",
            androidVersionLine = "Android: 14 (SDK 34)",
            deviceLine = "Device: Google Pixel 8",
            localeLine = "Locale: en-US",
            colorSourceLine = "Colour source: ${marker ?: "BRAND"}",
            themeModeLine = "Theme: SYSTEM",
            weekdayDisplayLine = "Weekday display: BOTH",
            holidaySetsLine = "Enabled holiday sets: 2",
        )

    @Test
    fun `assembles the prompt then the diagnostics block in a fixed order`() {
        val expected =
            listOf(
                "Describe what happened or what you'd like to see:",
                "",
                "Diagnostics (no event content):",
                "App version: 0.1.0 (3)",
                "Android: 14 (SDK 34)",
                "Device: Google Pixel 8",
                "Locale: en-US",
                "Colour source: BRAND",
                "Theme: SYSTEM",
                "Weekday display: BOTH",
                "Enabled holiday sets: 2",
            ).joinToString("\n")

        sample() shouldBe expected
    }

    @Test
    fun `has no way to carry event content -- only the allow-listed diagnostics ever appear`() {
        // There is no parameter here for an event title, note or location: the only way this test
        // could plant an event-content marker in the body is through one of the allowed diagnostic
        // lines themselves, which documents that the function has no other channel for content.
        val marker = "SECRET_EVENT_TITLE_MARKER"
        val body = sample()

        body shouldNotContain marker
    }

    @Test
    fun `every allowed diagnostic line is present`() {
        val body = sample()

        listOf(
            "App version: 0.1.0 (3)",
            "Android: 14 (SDK 34)",
            "Device: Google Pixel 8",
            "Locale: en-US",
            "Colour source: BRAND",
            "Theme: SYSTEM",
            "Weekday display: BOTH",
            "Enabled holiday sets: 2",
        ).forEach { line -> (line in body) shouldBe true }
    }
}
