package io.github.chrisjmendoza.yearal.core.testing

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FakeTimeChangeSignalTest {
    @Test
    fun `fire delivers one hint per call to an active collector`() =
        runTest {
            val signal = FakeTimeChangeSignal()
            val received = mutableListOf<Unit>()
            val collector = launch { signal.changes.toList(received) }
            runCurrent()

            signal.fire()
            runCurrent()
            signal.fire()
            runCurrent()

            received.size shouldBe 2
            collector.cancel()
        }

    @Test
    fun `fire never blocks even with no collector`() {
        // Must not throw or suspend: production callers (a BroadcastReceiver's onReceive) call this
        // from a synchronous, non-suspending context.
        FakeTimeChangeSignal().fire()
    }
}
