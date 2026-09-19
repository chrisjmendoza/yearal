package io.github.chrisjmendoza.yearal.core.domain

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * An external hint that "today" — or the zone a date should be computed in — may have changed for a
 * reason [RealDateTicker] cannot detect by itself: the device clock or time zone was changed by the
 * user or the system, or the process/activity just resumed after being backgrounded through such a
 * change.
 *
 * A firing carries no payload and is not itself proof that anything changed: it is only a prompt to
 * recompute from [java.time.Clock] and [ZoneProvider] again, right now, instead of waiting for the
 * next scheduled local-midnight delay. [RealDateTicker] still decides whether the recomputed date
 * actually differs before emitting, and `DefaultObserveAgendaUseCase` recomputes its whole agenda the
 * same way (`docs/contracts/Events.md` §5 promises re-emission on a zone change).
 *
 * The Android implementation is `AndroidTimeChangeSignal` in `:app`: it fires on a context-registered
 * `TIME_SET` / `TIMEZONE_CHANGED` / `DATE_CHANGED` receiver and on every activity resume
 * (`docs/ARCHITECTURE.md` §4 "State management"). `:core:testing`'s `FakeTimeChangeSignal` lets a test
 * fire it directly. [NoTimeChangeSignal] is the default for callers that need none.
 *
 * Spec: `docs/calendar-spec.md` §7.8.
 */
public interface TimeChangeSignal {
    /**
     * Emits `Unit` once per hint. **Never completes** on its own; a fresh subscription only ever sees
     * future firings, never a replay of one that happened before it subscribed.
     */
    public val changes: Flow<Unit>
}

/**
 * A [TimeChangeSignal] that never fires, so a caller with no real signal source (most JVM tests, and
 * anywhere the invalidation feature simply is not wired up) behaves exactly as it did before this type
 * existed.
 */
public object NoTimeChangeSignal : TimeChangeSignal {
    /**
     * Suspends forever instead of completing like [kotlinx.coroutines.flow.emptyFlow] would: a
     * collector racing this against a timer (as [RealDateTicker] does) must see "no signal yet" and
     * "no signal ever" as the same thing, not have the race resolve early just because this flow
     * finished.
     */
    override val changes: Flow<Unit> = flow { awaitCancellation() }
}
