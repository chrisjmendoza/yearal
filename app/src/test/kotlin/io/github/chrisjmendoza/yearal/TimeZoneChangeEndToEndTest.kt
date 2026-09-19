package io.github.chrisjmendoza.yearal

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import java.util.TimeZone

/**
 * End to end through the real Hilt graph (in the spirit of [DayRolloverWiringTest], not fakes): a real
 * `TIMEZONE_CHANGED` broadcast with a genuinely changed default zone must update
 * [MainViewModel.today] on an already-created ViewModel, with no restart. This traces the whole R1
 * wiring: [io.github.chrisjmendoza.yearal.time.AndroidTimeChangeSignal] (started once at
 * `IfcApplication.onCreate`) through `TimeModule` into `RealDateTicker` into [MainViewModel.today].
 *
 * `Etc/GMT+12` (UTC−12) and `Pacific/Kiritimati` (UTC+14) are 26 hours apart, more than a full day, so
 * converting any single instant to both **always** yields two different calendar dates — the test does
 * not depend on the wall-clock time it happens to run at.
 *
 * Deliberately no `kotlinx-coroutines-test` (`runTest`/`Dispatchers.setMain`) here: swapping
 * `Dispatchers.Main` for a `StandardTestDispatcher` while [MainActivity] actually composes
 * `IfcApp` (a real Compose tree, not a bare `ComponentActivity` like `ConverterSharingTest`'s) hung
 * the JVM indefinitely — Compose's `Recomposer` expects a real, Choreographer-backed main dispatcher,
 * not a virtual-time one, to ever produce a frame. Robolectric's own (paused) main-looper dispatcher
 * avoids the swap entirely, and [idleMainLooper] pumps it instead of `runCurrent()`.
 */
@RunWith(AndroidJUnit4::class)
class TimeZoneChangeEndToEndTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val application: Application = context.applicationContext as Application
    private val originalDefaultZone: TimeZone = TimeZone.getDefault()

    private fun idleMainLooper() = shadowOf(Looper.getMainLooper()).idle()

    /**
     * Pumps the main looper until [value] returns a non-null result, or fails after [MAX_PUMPS] pumps.
     *
     * Everything in this chain runs on `viewModelScope`, i.e. `Dispatchers.Main`, which Robolectric
     * pauses: the ticker loop, its emission and `stateIn`'s sharing coroutine are all main-looper tasks.
     * So the only thing that advances them is pumping the looper — sleeping does not, because the paused
     * looper's clock does not follow real time. Reading `.value` after a single pump therefore passes or
     * fails on scheduling luck, which is exactly how this test failed under a loaded full-suite run and
     * again on CI.
     */
    private fun <T : Any> awaitOnMainLooper(
        what: String,
        value: () -> T?,
    ): T {
        repeat(MAX_PUMPS) {
            idleMainLooper()
            value()?.let { return it }
        }
        return value() ?: error("$what did not arrive after $MAX_PUMPS main-looper pumps")
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalDefaultZone)
    }

    @Test
    fun `a real TIMEZONE_CHANGED broadcast with a changed default zone updates MainViewModel today`() {
        TimeZone.setDefault(TimeZone.getTimeZone("Etc/GMT+12"))

        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        idleMainLooper()
        val viewModel = ViewModelProvider(controller.get())[MainViewModel::class.java]

        // MainViewModel.today shares WhileSubscribed, so the ticker only runs while something collects
        // it. In the app that subscriber is the Compose tree; under Robolectric, whether composition has
        // produced a frame by now is not something this test should depend on, so it subscribes itself.
        // Unconfined runs the collector inline here, so the subscription exists before the first pump.
        val subscriber = CoroutineScope(Dispatchers.Unconfined)
        subscriber.launch { viewModel.today.collect { } }

        val dateBefore = awaitOnMainLooper("the ticker's first date") { viewModel.today.value }

        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"))
        application.sendBroadcast(Intent(Intent.ACTION_TIMEZONE_CHANGED))

        val dateAfter =
            awaitOnMainLooper("the date for the new zone") {
                viewModel.today.value?.takeIf { it != dateBefore }
            }
        dateAfter shouldNotBe dateBefore

        subscriber.cancel()
        controller.pause().stop().destroy()
    }

    private companion object {
        /** Generous: each pump is cheap, and every step of the chain is a main-looper task. */
        const val MAX_PUMPS = 200
    }
}
