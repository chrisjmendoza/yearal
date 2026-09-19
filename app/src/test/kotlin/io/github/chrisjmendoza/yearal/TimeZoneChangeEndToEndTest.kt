package io.github.chrisjmendoza.yearal

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.shouldNotBe
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
 * not a virtual-time one, to ever produce a frame. Robolectric's own real (paused) main-looper
 * dispatcher plus [MainViewModel.today] being a plain `StateFlow` (read synchronously with `.value`,
 * no collection needed) avoids the swap entirely; [idleMainLooper] pumps it instead of `runCurrent()`.
 */
@RunWith(AndroidJUnit4::class)
class TimeZoneChangeEndToEndTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val application: Application = context.applicationContext as Application
    private val originalDefaultZone: TimeZone = TimeZone.getDefault()

    private fun idleMainLooper() = shadowOf(Looper.getMainLooper()).idle()

    /**
     * Pumps the main looper until [value] returns a non-null result, or fails after [TIMEOUT_MILLIS].
     *
     * `MainViewModel.today` is fed by `RealDateTicker` on a background dispatcher, so its first value
     * and its post-broadcast value both arrive on real time that a single `idle()` cannot force. Reading
     * once after one pump passes on an idle machine and fails when the run is loaded — this test did
     * exactly that in a full-suite run. `nanoTime` is a monotonic stopwatch, not a clock: nothing here
     * computes a date (CLAUDE.md rule 2).
     */
    private fun <T : Any> awaitOnMainLooper(
        what: String,
        value: () -> T?,
    ): T {
        val deadline = System.nanoTime() + TIMEOUT_MILLIS * NANOS_PER_MILLI
        while (System.nanoTime() < deadline) {
            idleMainLooper()
            value()?.let { return it }
            Thread.sleep(POLL_MILLIS)
        }
        idleMainLooper()
        return value() ?: error("$what did not arrive within $TIMEOUT_MILLIS ms")
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

        val dateBefore = awaitOnMainLooper("the ticker's first date") { viewModel.today.value }

        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"))
        application.sendBroadcast(Intent(Intent.ACTION_TIMEZONE_CHANGED))

        val dateAfter =
            awaitOnMainLooper("the date for the new zone") {
                viewModel.today.value?.takeIf { it != dateBefore }
            }
        dateAfter shouldNotBe dateBefore

        controller.pause().stop().destroy()
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
        const val POLL_MILLIS = 10L
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
