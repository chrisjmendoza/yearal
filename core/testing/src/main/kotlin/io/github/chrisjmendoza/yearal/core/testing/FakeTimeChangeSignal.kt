package io.github.chrisjmendoza.yearal.core.testing

import io.github.chrisjmendoza.yearal.core.domain.TimeChangeSignal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * A hand-written [TimeChangeSignal] test double: [fire] hands off a hint synchronously, so a test can
 * simulate a `TIME_SET`/`TIMEZONE_CHANGED`/`DATE_CHANGED` broadcast or an activity resume mid-test
 * without touching coroutines itself, the same way [FakeDateTicker.set] and [FakeZoneProvider.set]
 * simulate their own events.
 *
 * The underlying buffer never drops a firing (CLAUDE.md rule 12 does not apply here, but a flaky test
 * would be just as bad): [fire] never needs the collector under test to be actively suspended in
 * [TimeChangeSignal.changes] at the moment it is called.
 */
public class FakeTimeChangeSignal : TimeChangeSignal {
    private val signal = MutableSharedFlow<Unit>(extraBufferCapacity = Int.MAX_VALUE)

    /** Emits once per [fire] call; never completes. */
    override val changes: Flow<Unit> = signal.asSharedFlow()

    /** Emits one hint, as if a system broadcast or a resume had just happened. */
    public fun fire() {
        check(signal.tryEmit(Unit)) { "FakeTimeChangeSignal.fire() could not emit" }
    }
}
