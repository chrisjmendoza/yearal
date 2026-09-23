package io.github.chrisjmendoza.yearal.widget

import io.github.chrisjmendoza.yearal.core.domain.settings.SettingsRepository
import io.github.chrisjmendoza.yearal.core.domain.widget.WidgetUpdater
import io.github.chrisjmendoza.yearal.widget.di.WidgetCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

/**
 * The production [WidgetUpdater] (docs/ARCHITECTURE.md §5 "Data": "the implementation is here as a
 * debounced `updateAll`"; ROADMAP M5 T6): [requestUpdate] enqueues a signal and returns immediately, a
 * single collector coroutine [debounce]s bursts of signals arriving within [DEBOUNCE_WINDOW] of each
 * other, and every settled burst results in exactly one [refresher] call.
 *
 * **Must be a Hilt singleton.** [io.github.chrisjmendoza.yearal.core.data.event.RoomEventRepository]
 * holds one instance for the process's lifetime through constructor injection (see
 * [io.github.chrisjmendoza.yearal.widget.di.WidgetModule]).
 *
 * **Trap found while building this class: `scope.launch { requests.collect { … } }` does not subscribe
 * synchronously.** `launch` only *schedules* the collector; on a real dispatcher (or a virtual-time test
 * dispatcher) it may not actually attach until after this constructor returns, so a caller that gets a
 * reference to a freshly-constructed [DebouncedWidgetUpdater] and calls [requestUpdate] immediately can
 * race the subscription. With `replay = 0` that first signal would simply be dropped (a `SharedFlow`
 * with no replay only buffers values *for subscribers that are already attached*, never for one that
 * has not subscribed yet) -- caught by [DebouncedWidgetUpdaterTest]'s very first assertion failing.
 * [requests] is built with `replay = 1` instead: whatever the most recent signal was is redelivered to
 * whichever subscriber attaches, whenever it attaches, which is what actually closes the race, together
 * with [BufferOverflow.DROP_OLDEST] so a burst can never suspend or fail the calling write. Dropping a
 * duplicate "please refresh" signal changes nothing observable -- one refresh after a burst is exactly
 * as correct as several.
 *
 * **Also the widget-follows-appearance hook** (`docs/design-plan.md` §4.9, §5.6; ARCHITECTURE §5): a
 * second collector in [init] watches [settingsRepository] and calls [requestUpdate] on every change
 * after the first (the value seen on subscription is the settings already in effect, not a change), so
 * a palette, theme or widget-appearance edit in Settings re-renders every placed widget through the
 * same debounced, off-main-thread path an event write uses -- no separate scheduling, no separate
 * thread policy to get right twice. **The same subscribe-race as above, solved differently:** a plain
 * `drop(1)` would drop whatever value the collector *actually* first observes, which -- because
 * [SettingsRepository.settings] is conflated, like any `StateFlow` -- could already be a changed value
 * if the change happened before this `launch` got to attach, silently eating it. Reading [first] gives
 * a definite baseline instead, and [dropWhile] then skips only values *equal* to it; a value that
 * differs from that baseline always gets through, however early or late the collector actually starts.
 *
 * @param refresher re-renders every widget this module owns; the same seam
 *   [WidgetRolloverListener] uses for the midnight rollover path.
 * @param settingsRepository the app's [UserSettings][io.github.chrisjmendoza.yearal.core.domain.settings.UserSettings],
 *   watched for the appearance-follows-app behaviour above.
 * @param scope where both collectors run; a real [kotlinx.coroutines.Dispatchers.Default]-backed
 *   scope in production (bound in [io.github.chrisjmendoza.yearal.widget.di.WidgetModule]) and a test
 *   scope with a virtual-time dispatcher in tests, so the debounce window can be proven without a real
 *   1-second sleep.
 */
@Singleton
@OptIn(FlowPreview::class)
class DebouncedWidgetUpdater
    @Inject
    constructor(
        private val refresher: WidgetRefresher,
        private val settingsRepository: SettingsRepository,
        @param:WidgetCoroutineScope private val scope: CoroutineScope,
    ) : WidgetUpdater {
        private val requests =
            MutableSharedFlow<Unit>(
                replay = 1,
                extraBufferCapacity = REQUEST_BUFFER_CAPACITY,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )

        init {
            scope.launch {
                requests.debounce(DEBOUNCE_WINDOW).collect {
                    refresher.refreshAll()
                }
            }
            scope.launch {
                val initial = settingsRepository.settings.first()
                settingsRepository.settings.dropWhile { it == initial }.distinctUntilChanged().collect {
                    requestUpdate()
                }
            }
        }

        /** Buffers the signal (never suspends, never throws) for the debounced collector to pick up. */
        override fun requestUpdate() {
            requests.tryEmit(Unit)
        }

        /** `internal` so a test can assert on the exact debounce window without duplicating the constant. */
        internal companion object {
            val DEBOUNCE_WINDOW = 1.seconds
            const val REQUEST_BUFFER_CAPACITY = 64
        }
    }
