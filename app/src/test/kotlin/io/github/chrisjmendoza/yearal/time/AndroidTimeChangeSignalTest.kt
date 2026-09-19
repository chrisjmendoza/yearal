package io.github.chrisjmendoza.yearal.time

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

/**
 * [AndroidTimeChangeSignal]: the three watched broadcasts each fire it once and a foreign action does
 * not, an activity resume fires it, `start()` is idempotent, and the context-registered receiver is
 * not exported (`docs/security-and-privacy.md` §6.3; ROADMAP R1).
 *
 * This runs against the real, `@HiltAndroidApp` `IfcApplication` (`ApplicationProvider.getApplicationContext()`
 * is what Robolectric boots from the merged manifest; there is no `@Config(application = ...)` override
 * here). A plain stand-in `Application` was tried first, but `sendBroadcast` is process-global: it also
 * reaches `:core:scheduling`'s manifest-registered `SystemEventReceiver`, which reacts to the same
 * `TIME_CHANGED`/`TIMEZONE_CHANGED` actions and does its own `EntryPointAccessors.fromApplication`
 * lookup — against a stand-in `Application` that throws (it is not a generated Hilt component). Because
 * the real graph is used, [ownReceiverWrapper] distinguishes this class's own receiver from any other
 * the merged app also registers.
 *
 * **No JUnit4 `Timeout` rule here on purpose.** `org.junit.rules.Timeout` runs the test body on a
 * second thread so it can interrupt it, but Robolectric's paused-looper mode requires
 * `shadowOf(Looper.getMainLooper()).idle()` and `Robolectric.buildActivity(...)` to be called from the
 * test's own thread — tried once, it turned `shadowOf(...).idle()` into
 * `IllegalStateException: Main looper can only be controlled from its thread in PAUSED mode` and
 * `buildActivity` into `IllegalStateException: buildActivity must be called on main Looper thread`,
 * a real Robolectric/JUnit4-`Timeout` incompatibility, not a flaky assertion. Every `runTest` body
 * below already has kotlinx-coroutines-test's own 60 s default coroutine deadline, which needs no
 * second thread and so does not fight Robolectric; the one non-suspending test has no wait to hang on.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AndroidTimeChangeSignalTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val application: Application = context.applicationContext as Application

    private fun idleMainLooper() = shadowOf(Looper.getMainLooper()).idle()

    private fun startedSignal(): AndroidTimeChangeSignal = AndroidTimeChangeSignal(context).apply { start() }

    /**
     * The wrapper Robolectric recorded for **this class's own** receiver, among possibly several
     * registered for the same standard actions (WorkManager, pulled in by Glance, registers its own).
     * An anonymous `object : BroadcastReceiver()` declared as a property initializer of
     * [AndroidTimeChangeSignal] has that class as its `enclosingClass`, which is what distinguishes it
     * without [AndroidTimeChangeSignal] needing to expose the receiver itself.
     */
    private fun ownReceiverWrapper() =
        shadowOf(application)
            .registeredReceivers
            .firstOrNull { it.broadcastReceiver.javaClass.enclosingClass == AndroidTimeChangeSignal::class.java }
            .shouldNotBeNull()

    @Test
    fun `each of the three watched broadcasts fires the signal once`() =
        runTest {
            val signal = startedSignal()
            val received = mutableListOf<Unit>()
            val collector = launch { signal.changes.toList(received) }
            runCurrent() // let the collector actually subscribe before anything fires.

            application.sendBroadcast(Intent(Intent.ACTION_TIME_CHANGED))
            application.sendBroadcast(Intent(Intent.ACTION_TIMEZONE_CHANGED))
            application.sendBroadcast(Intent(Intent.ACTION_DATE_CHANGED))
            idleMainLooper()
            runCurrent()

            received.shouldHaveSize(3)
            collector.cancel()
        }

    @Test
    fun `a foreign broadcast action does not fire the signal`() =
        runTest {
            val signal = startedSignal()
            val received = mutableListOf<Unit>()
            val collector = launch { signal.changes.toList(received) }
            runCurrent()

            application.sendBroadcast(Intent(Intent.ACTION_BATTERY_CHANGED))
            application.sendBroadcast(Intent(Intent.ACTION_LOCALE_CHANGED))
            idleMainLooper()
            runCurrent()

            received.shouldBeEmpty()
            collector.cancel()
        }

    @Test
    fun `an activity resume fires the signal`() =
        runTest {
            val signal = startedSignal()
            val received = mutableListOf<Unit>()
            val collector = launch { signal.changes.toList(received) }
            runCurrent()

            val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
            idleMainLooper()
            runCurrent()

            received.shouldHaveSize(1)
            collector.cancel()
            controller.pause().stop().destroy()
        }

    @Test
    fun `the context-registered receiver watches all three actions and is not exported`() {
        startedSignal()

        val wrapper = ownReceiverWrapper()

        assertSoftly {
            wrapper.intentFilter.hasAction(Intent.ACTION_TIME_CHANGED).shouldBeTrue()
            wrapper.intentFilter.hasAction(Intent.ACTION_TIMEZONE_CHANGED).shouldBeTrue()
            wrapper.intentFilter.hasAction(Intent.ACTION_DATE_CHANGED).shouldBeTrue()
            (wrapper.flags and ContextCompat.RECEIVER_NOT_EXPORTED) shouldBe ContextCompat.RECEIVER_NOT_EXPORTED
        }
    }

    @Test
    fun `start is idempotent, so a broadcast still fires the signal exactly once after three calls`() =
        runTest {
            val signal = AndroidTimeChangeSignal(context)
            signal.start()
            signal.start()
            signal.start()

            val received = mutableListOf<Unit>()
            val collector = launch { signal.changes.toList(received) }
            runCurrent()

            // A duplicate registration would deliver this broadcast to onReceive twice, not once.
            application.sendBroadcast(Intent(Intent.ACTION_TIME_CHANGED))
            idleMainLooper()
            runCurrent()

            received.shouldHaveSize(1)
            collector.cancel()
        }
}
