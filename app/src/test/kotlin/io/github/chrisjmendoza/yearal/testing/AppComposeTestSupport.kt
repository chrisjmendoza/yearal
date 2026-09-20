package io.github.chrisjmendoza.yearal.testing

import android.os.Looper
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import org.robolectric.Shadows.shadowOf

/**
 * Shared harness for `:app` Robolectric tests that compose the real app through the real Hilt graph
 * (ROADMAP R10's [io.github.chrisjmendoza.yearal.IntroOnceEndToEndTest] is the first one) rather than
 * fakes, in the spirit of [io.github.chrisjmendoza.yearal.DayRolloverWiringTest] and
 * [io.github.chrisjmendoza.yearal.MainActivityTest] — but the first to also read Compose semantics, so it
 * inherits a problem those two never had to solve: **the paused main looper**
 * (`docs/WORKFLOW.md` §2). Everything on `Dispatchers.Main` — including `viewModelScope`, so every
 * `stateIn` sharing in this app — only advances when the looper is pumped;
 * [ComposeTestRule]'s own Robolectric idling strategy pumps it as part of `waitForIdle()`/node queries,
 * but only up to Espresso's own idle timeout, and a query that runs before a `Dispatchers.Main` write has
 * landed simply reports the node as absent rather than waiting for it. [awaitNodeWithText] and
 * [awaitOnMainLooper] both pump explicitly, in a bounded loop, and fail naming what never arrived, rather
 * than trusting a single pass or hanging.
 *
 * There is deliberately no shortcut here that reaches into the Hilt graph directly (an `@EntryPoint`
 * cannot be merged into the app's actual runtime `SingletonComponent` without a `@HiltAndroidTest` runner
 * and `HiltTestApplication`, neither of which exist in this project yet — an `@EntryPoint` declared in
 * `app/src/test` compiles but throws `ClassCastException` at the real, main-sourceSet-only generated
 * component, discovered while building this harness). Reading a ViewModel's own already-`@Inject`ed state
 * — see [io.github.chrisjmendoza.yearal.IntroOnceEndToEndTest] for `IntroGateViewModel.hasSeenIntro` — is
 * both simpler and closer to what the production code itself observes.
 */
internal const val DEFAULT_AWAIT_TIMEOUT_MILLIS = 10_000L

/**
 * Polls for a semantics node whose text matches [text] (see [androidx.compose.ui.test.hasText] for what
 * [substring] and [ignoreCase] mean), pumping Robolectric's paused main looper on every attempt.
 *
 * Bounded by [timeoutMillis]: fails with a message naming the missing text rather than hanging. Use this
 * instead of a single `onNodeWithText(...).assertIsDisplayed()` for anything that depends on
 * `Dispatchers.Main` work that may not have completed by the current frame — which, per the class KDoc,
 * is everything driven by a ViewModel in this app.
 */
internal fun ComposeTestRule.awaitNodeWithText(
    text: String,
    substring: Boolean = false,
    ignoreCase: Boolean = false,
    timeoutMillis: Long = DEFAULT_AWAIT_TIMEOUT_MILLIS,
) {
    waitUntil(timeoutMillis = timeoutMillis) {
        shadowOf(Looper.getMainLooper()).idle()
        onAllNodesWithText(text, substring = substring, ignoreCase = ignoreCase).fetchSemanticsNodes().isNotEmpty()
    }
}

/**
 * Pumps Robolectric's paused main looper until [value] returns a non-null result, bounded by
 * [timeoutMillis] of *real* elapsed time — the same idea as
 * [io.github.chrisjmendoza.yearal.TimeZoneChangeEndToEndTest]'s own `awaitOnMainLooper`, promoted here so
 * the next `:app` test that waits on a `Dispatchers.Main`-backed `StateFlow` does not need to redefine it,
 * but timed rather than counted: that test's chain is pure in-process coroutine hopping, where pumping
 * the looper as fast as possible is enough, but a `StateFlow` fed by real I/O (DataStore's own
 * `Dispatchers.IO` reads and writes, as [io.github.chrisjmendoza.yearal.IntroOnceEndToEndTest] waits on)
 * needs the background thread doing that I/O to actually get real CPU time between pumps — a tight loop
 * of bare `idle()` calls with nothing to drain can starve it and time out even though the write would
 * have landed a few milliseconds later. [Thread.sleep] between attempts costs nothing here since nothing
 * else is waiting on this thread.
 *
 * @throws IllegalStateException naming [what], if [value] is still `null` after [timeoutMillis].
 */
internal fun <T : Any> awaitOnMainLooper(
    what: String,
    timeoutMillis: Long = DEFAULT_AWAIT_TIMEOUT_MILLIS,
    value: () -> T?,
): T {
    // System.nanoTime, never currentTimeMillis (CLAUDE.md rule 2), and the right source for a deadline
    // anyway: monotonic, so a suite that deliberately moves the wall clock cannot skew this timeout.
    val deadline = System.nanoTime() + timeoutMillis * NANOS_PER_MILLI
    while (true) {
        shadowOf(Looper.getMainLooper()).idle()
        value()?.let { return it }
        if (System.nanoTime() >= deadline) {
            return value() ?: error("$what did not arrive within ${timeoutMillis}ms of main-looper pumps")
        }
        Thread.sleep(POLL_INTERVAL_MILLIS)
    }
}

/** How long [awaitOnMainLooper] sleeps between pump attempts once one has found nothing yet. */
private const val POLL_INTERVAL_MILLIS = 10L

private const val NANOS_PER_MILLI = 1_000_000L
