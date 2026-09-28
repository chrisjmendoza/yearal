package io.github.chrisjmendoza.yearal.feature.settings.more

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.chrisjmendoza.yearal.core.designsystem.theme.Dimens
import io.github.chrisjmendoza.yearal.core.designsystem.theme.IfcTheme
import io.github.chrisjmendoza.yearal.core.domain.settings.ColorSource
import io.github.chrisjmendoza.yearal.core.domain.settings.ThemeMode
import io.github.chrisjmendoza.yearal.core.domain.settings.UserSettings
import io.github.chrisjmendoza.yearal.core.domain.settings.WeekdayDisplay
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * [MoreScreen] under Robolectric: the Holidays, Settings, Learn, Privacy and Send feedback rows are
 * buttons that fire their own action, and the About row shows the app name and version without being
 * clickable. The feedback row's own behaviour (ROADMAP M8 T6; docs/security-and-privacy.md §6.3) is an
 * `ACTION_SENDTO` `mailto:` intent behind the system chooser -- restricted to email apps, no permission
 * needed -- carrying only the version-stamped subject and a body of device/app/settings diagnostics,
 * never event or holiday-pack content (CLAUDE.md rule 8). Also proves every row meets the 48dp
 * touch-target floor and spans the full screen width (a11y audit finding #27), and that the hub survives
 * 200% font scale (docs/ARCHITECTURE.md §4 "Accessibility").
 */
@RunWith(AndroidJUnit4::class)
class MoreScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val application = ApplicationProvider.getApplicationContext<Application>()

    private var holidaysClicks = 0
    private var settingsClicks = 0
    private var learnClicks = 0
    private var privacyClicks = 0

    private fun show(
        settings: UserSettings = UserSettings.DEFAULT,
        fontScale: Float = 1f,
        versionName: String = "0.1.0",
        versionCode: Int = 3,
    ) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                IfcTheme(dynamicColor = false) {
                    MoreScreen(
                        appName = "Yearal",
                        versionName = versionName,
                        versionCode = versionCode,
                        settings = settings,
                        onHolidaysClick = { holidaysClicks++ },
                        onSettingsClick = { settingsClicks++ },
                        onLearnClick = { learnClicks++ },
                        onPrivacyClick = { privacyClicks++ },
                    )
                }
            }
        }
    }

    @Test
    fun `Holidays row is clickable and invokes its callback`() {
        show()

        compose
            .onNodeWithText("Holidays")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        holidaysClicks shouldBe 1
        compose.onNodeWithText("Browse holiday sets and this year's dates").assertIsDisplayed()
    }

    @Test
    fun `Settings row is clickable and invokes its callback`() {
        show()

        compose
            .onNodeWithText("Settings")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        settingsClicks shouldBe 1
        compose.onNodeWithText("Weekday headers, appearance").assertIsDisplayed()
    }

    @Test
    fun `Learn row is clickable and invokes its callback`() {
        show()

        compose
            .onNodeWithText("Learn")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        learnClicks shouldBe 1
        compose.onNodeWithText("What the IFC is, how it works, and why the weekdays differ").assertIsDisplayed()
    }

    @Test
    fun `Privacy row is clickable and invokes its callback`() {
        show()

        compose
            .onNodeWithText("Privacy")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        privacyClicks shouldBe 1
        compose.onNodeWithText("What the app stores, and what it never does").assertIsDisplayed()
    }

    @Test
    fun `About row shows the app name and version and is not clickable`() {
        show()

        // The new Send feedback row (ROADMAP M8 T6) pushes About below the fold in this test window;
        // scroll it into view before asserting, same as any other row further down the hub.
        compose
            .onNodeWithText("Yearal")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHasNoClickAction()
        compose.onNodeWithText("Version 0.1.0 (3)").assertIsDisplayed()
        compose.onNodeWithText("More").assertIsDisplayed()
    }

    // A debug build's versionName carries SemVer build metadata (docs/release-builds.md "Version
    // numbers"); the About row shows the release version and build number only, the Android convention.
    @Test
    fun `About row shows a debug build as version and build number, without the metadata`() {
        show(versionName = "0.1.0+46.576c910.dirty", versionCode = 46)

        compose.onNodeWithText("Version 0.1.0 (46)").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("576c910", substring = true).assertCountEquals(0)
    }

    @Test
    fun `Send feedback row exists, is at least 48dp tall, and is clickable`() {
        show()

        compose
            .onNodeWithText("Send feedback")
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
        compose.onNodeWithText("Report a bug or suggestion by email").assertIsDisplayed()
    }

    // A11y audit finding #27: the Holidays/Settings/Learn/Privacy rows used to have no
    // `.fillMaxWidth()`, unlike Settings' own equivalent rows, so their tap target may not have spanned
    // the whole row's visible width. Every row (including Send feedback) now shares one modifier.
    @Test
    fun `every nav row is at least 48dp tall, matching Send feedback`() {
        show()

        listOf("Holidays", "Settings", "Learn", "Privacy").forEach { label ->
            compose.onNodeWithText(label).performScrollTo().assertHeightIsAtLeast(48.dp)
        }
    }

    // A11y audit finding #27, updated for the tablet pass (docs/ARCHITECTURE.md §4 "Adaptive layouts"):
    // this used to measure each row against the root window's width. Now that the hub's content column
    // caps at Dimens.ContentMaxWidth on a wide window, a row spans that capped column exactly, not the
    // whole screen — still proving every row's tap target is full-width, just full-width *of its column*.
    @Test
    fun `every nav row and Send feedback span the full width of the content column`() {
        show()

        val columnWidthPx =
            compose
                .onNodeWithTag(MoreScreenTestTags.CONTENT_COLUMN)
                .fetchSemanticsNode()
                .size.width

        listOf("Holidays", "Settings", "Learn", "Privacy", "Send feedback").forEach { label ->
            compose
                .onNodeWithText(label)
                .performScrollTo()
                .fetchSemanticsNode()
                .size.width shouldBe columnWidthPx
        }
    }

    // Tablet pass (docs/ARCHITECTURE.md §4 "Adaptive layouts"): on a wide window the content column
    // itself caps at Dimens.ContentMaxWidth rather than stretching every row edge to edge. Measured on
    // the "Holidays" row rather than the capped Column's own tag: `limitContentWidth()`'s outer
    // `fillMaxWidth()` reports the *window's* width up through its own `wrapContentWidth`/`widthIn`
    // wrapping (that is how it re-centres the narrower content), so the column's own semantics bounds
    // never shrink — the cap is only provable on a `fillMaxWidth()` row actually laid out *inside* the
    // column's now-narrowed measurement, which every nav row here is (see `navRowModifier`).
    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun `on a wide window the content column caps at ContentMaxWidth`() {
        show()

        val rowWidthPx =
            compose
                .onNodeWithText("Holidays")
                .fetchSemanticsNode()
                .size.width
        val maxWidthPx = with(compose.density) { Dimens.ContentMaxWidth.roundToPx() }

        rowWidthPx shouldBeLessThanOrEqual maxWidthPx
    }

    // docs/ARCHITECTURE.md §4 "Accessibility": 200% font scale never clips.
    @Test
    fun `at 200 percent font scale the rows and the About text stay displayed`() {
        show(fontScale = 2f)

        compose.onNodeWithText("Holidays").assertIsDisplayed()
        compose.onNodeWithText("Send feedback").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Yearal").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `tapping Send feedback fires an ACTION_SENDTO mailto intent with the expected extras`() {
        show(
            UserSettings.DEFAULT.copy(
                colorSource = ColorSource.DYNAMIC,
                themeMode = ThemeMode.DARK,
                weekdayDisplay = WeekdayDisplay.ACTUAL,
                enabledHolidaySets = setOf("ifc"),
            ),
        )

        compose.onNodeWithText("Send feedback").performClick()

        val chooser = shadowOf(application).nextStartedActivity.shouldNotBeNull()
        chooser.action shouldBe Intent.ACTION_CHOOSER
        val sendTo = chooser.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java).shouldNotBeNull()
        sendTo.action shouldBe Intent.ACTION_SENDTO
        sendTo.data shouldBe Uri.parse("mailto:")
        sendTo.getStringArrayExtra(Intent.EXTRA_EMAIL)?.toList() shouldBe listOf("chrisjmendoza@gmail.com")
        sendTo.getStringExtra(Intent.EXTRA_SUBJECT) shouldBe "Yearal feedback (v0.1.0)"

        val body = sendTo.getStringExtra(Intent.EXTRA_TEXT).shouldNotBeNull()
        listOf(
            "App version: 0.1.0 (3)",
            "Colour source: DYNAMIC",
            "Theme: DARK",
            "Weekday display: ACTUAL",
            "Enabled holiday sets: 1",
        ).forEach { fragment -> body shouldContain fragment }
    }
}
