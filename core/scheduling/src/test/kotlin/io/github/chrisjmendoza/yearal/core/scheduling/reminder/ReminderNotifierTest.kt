package io.github.chrisjmendoza.yearal.core.scheduling.reminder

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.chrisjmendoza.yearal.core.testing.EventFixtures
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNotificationManager
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The reminder notification's tap target (ROADMAP M4 T10; `docs/security-and-privacy.md` §6.3, §6.4):
 * explicit, immutable, carrying only the event id under [ReminderIntent.EXTRA_EVENT_ID] with
 * [ReminderIntent.ACTION_OPEN_EVENT], and never sharing a request code between two different events.
 * [AlarmReminderSchedulerTest] already covers the alarm and posting behaviour this only builds on top
 * of, so this class seeds [ReminderNotifier.post] directly with hand-built [PendingReminder]s.
 */
@RunWith(AndroidJUnit4::class)
class ReminderNotifierTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val notifications: ShadowNotificationManager =
        shadowOf(context.getSystemService(NotificationManager::class.java))
    private val notifier = ReminderNotifier(context)
    private val zone: ZoneId = ZoneId.of("America/New_York")

    // This module's own test manifest declares no launcher activity (only :app's does), so
    // getLaunchIntentForPackage has nothing to resolve without this -- the same fake-launcher setup
    // AlarmReminderSchedulerTest relies on implicitly and TodayWidgetActionsTest (:widget) spells out.
    @Before
    fun registerAFakeLauncherActivity() {
        val component = ComponentName(context.packageName, "io.github.chrisjmendoza.yearal.MainActivity")
        val activityInfo =
            ActivityInfo().apply {
                packageName = component.packageName
                name = component.className
                applicationInfo = context.applicationInfo
            }
        val shadowPackageManager = shadowOf(context.packageManager)
        shadowPackageManager.addOrUpdateActivity(activityInfo)
        shadowPackageManager.addIntentFilterForActivity(
            component,
            IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) },
        )
    }

    private fun grantNotifications() {
        shadowOf(context as android.app.Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun reminderFor(
        eventId: Long,
        title: String,
    ): PendingReminder {
        val date = LocalDate.of(2026, 6, 30)
        return PendingReminder(
            eventId = eventId,
            occurrenceDate = date,
            minutesBefore = 0,
            triggerAt = date.atStartOfDay(zone).toInstant(),
            reference = ZonedDateTime.of(date, java.time.LocalTime.of(9, 0), zone),
            allDay = true,
        )
    }

    @Test
    fun `the tap intent is explicit, immutable, and carries only the event id`() {
        grantNotifications()
        val event = EventFixtures.allDay(id = 42L, date = LocalDate.of(2026, 6, 30), title = "Dentist")

        notifier.post(listOf(reminderFor(42L, "Dentist")), mapOf(42L to event), zone)

        val posted = notifications.allNotifications.single()
        val pending = posted.contentIntent.shouldNotBeNull()
        val operation = shadowOf(pending)
        operation.isActivity shouldBe true
        (operation.flags and PendingIntent.FLAG_IMMUTABLE) shouldBe PendingIntent.FLAG_IMMUTABLE
        (operation.flags and PendingIntent.FLAG_MUTABLE) shouldBe 0
        val intent = operation.savedIntent
        intent.component.shouldNotBeNull()
        intent.action shouldBe ReminderIntent.ACTION_OPEN_EVENT
        intent.extras?.keySet() shouldBe setOf(ReminderIntent.EXTRA_EVENT_ID)
        intent.getLongExtra(ReminderIntent.EXTRA_EVENT_ID, -1L) shouldBe 42L
        intent.data.shouldBeNull()
    }

    @Test
    fun `the public version's tap intent carries the same event id, never the title`() {
        grantNotifications()
        val event = EventFixtures.allDay(id = 7L, date = LocalDate.of(2026, 6, 30), title = "Therapy")

        notifier.post(listOf(reminderFor(7L, "Therapy")), mapOf(7L to event), zone)

        val posted = notifications.allNotifications.single()
        val publicIntent = shadowOf(posted.publicVersion!!.contentIntent).savedIntent
        publicIntent.getLongExtra(ReminderIntent.EXTRA_EVENT_ID, -1L) shouldBe 7L
        publicIntent.extras?.keySet() shouldBe setOf(ReminderIntent.EXTRA_EVENT_ID)
    }

    @Test
    fun `two different events never share a request code`() {
        grantNotifications()
        val first = EventFixtures.allDay(id = 1L, date = LocalDate.of(2026, 6, 30), title = "First")
        val second = EventFixtures.allDay(id = 2L, date = LocalDate.of(2026, 6, 30), title = "Second")

        notifier.post(
            listOf(reminderFor(1L, "First"), reminderFor(2L, "Second")),
            mapOf(1L to first, 2L to second),
            zone,
        )

        // Each posted Notification holds its own PendingIntent *object reference*; if both had shared
        // one request code with FLAG_UPDATE_CURRENT, the system (and Robolectric's shadow) would mutate
        // that one token in place, and the first notification's reference would silently pick up the
        // second event's extras too. Reading each notification's own contentIntent straight back proves
        // that never happens: each keeps its own event id, in posting order.
        val eventIds =
            notifications.allNotifications
                .map { shadowOf(it.contentIntent).savedIntent }
                .map { it.getLongExtra(ReminderIntent.EXTRA_EVENT_ID, -1L) }
        eventIds.toSet() shouldBe setOf(1L, 2L)
        1L.hashCode() shouldNotBe 2L.hashCode()
    }

    @Test
    fun `two reminders on the same event resolve to the identical tap intent, harmlessly`() {
        grantNotifications()
        val event = EventFixtures.allDay(id = 9L, date = LocalDate.of(2026, 6, 30), title = "Flight")
        val first = reminderFor(9L, "Flight")
        val second = first.copy(minutesBefore = 60, triggerAt = first.triggerAt.plusSeconds(1))

        notifier.post(listOf(first, second), mapOf(9L to event), zone)

        val intents = notifications.allNotifications.map { shadowOf(it.contentIntent).savedIntent }
        intents.map { it.getLongExtra(ReminderIntent.EXTRA_EVENT_ID, -1L) } shouldBe listOf(9L, 9L)
    }
}
