package io.github.chrisjmendoza.yearal

import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.chrisjmendoza.yearal.core.scheduling.reminder.ReminderIntent
import io.github.chrisjmendoza.yearal.core.testing.FakeDateTicker
import io.github.chrisjmendoza.yearal.core.testing.FakeSettingsRepository
import io.github.chrisjmendoza.yearal.intent.AppRoute
import io.github.chrisjmendoza.yearal.widget.WidgetIntents
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * [MainViewModel]'s intent-routing hand-off (ROADMAP M3 T5, M4 T10): [MainViewModel.routeFromCreate]
 * routes an activity's launch intent exactly once per lifetime — including across a simulated process
 * restart, where a fresh [MainViewModel] but a **restored** [SavedStateHandle] must still not re-route
 * — while [MainViewModel.routeFromNewIntent] always routes, since it only ever fires for a genuinely
 * new tap. [io.github.chrisjmendoza.yearal.ui.navigation.TabBackStacksTest] covers what a route
 * actually does to the back stacks; this covers only the exactly-once plumbing.
 */
@RunWith(AndroidJUnit4::class)
class MainViewModelTest {
    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) =
        MainViewModel(
            dateTicker = FakeDateTicker(LocalDate.of(2026, 9, 17)),
            settingsRepository = FakeSettingsRepository(),
            savedStateHandle = handle,
        )

    private fun dayIntent(epochDay: Long) =
        Intent(WidgetIntents.ACTION_OPEN_DAY).putExtra(WidgetIntents.EXTRA_EPOCH_DAY, epochDay)

    private fun eventIntent(eventId: Long) =
        Intent(ReminderIntent.ACTION_OPEN_EVENT).putExtra(ReminderIntent.EXTRA_EVENT_ID, eventId)

    /** Simulates a `Bundle` round trip through process death, the same pattern used across the app's ViewModel tests. */
    private fun restored(handle: SavedStateHandle) =
        SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) })

    @Test
    fun `routeFromCreate resolves the intent through IntentRouter`() {
        val viewModel = viewModel()

        viewModel.routeFromCreate(dayIntent(20_713L))

        viewModel.pendingRoute.value shouldBe AppRoute.Day(20_713L)
    }

    @Test
    fun `routeFromCreate on a plain launcher intent produces no route`() {
        val viewModel = viewModel()

        viewModel.routeFromCreate(Intent(Intent.ACTION_MAIN))

        viewModel.pendingRoute.value shouldBe AppRoute.Default
    }

    @Test
    fun `a second routeFromCreate on the same ViewModel instance does not re-route`() {
        // Models a configuration change: the activity-scoped ViewModel instance survives it, so
        // onCreate calling routeFromCreate again must be a no-op.
        val viewModel = viewModel()
        viewModel.routeFromCreate(dayIntent(1L))
        viewModel.consumeRoute()

        viewModel.routeFromCreate(dayIntent(2L))

        viewModel.pendingRoute.value shouldBe null
    }

    @Test
    fun `routeFromCreate does not re-route after a simulated process restart`() {
        // Models Android re-delivering the original launch Intent to a brand-new process: a fresh
        // MainViewModel, but the SavedStateHandle's Bundle survived and already recorded "routed".
        val handle = SavedStateHandle()
        val first = viewModel(handle)
        first.routeFromCreate(eventIntent(42L))
        first.consumeRoute()

        val secondProcess = viewModel(restored(handle))
        secondProcess.routeFromCreate(eventIntent(42L))

        secondProcess.pendingRoute.value shouldBe null
    }

    @Test
    fun `a fresh ViewModel with no prior routing history routes normally`() {
        // The counterpart of the process-restart case: an ordinary cold start, where the
        // SavedStateHandle has never recorded anything.
        val viewModel = viewModel(SavedStateHandle())

        viewModel.routeFromCreate(eventIntent(7L))

        viewModel.pendingRoute.value shouldBe AppRoute.EventDetail(7L)
    }

    @Test
    fun `routeFromNewIntent always routes, even after routeFromCreate already has`() {
        val viewModel = viewModel()
        viewModel.routeFromCreate(dayIntent(1L))
        viewModel.consumeRoute()

        viewModel.routeFromNewIntent(dayIntent(2L))

        viewModel.pendingRoute.value shouldBe AppRoute.Day(2L)
    }

    @Test
    fun `repeated taps each route freshly through routeFromNewIntent`() {
        val viewModel = viewModel()

        viewModel.routeFromNewIntent(eventIntent(1L))
        viewModel.consumeRoute()
        viewModel.routeFromNewIntent(eventIntent(2L))

        viewModel.pendingRoute.value shouldBe AppRoute.EventDetail(2L)
    }

    @Test
    fun `consumeRoute clears the pending route`() {
        val viewModel = viewModel()
        viewModel.routeFromCreate(dayIntent(1L))

        viewModel.consumeRoute()

        viewModel.pendingRoute.value shouldBe null
    }
}
