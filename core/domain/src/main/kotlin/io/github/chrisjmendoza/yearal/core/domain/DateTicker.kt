package io.github.chrisjmendoza.yearal.core.domain

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Clock
import java.time.Duration
import java.time.LocalDate

/**
 * A stream of the current local date that re-emits at every local midnight.
 *
 * This is the single source of "today" for anything that must not call [java.time.LocalDate.now]
 * itself (CLAUDE.md rule 2): a "today" highlight, a widget, a notification. It never derives a date
 * from epoch milliseconds.
 *
 * Spec: `docs/calendar-spec.md` §7.8; `docs/ARCHITECTURE.md` "State management" and "Midnight
 * rollover".
 */
public interface DateTicker {
    /**
     * Emits the current local date immediately on collection, then again after every local
     * midnight for as long as it is collected. Never completes on its own; cancel the collecting
     * coroutine to stop it.
     */
    public val today: Flow<LocalDate>
}

/**
 * [DateTicker] driven by a real [clock] and [zoneProvider], with an optional [timeChangeSignal] that
 * lets an outside caller force an immediate recompute.
 *
 * Each cycle re-reads [zoneProvider], so a zone change while the app is alive is picked up on the
 * next emission rather than baked in at construction, and computes the delay until the next local
 * midnight from [clock] instead of a fixed 24 hours, so it self-corrects after being suspended
 * (device sleep, a DST transition that is shorter or longer than 24 hours) for a different amount
 * of real time than expected.
 *
 * Between emissions this waits for **either** that midnight delay **or** a firing of
 * [timeChangeSignal], whichever comes first — a firing preempts the pending delay rather than merely
 * being noted for later. Either way it then re-reads [clock] and [zoneProvider], emits only if the
 * recomputed date actually differs from the last one emitted (so a spurious or no-op signal, or one
 * that does not cross a date boundary, never produces a duplicate), and arms a fresh delay to the
 * *new* next local midnight. A clock moved backwards across a boundary emits the earlier date; nothing
 * about this class assumes dates only move forward.
 *
 * [timeChangeSignal] defaults to [NoTimeChangeSignal], so existing callers that construct this with
 * only a [clock] and a [zoneProvider] keep exactly their previous behaviour: a plain midnight-only
 * ticker. The Android-specific "also re-emit on resume and on `TIME_SET`/`TIMEZONE_CHANGED`/
 * `DATE_CHANGED`" behaviour (`docs/ARCHITECTURE.md` "State management") lives outside this pure JVM
 * module, in `:app`'s `AndroidTimeChangeSignal`, and reaches this class only through that parameter.
 *
 * Spec: `docs/calendar-spec.md` §7.8.
 */
public class RealDateTicker(
    private val clock: Clock,
    private val zoneProvider: ZoneProvider,
    private val timeChangeSignal: TimeChangeSignal = NoTimeChangeSignal,
) : DateTicker {
    override val today: Flow<LocalDate>
        get() =
            flow {
                coroutineScope {
                    // Conflated: a burst of firings while this is busy recomputing collapses to one
                    // wake-up, which is correct because a firing carries no data — every wake-up reads
                    // the clock and zone fresh regardless of how many hints preceded it.
                    val trigger = Channel<Unit>(Channel.CONFLATED)
                    val forwarder =
                        launch {
                            timeChangeSignal.changes.collect { trigger.trySend(Unit) }
                        }
                    try {
                        var lastEmitted: LocalDate? = null
                        while (true) {
                            val zone = zoneProvider.currentZone()
                            val now = clock.instant()
                            // Not LocalDate.ofInstant: that is a Java 9 API, absent below API 34.
                            val today = now.atZone(zone).toLocalDate()
                            if (today != lastEmitted) {
                                lastEmitted = today
                                emit(today)
                            }
                            val nextMidnight = today.plusDays(1).atStartOfDay(zone).toInstant()
                            val untilMidnight = Duration.between(now, nextMidnight).coerceAtLeast(Duration.ZERO)
                            // Whichever happens first: the delay elapses, or a hint arrives and
                            // preempts it. Either way the loop above re-reads the clock and zone.
                            withTimeoutOrNull(untilMidnight.toMillis()) { trigger.receive() }
                        }
                    } finally {
                        forwarder.cancel()
                        trigger.close()
                    }
                }
            }
}
