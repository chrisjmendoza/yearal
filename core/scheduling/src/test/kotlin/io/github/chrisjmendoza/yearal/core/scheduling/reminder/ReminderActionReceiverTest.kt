package io.github.chrisjmendoza.yearal.core.scheduling.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.chrisjmendoza.yearal.core.domain.event.DefaultRecurrenceExpander
import io.github.chrisjmendoza.yearal.core.domain.event.Reminder
import io.github.chrisjmendoza.yearal.core.scheduling.DayRolloverNotifier
import io.github.chrisjmendoza.yearal.core.scheduling.DayRolloverScheduler
import io.github.chrisjmendoza.yearal.core.scheduling.FakeSchedulingApplication
import io.github.chrisjmendoza.yearal.core.scheduling.FakeSchedulingComponent
import io.github.chrisjmendoza.yearal.core.scheduling.RolloverBroadcastHandler
import io.github.chrisjmendoza.yearal.core.testing.EventFixtures
import io.github.chrisjmendoza.yearal.core.testing.FakeEventRepository
import io.github.chrisjmendoza.yearal.core.testing.FakeZoneProvider
import io.github.chrisjmendoza.yearal.core.testing.MutableClock
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowNotificationManager
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Provider

/**
 * The Snooze/Done broadcast wiring end to end through [ReminderActionReceiver], registered from this
 * module's manifest and resolved through the real Hilt entry-point lookup (ROADMAP M6 T4), the
 * counterpart of [ReminderReceiversTest] for the two notification actions and the snooze-fire alarm.
 *
 * A real [AlarmReminderScheduler] is used — unlike [ReminderReceiversTest]'s counting fake — because
 * `snooze`/`dismiss`/`fireSnooze` are not part of the frozen [io.github.chrisjmendoza.yearal.core.domain.event.ReminderScheduler]
 * contract a fake could stand in for; its own behaviour is [ReminderSnoozeTest]'s.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(application = FakeSchedulingApplication::class)
class ReminderActionReceiverTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val application: Application = context as Application
    private val alarms: ShadowAlarmManager = shadowOf(context.getSystemService(AlarmManager::class.java))
    private val notifications: ShadowNotificationManager =
        shadowOf(context.getSystemService(NotificationManager::class.java))

    private val clock = MutableClock(Instant.parse("2026-06-29T12:00:00Z"))
    private val zones = FakeZoneProvider(ZoneId.of("America/New_York"))
    private val repository = FakeEventRepository(clock)
    private val snoozeStore = SnoozeStore(context)
    private val scheduler =
        AlarmReminderScheduler(
            context = context,
            repository = Provider { repository },
            expander = DefaultRecurrenceExpander(),
            clock = clock,
            zoneProvider = zones,
            notifier = ReminderNotifier(context),
            snoozeStore = snoozeStore,
        )

    @Before
    fun installGraph() {
        val scope = CoroutineScope(UnconfinedTestDispatcher(TestCoroutineScheduler()))
        (context as FakeSchedulingApplication).component =
            FakeSchedulingComponent(
                // The rollover path is never exercised here; a real, harmless handler fills the
                // required slot the same way ReminderReceiversTest fills reminderHandler.
                handler =
                    RolloverBroadcastHandler(
                        DayRolloverScheduler(context, clock, zones),
                        DayRolloverNotifier(emptySet()),
                        scope,
                    ),
                reminderActionHandler = ReminderActionHandler(scheduler, scope),
            )
    }

    @After
    fun tearDown() {
        ShadowAlarmManager.reset()
    }

    private fun grantNotifications() {
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun seedAllDay(date: LocalDate) =
        runBlocking {
            val id =
                repository.upsertEvent(
                    EventFixtures.allDay(date = date, title = "Dentist", reminders = setOf(Reminder(0))),
                )
            checkNotNull(repository.getEvent(id))
        }

    private fun sendAction(
        action: String,
        eventId: Long? = null,
        occurrenceEpochDay: Long? = null,
        notificationId: Int? = null,
    ) {
        val intent = Intent(context, ReminderActionReceiver::class.java).setAction(action)
        eventId?.let { intent.putExtra(ReminderActionIntent.EXTRA_EVENT_ID, it) }
        occurrenceEpochDay?.let { intent.putExtra(ReminderActionIntent.EXTRA_OCCURRENCE_EPOCH_DAY, it) }
        notificationId?.let { intent.putExtra(ReminderActionIntent.EXTRA_NOTIFICATION_ID, it) }
        context.sendBroadcast(intent)
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `a Snooze broadcast arms the snooze-fire alarm and cancels the notification`() {
        grantNotifications()
        val event = seedAllDay(LocalDate.of(2026, 6, 29))
        val date = LocalDate.of(2026, 6, 29)
        val notificationId = ReminderNotifier.notificationId(pendingReminder(event.id, date))

        sendAction(ReminderActionIntent.ACTION_SNOOZE, event.id, date.toEpochDay(), notificationId)

        alarms.scheduledAlarms.shouldHaveSize(1)
        notifications.getNotification(notificationId).shouldBeNull()
    }

    @Test
    fun `a Done broadcast cancels the notification and arms nothing`() {
        val notificationId = 99
        sendAction(ReminderActionIntent.ACTION_DONE, notificationId = notificationId)

        alarms.scheduledAlarms.shouldBeEmpty()
    }

    @Test
    fun `a Done broadcast with no notification id extra is ignored`() {
        sendAction(ReminderActionIntent.ACTION_DONE)

        alarms.scheduledAlarms.shouldBeEmpty()
    }

    @Test
    fun `a Snooze broadcast missing the event id is ignored`() {
        sendAction(ReminderActionIntent.ACTION_SNOOZE, occurrenceEpochDay = 1L, notificationId = 5)

        alarms.scheduledAlarms.shouldBeEmpty()
        snoozeStore.all().shouldBeEmpty()
    }

    @Test
    fun `the snooze-fire alarm broadcast re-posts the notification`() {
        grantNotifications()
        val event = seedAllDay(LocalDate.of(2026, 6, 29))
        val date = LocalDate.of(2026, 6, 29)
        val notificationId = ReminderNotifier.notificationId(pendingReminder(event.id, date))
        sendAction(ReminderActionIntent.ACTION_SNOOZE, event.id, date.toEpochDay(), notificationId)
        clock.set(clock.instant().plus(AlarmReminderScheduler.SNOOZE_DURATION))

        sendAction(ReminderActionIntent.ACTION_SNOOZE_FIRE, event.id, date.toEpochDay(), notificationId)

        notifications.getNotification(notificationId).shouldNotBeNull()
    }

    @Test
    fun `an unrecognized action is ignored`() {
        sendAction("not.a.real.action", 1L, 1L, 1)

        alarms.scheduledAlarms.shouldBeEmpty()
        notifications.allNotifications.shouldBeEmpty()
    }

    private fun pendingReminder(
        eventId: Long,
        date: LocalDate,
    ): PendingReminder =
        PendingReminder(
            eventId = eventId,
            occurrenceDate = date,
            minutesBefore = 0,
            triggerAt = Instant.MIN,
            reference = date.atStartOfDay(zones.currentZone()),
            allDay = true,
        )
}
