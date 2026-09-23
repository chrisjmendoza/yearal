package io.github.chrisjmendoza.yearal.widget

import io.github.chrisjmendoza.yearal.core.testing.FakeSettingsRepository
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * [DebouncedWidgetUpdater] is the [io.github.chrisjmendoza.yearal.core.domain.widget.WidgetUpdater]
 * `RoomEventRepository` calls after every successful write (ROADMAP M5 T6;
 * docs/ARCHITECTURE.md §5 "Data"), and also the widget-follows-appearance hook (`docs/design-plan.md`
 * §4.9, §5.6): it watches [io.github.chrisjmendoza.yearal.core.domain.settings.SettingsRepository] and
 * requests a refresh through the same path on every settings change. These tests run both collectors
 * on `runTest`'s own virtual-time dispatcher (passed in as [DebouncedWidgetUpdater]'s scope) so the
 * one-second window is proven without a real sleep.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DebouncedWidgetUpdaterTest {
    private class RecordingRefresher : WidgetRefresher {
        var calls: Int = 0

        override suspend fun refreshAll() {
            calls++
        }
    }

    private val windowMillis = DebouncedWidgetUpdater.DEBOUNCE_WINDOW.inWholeMilliseconds

    @Test
    fun `a burst of requests collapses into exactly one refresh after the debounce window`() =
        runTest {
            val refresher = RecordingRefresher()
            val updater = DebouncedWidgetUpdater(refresher, FakeSettingsRepository(), backgroundScope)

            repeat(20) { updater.requestUpdate() }
            advanceTimeBy(windowMillis + 1)

            refresher.calls shouldBe 1
        }

    @Test
    fun `two bursts separated by more than the debounce window refresh exactly twice`() =
        runTest {
            val refresher = RecordingRefresher()
            val updater = DebouncedWidgetUpdater(refresher, FakeSettingsRepository(), backgroundScope)

            updater.requestUpdate()
            advanceTimeBy(windowMillis + 1)
            updater.requestUpdate()
            advanceTimeBy(windowMillis + 1)

            refresher.calls shouldBe 2
        }

    @Test
    fun `requests inside the window keep postponing the refresh, still only one`() =
        runTest {
            val refresher = RecordingRefresher()
            val updater = DebouncedWidgetUpdater(refresher, FakeSettingsRepository(), backgroundScope)

            updater.requestUpdate()
            advanceTimeBy(windowMillis / 2)
            updater.requestUpdate() // resets the window before it elapsed
            advanceTimeBy(windowMillis / 2)
            refresher.calls shouldBe 0 // the reset means the first window never fired

            advanceTimeBy(windowMillis)
            refresher.calls shouldBe 1
        }

    @Test
    fun `no requests means no refresh, ever`() =
        runTest {
            val refresher = RecordingRefresher()
            DebouncedWidgetUpdater(refresher, FakeSettingsRepository(), backgroundScope)

            advanceTimeBy(windowMillis * 10)

            refresher.calls shouldBe 0
        }

    @Test
    fun `a settings change requests a refresh through the same debounced path`() =
        runTest {
            val refresher = RecordingRefresher()
            val settingsRepository = FakeSettingsRepository()
            DebouncedWidgetUpdater(refresher, settingsRepository, backgroundScope)
            // Lets the settings-watching collector actually attach and record its baseline first --
            // exactly what a real Dispatchers.Default coroutine already does within microseconds of
            // construction, long before a human can reach the Settings screen (see
            // DebouncedWidgetUpdater's own KDoc on this same race for the `requests` flow).
            runCurrent()

            settingsRepository.update { it.copy(pureBlack = true) }
            advanceTimeBy(windowMillis + 1)

            refresher.calls shouldBe 1
        }

    @Test
    fun `the settings value already in effect at construction time does not itself trigger a refresh`() =
        runTest {
            val refresher = RecordingRefresher()
            val settingsRepository = FakeSettingsRepository()
            DebouncedWidgetUpdater(refresher, settingsRepository, backgroundScope)

            advanceTimeBy(windowMillis * 10)

            refresher.calls shouldBe 0
        }

    @Test
    fun `several settings changes in a burst still collapse into one refresh`() =
        runTest {
            val refresher = RecordingRefresher()
            val settingsRepository = FakeSettingsRepository()
            DebouncedWidgetUpdater(refresher, settingsRepository, backgroundScope)
            runCurrent()

            repeat(5) { settingsRepository.update { s -> s.copy(widgetBackgroundOpacity = 50 + it) } }
            advanceTimeBy(windowMillis + 1)

            refresher.calls shouldBe 1
        }
}
