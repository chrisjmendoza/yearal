package io.github.chrisjmendoza.yearal

import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.chrisjmendoza.yearal.testing.awaitNodeWithText
import io.github.chrisjmendoza.yearal.testing.awaitOnMainLooper
import io.github.chrisjmendoza.yearal.ui.IntroGateViewModel
import io.kotest.matchers.collections.shouldBeEmpty
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

/**
 * ROADMAP R10: proves "the intro shows once" (`docs/FEATURES.md` L1) end to end, composing the real
 * [IfcApp][io.github.chrisjmendoza.yearal.ui.IfcApp] through the real Hilt graph — the same shape of
 * proof as [DayRolloverWiringTest] and [MainActivityTest], but the first `:app` test to compose UI and
 * read its semantics, via [androidx.compose.ui.test.junit4.v2.createAndroidComposeRule]. Unit-level
 * coverage already exists for the pieces (`IntroViewModelTest`, `UserSettingsSerializerTest`'s migration
 * case, [IntroGateViewModel]'s null-until-loaded gate) — what was missing, per the roadmap entry, was a
 * test that actually launches the app twice against the *same persisted settings* and checks what is on
 * screen, not just what a ViewModel's `StateFlow` holds.
 *
 * The compose rule hosts [MainActivity] (`createAndroidComposeRule<MainActivity>()`, not
 * `createComposeRule()`: [IfcApp][io.github.chrisjmendoza.yearal.ui.IfcApp] calls `hiltViewModel()`,
 * which needs a real `@AndroidEntryPoint` activity, not the rule's own bare host). "The relaunch" is
 * proved by closing that first activity's [androidx.test.core.app.ActivityScenario] — which clears its
 * `ViewModelStore`, exactly like a real task finishing — and then building a **second**, independent
 * [MainActivity] with [Robolectric.buildActivity] (the same primitive [MainActivityTest] and
 * [TimeZoneChangeEndToEndTest] use): a fresh [IntroGateViewModel] instance, not the first one recomposed,
 * reading the same, already-durably-written
 * [io.github.chrisjmendoza.yearal.core.domain.settings.SettingsRepository]. The compose rule's node
 * queries keep working against that second activity because Compose UI Test hooks recomposition
 * globally for the life of the rule, not per `ActivityScenario` (its own KDoc: `createAndroidComposeRule`
 * supports "compose content ... set by [an] Activity", exactly this shape) — confirmed empirically here,
 * not just asserted: [compose] finds the second activity's nodes with no re-creation of the rule itself.
 *
 * What this deliberately does **not** attempt: catching the intro appearing while settings are still
 * loading. [IntroGateViewModel.hasSeenIntro]'s null-until-loaded state already has a unit test proving
 * [IfcApp][io.github.chrisjmendoza.yearal.ui.IfcApp] waits for it; reproducing that at this level would
 * mean racing the real, uncontrolled wall-clock timing of DataStore's first disk read against Compose's
 * first frame — exactly the kind of test that "passes on an idle machine and fails under load or on CI"
 * `docs/WORKFLOW.md` §2 warns about. Forcing that timing deterministically would need a fake
 * `SettingsRepository` wired in through Hilt (a `@TestInstallIn` module and a `@HiltAndroidTest` runner,
 * neither of which exist in this project yet — see `io.github.chrisjmendoza.yearal.testing`'s KDoc for
 * why an `@EntryPoint` is not a shortcut around that) — out of scope for this task.
 */
@RunWith(AndroidJUnit4::class)
class IntroOnceEndToEndTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun idleMainLooper() = shadowOf(Looper.getMainLooper()).idle()

    private fun introGateViewModel(activity: MainActivity) = ViewModelProvider(activity)[IntroGateViewModel::class.java]

    /**
     * Waits for [IntroGateViewModel.hasSeenIntro] to become `true` on [activity]'s own instance —
     * i.e. for the write [io.github.chrisjmendoza.yearal.feature.settings.intro.IntroViewModel.markSeen]
     * made to actually land, not just for the click that triggered it to return. `SettingsRepository
     * .update` (the real, DataStore-backed implementation) only completes once the write is durable, and
     * that completion is what resumes the coroutine feeding this `StateFlow` — so once this returns, a
     * completely fresh reader of the same store (a new process, or — as this test's last case does — a
     * new [IntroGateViewModel] instance) is guaranteed to see it too.
     */
    private fun awaitPersistedIntroSeen(activity: MainActivity) {
        awaitOnMainLooper("hasSeenIntro to persist as true") {
            introGateViewModel(activity).hasSeenIntro.value?.takeIf { it }
        }
    }

    @Test
    fun `first launch with empty settings shows the intro`() {
        compose.awaitNodeWithText("Welcome")
        compose.onNodeWithText("Skip").assertIsDisplayed()
    }

    @Test
    fun `dismissing the intro reveals the normal shell and persists that it was seen`() {
        compose.awaitNodeWithText("Welcome")

        compose.onNodeWithText("Skip").performClick()
        awaitPersistedIntroSeen(compose.activity)

        // The intro's own title/skip action are gone, and the Today tab underneath -- always an
        // IFC-prefixed date (CLAUDE.md rule 5) once it has a date to show -- is what is left on screen.
        compose.awaitNodeWithText("IFC ", substring = true)
        compose.onAllNodesWithText("Welcome").fetchSemanticsNodes().shouldBeEmpty()
    }

    @Test
    fun `a relaunch over the same persisted settings does not show the intro again`() {
        compose.awaitNodeWithText("Welcome")
        compose.onNodeWithText("Skip").performClick()
        awaitPersistedIntroSeen(compose.activity)

        // Finishing this ActivityScenario clears its ViewModelStore, same as a real task finishing.
        compose.activityRule.scenario.close()

        // A brand-new MainActivity: a fresh IntroGateViewModel instance (not the one just dismissed)
        // reading the settings this test just proved are durably persisted -- the actual point of R10.
        val relaunch = Robolectric.buildActivity(MainActivity::class.java).setup()
        idleMainLooper()

        compose.awaitNodeWithText("IFC ", substring = true)
        compose.onAllNodesWithText("Welcome").fetchSemanticsNodes().shouldBeEmpty()

        relaunch.pause().stop().destroy()
    }
}
