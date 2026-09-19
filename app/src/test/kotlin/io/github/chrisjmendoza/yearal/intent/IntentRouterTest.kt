package io.github.chrisjmendoza.yearal.intent

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.chrisjmendoza.yearal.core.scheduling.reminder.ReminderIntent
import io.github.chrisjmendoza.yearal.widget.WidgetIntents
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [IntentRouter.resolve] is the validation seam `docs/security-and-privacy.md` §6.3 requires for the
 * launcher activity: an unrecognized action, a missing or malformed extra, or an epoch day outside
 * `:core:calendar`'s supported years must all fail soft to [AppRoute.Default], never crash, and never
 * follow a URI, a class name or a nested `Intent` smuggled under a known extra's name. Robolectric only
 * for a real `android.content.Intent`; [IntentRouter] itself has no other Android dependency.
 */
@RunWith(AndroidJUnit4::class)
class IntentRouterTest {
    // -- Recognized routes ---------------------------------------------------------------------------

    @Test
    fun `the Today widget's intent routes to Today`() {
        val intent = Intent(WidgetIntents.ACTION_OPEN_TODAY)

        IntentRouter.resolve(intent) shouldBe AppRoute.Today
    }

    @Test
    fun `the Month widget's whole-widget intent routes to the current month`() {
        val intent = Intent(WidgetIntents.ACTION_OPEN_MONTH)

        IntentRouter.resolve(intent) shouldBe AppRoute.CurrentMonth
    }

    @Test
    fun `a day cell's intent routes to that day`() {
        val intent =
            Intent(WidgetIntents.ACTION_OPEN_DAY)
                .putExtra(WidgetIntents.EXTRA_EPOCH_DAY, 20_713L)

        IntentRouter.resolve(intent) shouldBe AppRoute.Day(20_713L)
    }

    @Test
    fun `a reminder's intent routes to that event`() {
        val intent =
            Intent(ReminderIntent.ACTION_OPEN_EVENT)
                .putExtra(ReminderIntent.EXTRA_EVENT_ID, 42L)

        IntentRouter.resolve(intent) shouldBe AppRoute.EventDetail(42L)
    }

    // -- Malformed or absent action -------------------------------------------------------------------

    @Test
    fun `a null action falls back to the default route`() {
        IntentRouter.resolve(Intent()) shouldBe AppRoute.Default
    }

    @Test
    fun `the launcher's own MAIN action falls back to the default route`() {
        IntentRouter.resolve(Intent(Intent.ACTION_MAIN)) shouldBe AppRoute.Default
    }

    @Test
    fun `an unrecognized custom action falls back to the default route`() {
        IntentRouter.resolve(Intent("com.example.SOMETHING_ELSE")) shouldBe AppRoute.Default
    }

    // -- Out-of-range and missing epoch days ------------------------------------------------------------

    @Test
    fun `a day intent with no epoch-day extra falls back to the default route`() {
        IntentRouter.resolve(Intent(WidgetIntents.ACTION_OPEN_DAY)) shouldBe AppRoute.Default
    }

    @Test
    fun `Long-MIN_VALUE fails LocalDate conversion and falls back to the default route`() {
        val intent = Intent(WidgetIntents.ACTION_OPEN_DAY).putExtra(WidgetIntents.EXTRA_EPOCH_DAY, Long.MIN_VALUE)

        IntentRouter.resolve(intent) shouldBe AppRoute.Default
    }

    @Test
    fun `Long-MAX_VALUE fails LocalDate conversion and falls back to the default route`() {
        val intent = Intent(WidgetIntents.ACTION_OPEN_DAY).putExtra(WidgetIntents.EXTRA_EPOCH_DAY, Long.MAX_VALUE)

        IntentRouter.resolve(intent) shouldBe AppRoute.Default
    }

    @Test
    fun `year 0 parses as a date but fails the supported-year check`() {
        // LocalDate.ofEpochDay succeeds for year 0 (proleptic ISO); IfcDate.MIN_YEAR is 1.
        val epochDay =
            java.time.LocalDate
                .of(0, 6, 15)
                .toEpochDay()
        val intent = Intent(WidgetIntents.ACTION_OPEN_DAY).putExtra(WidgetIntents.EXTRA_EPOCH_DAY, epochDay)

        IntentRouter.resolve(intent) shouldBe AppRoute.Default
    }

    @Test
    fun `year 10000 parses as a date but fails the supported-year check`() {
        // IfcDate.MAX_YEAR is 9999.
        val epochDay =
            java.time.LocalDate
                .of(10_000, 1, 1)
                .toEpochDay()
        val intent = Intent(WidgetIntents.ACTION_OPEN_DAY).putExtra(WidgetIntents.EXTRA_EPOCH_DAY, epochDay)

        IntentRouter.resolve(intent) shouldBe AppRoute.Default
    }

    @Test
    fun `year 9999 is the last supported year and still routes`() {
        val epochDay =
            java.time.LocalDate
                .of(9999, 12, 31)
                .toEpochDay()
        val intent = Intent(WidgetIntents.ACTION_OPEN_DAY).putExtra(WidgetIntents.EXTRA_EPOCH_DAY, epochDay)

        IntentRouter.resolve(intent) shouldBe AppRoute.Day(epochDay)
    }

    // -- Unknown or invalid event ids --------------------------------------------------------------

    @Test
    fun `an event intent with no id extra falls back to the default route`() {
        IntentRouter.resolve(Intent(ReminderIntent.ACTION_OPEN_EVENT)) shouldBe AppRoute.Default
    }

    @Test
    fun `a zero or negative event id falls back to the default route`() {
        val zero = Intent(ReminderIntent.ACTION_OPEN_EVENT).putExtra(ReminderIntent.EXTRA_EVENT_ID, 0L)
        val negative = Intent(ReminderIntent.ACTION_OPEN_EVENT).putExtra(ReminderIntent.EXTRA_EVENT_ID, -1L)

        IntentRouter.resolve(zero) shouldBe AppRoute.Default
        IntentRouter.resolve(negative) shouldBe AppRoute.Default
    }

    @Test
    fun `an id that structurally could exist but does not is still routed -- the editor shows not-found`() {
        // IntentRouter cannot check existence (CLAUDE.md rule 8: ids only, no database read here);
        // EventEditorViewModel's own "not found" state is what actually handles a deleted event's id.
        val intent = Intent(ReminderIntent.ACTION_OPEN_EVENT).putExtra(ReminderIntent.EXTRA_EVENT_ID, 999_999L)

        IntentRouter.resolve(intent) shouldBe AppRoute.EventDetail(999_999L)
    }

    // -- No intent redirection: a URI, file path, class name or nested Intent is never followed -----

    @Test
    fun `a URI under the epoch-day extra's own name is ignored, not followed`() {
        val intent =
            Intent(WidgetIntents.ACTION_OPEN_DAY)
                .putExtra(WidgetIntents.EXTRA_EPOCH_DAY, android.net.Uri.parse("content://evil/1"))

        // The stored value is a Uri, not a Long: longExtraOrNull's `as? Long` cast fails, which this
        // router treats exactly like the extra being absent -- never coerced into a number and routed.
        IntentRouter.resolve(intent) shouldBe AppRoute.Default
    }

    @Test
    fun `a nested Intent under the event-id extra's own name is ignored, not followed`() {
        val nested = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("content://evil/1"))
        val intent =
            Intent(ReminderIntent.ACTION_OPEN_EVENT)
                .putExtra(ReminderIntent.EXTRA_EVENT_ID, nested)

        // Same as the Uri case: a nested Intent is not a Long, so it is never read, launched, or even
        // type-checked beyond the failed cast.
        IntentRouter.resolve(intent) shouldBe AppRoute.Default
    }

    @Test
    fun `an intent carrying a class-name extra under an unrelated key is never read`() {
        val intent =
            Intent(WidgetIntents.ACTION_OPEN_DAY)
                .putExtra(WidgetIntents.EXTRA_EPOCH_DAY, 1L)
                .putExtra("componentClassName", "io.github.chrisjmendoza.yearal.SomeOtherActivity")

        // IntentRouter reads only the two named Long extras; an extra under any other key, of any
        // type, is simply never looked at.
        IntentRouter.resolve(intent) shouldBe AppRoute.Day(1L)
    }

    @Test
    fun `an intent's own data URI is never read`() {
        val intent =
            Intent(WidgetIntents.ACTION_OPEN_DAY, android.net.Uri.parse("content://evil/1"))
                .putExtra(WidgetIntents.EXTRA_EPOCH_DAY, 5L)

        IntentRouter.resolve(intent) shouldBe AppRoute.Day(5L)
    }
}
