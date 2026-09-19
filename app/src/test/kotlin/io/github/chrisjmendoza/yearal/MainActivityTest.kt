package io.github.chrisjmendoza.yearal

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.chrisjmendoza.yearal.core.scheduling.reminder.ReminderIntent
import io.github.chrisjmendoza.yearal.intent.AppRoute
import io.github.chrisjmendoza.yearal.widget.WidgetIntents
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.android.controller.ActivityController

/**
 * [MainActivity]'s `onCreate`/`onNewIntent` hand-off to [MainViewModel] (ROADMAP M3 T5, M4 T10;
 * `docs/ARCHITECTURE.md` §4 "Intent routing"), against the real Hilt graph exactly like
 * [DayRolloverWiringTest] — proving the actual wiring, not a substitute. What each [AppRoute] does to
 * the back stacks is [io.github.chrisjmendoza.yearal.ui.navigation.TabBackStacksTest]'s job, not this
 * class's; this only proves the activity lifecycle routes at the right times and not others.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    private fun dayIntent(epochDay: Long) =
        Intent(WidgetIntents.ACTION_OPEN_DAY).putExtra(WidgetIntents.EXTRA_EPOCH_DAY, epochDay)

    private fun eventIntent(eventId: Long) =
        Intent(ReminderIntent.ACTION_OPEN_EVENT).putExtra(ReminderIntent.EXTRA_EVENT_ID, eventId)

    private fun controllerWith(intent: Intent): ActivityController<MainActivity> =
        Robolectric.buildActivity(MainActivity::class.java, intent)

    @Test
    fun `onCreate with a day intent records the route`() {
        val activity = controllerWith(dayIntent(20_713L)).setup().get()

        activity.viewModel.pendingRoute.value shouldBe AppRoute.Day(20_713L)
    }

    @Test
    fun `onCreate with a plain launcher intent records the default route`() {
        val activity = controllerWith(Intent(Intent.ACTION_MAIN)).setup().get()

        activity.viewModel.pendingRoute.value shouldBe AppRoute.Default
    }

    @Test
    fun `onNewIntent always routes, replacing whatever onCreate left pending`() {
        val controller = controllerWith(dayIntent(1L)).setup()
        val activity = controller.get()
        activity.viewModel.consumeRoute()

        controller.newIntent(eventIntent(42L))

        activity.viewModel.pendingRoute.value shouldBe AppRoute.EventDetail(42L)
    }

    @Test
    fun `a second onCreate after a simulated recreation does not re-route`() {
        // ActivityController.recreate() tears down and rebuilds the Activity in the same process,
        // exactly like a configuration change: the activity-scoped MainViewModel instance is retained
        // across it, so the second onCreate's routeFromCreate call must see the route already consumed
        // and do nothing, rather than reintroducing the day-1 route this test already consumed.
        val controller = controllerWith(dayIntent(1L)).setup()
        val activity = controller.get()
        activity.viewModel.pendingRoute.value
            .shouldNotBeNull()
        activity.viewModel.consumeRoute()

        controller.recreate()

        controller
            .get()
            .viewModel.pendingRoute.value shouldBe null
    }
}
