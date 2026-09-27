package io.github.chrisjmendoza.yearal.core.scheduling.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.chrisjmendoza.yearal.core.domain.event.DefaultRecurrenceExpander
import io.github.chrisjmendoza.yearal.core.domain.event.Event
import io.github.chrisjmendoza.yearal.core.domain.event.EventTiming
import io.github.chrisjmendoza.yearal.core.domain.event.Reminder
import io.github.chrisjmendoza.yearal.core.testing.EventFixtures
import io.github.chrisjmendoza.yearal.core.testing.FakeEventRepository
import io.github.chrisjmendoza.yearal.core.testing.FakeZoneProvider
import io.github.chrisjmendoza.yearal.core.testing.MutableClock
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowNotificationManager
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Provider

/**
 * The Snooze and Done notification actions (ROADMAP M6 T4; FEATURES E11's snooze half):
 * [AlarmReminderScheduler.snooze], [AlarmReminderScheduler.dismiss] and
 * [AlarmReminderScheduler.fireSnooze], the second, independent alarm a snooze arms through the same
 * [io.github.chrisjmendoza.yearal.core.scheduling.armWakeup] path as the single next-alarm, and its
 * survival across a reboot via [SnoozeStore].
 *
 * As in [AlarmReminderSchedulerTest], the real [DefaultRecurrenceExpander] and a real [ReminderNotifier]
 * are used; only the clock, zone and event store are fakes.
 */
@RunWith(AndroidJUnit4::class)
class ReminderSnoozeTest {
    @get:Rule
    val timeout: Timeout = Timeout.seconds(60)

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val application: Application = context as Application
    private val alarms: ShadowAlarmManager = shadowOf(context.getSystemService(AlarmManager::class.java))
    private val notifications: ShadowNotificationManager =
        shadowOf(context.getSystemService(NotificationManager::class.java))

    private val clock = MutableClock(Instant.parse("2026-06-29T12:00:00Z"))
    private val zones = FakeZoneProvider(ZoneId.of("America/New_York"))
    private val repository = FakeEventRepository(clock)
    private val notifier = ReminderNotifier(context)
    private val snoozeStore = SnoozeStore(context)
    private val scheduler = newScheduler()

    private fun newScheduler() =
        AlarmReminderScheduler(
            context = context,
            repository = Provider { repository },
            expander = DefaultRecurrenceExpander(),
            clock = clock,
            zoneProvider = zones,
            notifier = notifier,
            snoozeStore = snoozeStore,
        )

    @After
    fun tearDown() {
        ShadowAlarmManager.reset()
    }

    private fun grantNotifications() {
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    /** An all-day event on [date] with a single 0-minutes-before reminder, stored and returned. */
    private fun seedAllDay(
        date: LocalDate,
        title: String = "Dentist",
    ) = runBlocking {
        val id =
            repository.upsertEvent(EventFixtures.allDay(date = date, title = title, reminders = setOf(Reminder(0))))
        checkNotNull(repository.getEvent(id))
    }

    private fun onlyAlarm(): ShadowAlarmManager.ScheduledAlarm = alarms.scheduledAlarms.shouldHaveSize(1).single()

    private fun onlyAlarmTime(): Instant = Instant.ofEpochMilli(onlyAlarm().triggerAtMs)

    /** Posts a bare notification directly, standing in for one [ReminderNotifier] already posted. */
    private fun postDummyNotification(notificationId: Int) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(ReminderNotifier.CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_HIGH),
        )
        manager.notify(notificationId, Notification.Builder(context, ReminderNotifier.CHANNEL_ID).build())
    }

    // ---------------------------------------------------------------------------------------------
    // Snooze
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `snooze cancels the current notification and arms an alarm 10 minutes from the clock`() {
        grantNotifications()
        val event = seedAllDay(LocalDate.of(2026, 6, 29))
        val date = LocalDate.of(2026, 6, 29)
        val notificationId = ReminderNotifier.notificationId(pendingReminder(event.id, date))
        postDummyNotification(notificationId)

        scheduler.snooze(event.id, date.toEpochDay(), notificationId)

        notifications.getNotification(notificationId).shouldBeNull()
        onlyAlarmTime() shouldBe clock.instant().plus(AlarmReminderScheduler.SNOOZE_DURATION)
    }

    @Test
    fun `snooze does not touch the single next-alarm reminder request code`() {
        val event = seedAllDay(LocalDate.of(2026, 6, 30))
        val date = LocalDate.of(2026, 6, 30)
        clock.set(Instant.parse("2026-06-29T12:00:00Z"))
        runBlocking { scheduler.reschedule() } // arms the regular next-alarm
        alarms.scheduledAlarms.shouldHaveSize(1)

        val notificationId = ReminderNotifier.notificationId(pendingReminder(event.id, date))
        scheduler.snooze(event.id, date.toEpochDay(), notificationId)

        // Both the regular next-alarm and the new snooze-fire alarm are armed, distinctly.
        alarms.scheduledAlarms.shouldHaveSize(2)
    }

    // ---------------------------------------------------------------------------------------------
    // Done
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `done cancels the notification and schedules nothing`() {
        grantNotifications()
        val notificationId = 4242
        postDummyNotification(notificationId)

        scheduler.dismiss(notificationId)

        notifications.getNotification(notificationId).shouldBeNull()
        alarms.scheduledAlarms.shouldBeEmpty()
    }

    @Test
    fun `done after a snooze also cancels the pending snooze alarm`() {
        val event = seedAllDay(LocalDate.of(2026, 6, 29))
        val date = LocalDate.of(2026, 6, 29)
        val notificationId = ReminderNotifier.notificationId(pendingReminder(event.id, date))
        scheduler.snooze(event.id, date.toEpochDay(), notificationId)
        alarms.scheduledAlarms.shouldHaveSize(1)

        scheduler.dismiss(notificationId)

        alarms.scheduledAlarms.shouldBeEmpty()
    }

    // ---------------------------------------------------------------------------------------------
    // Firing a snoozed reminder
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `a snoozed reminder that fires is posted again, with both actions, under the same id`() {
        grantNotifications()
        val event = seedAllDay(LocalDate.of(2026, 6, 29), title = "Therapy")
        val date = LocalDate.of(2026, 6, 29)
        val notificationId = ReminderNotifier.notificationId(pendingReminder(event.id, date))
        scheduler.snooze(event.id, date.toEpochDay(), notificationId)
        clock.set(clock.instant().plus(AlarmReminderScheduler.SNOOZE_DURATION))

        runBlocking { scheduler.fireSnooze(event.id, date.toEpochDay(), notificationId) }

        val posted = notifications.getNotification(notificationId).shouldNotBeNull()
        posted.extras.getString(Notification.EXTRA_TITLE) shouldBe "Therapy"
        posted.actions.orEmpty() shouldHaveSize 2
        alarms.scheduledAlarms.shouldBeEmpty() // the one-shot snooze alarm is gone, and nothing else was pending
    }

    @Test
    fun `firing a snoozed reminder clears its persisted entry`() {
        val event = seedAllDay(LocalDate.of(2026, 6, 29))
        val date = LocalDate.of(2026, 6, 29)
        val notificationId = ReminderNotifier.notificationId(pendingReminder(event.id, date))
        scheduler.snooze(event.id, date.toEpochDay(), notificationId)
        snoozeStore.all() shouldHaveSize 1

        runBlocking { scheduler.fireSnooze(event.id, date.toEpochDay(), notificationId) }

        snoozeStore.all().shouldBeEmpty()
    }

    @Test
    fun `a snooze whose event was deleted in the meantime fires nothing, harmlessly`() {
        val event = seedAllDay(LocalDate.of(2026, 6, 29))
        val date = LocalDate.of(2026, 6, 29)
        val notificationId = ReminderNotifier.notificationId(pendingReminder(event.id, date))
        scheduler.snooze(event.id, date.toEpochDay(), notificationId)
        runBlocking { repository.deleteEvent(event.id) }

        runBlocking { scheduler.fireSnooze(event.id, date.toEpochDay(), notificationId) }

        notifications.allNotifications.shouldBeEmpty()
    }

    @Test
    fun `snoozing at 23-55 and firing after midnight still describes the snoozed occurrence`() {
        grantNotifications()
        // A one-off timed event at 23:30 EDT on 29 June: its reminder tap is snoozed 5 minutes before
        // local midnight, and fires 10 minutes later, at 00:05 EDT on 30 June.
        val date = LocalDate.of(2026, 6, 29)
        val event =
            runBlocking {
                val id =
                    repository.upsertEvent(
                        Event(
                            uid = "late-night-snooze",
                            title = "Late-night snooze",
                            timing = EventTiming.Timed(date, 23 * 60 + 30, 30),
                            reminders = setOf(Reminder(0)),
                        ),
                    )
                checkNotNull(repository.getEvent(id))
            }
        val notificationId = ReminderNotifier.notificationId(pendingReminder(event.id, date))
        clock.set(Instant.parse("2026-06-30T03:55:00Z")) // 23:55 EDT on 29 June
        scheduler.snooze(event.id, date.toEpochDay(), notificationId)
        onlyAlarmTime() shouldBe Instant.parse("2026-06-30T04:05:00Z") // 00:05 EDT on 30 June

        clock.set(onlyAlarmTime())
        runBlocking { scheduler.fireSnooze(event.id, date.toEpochDay(), notificationId) }

        val posted = notifications.getNotification(notificationId).shouldNotBeNull()
        // Still 29 June's 23:30 start, not a "today" recomputed after crossing local midnight.
        posted.shortTime() shouldBe "11:30 PM"
    }

    /** The notification's text with the narrow no-break space of the localized time normalized. */
    private fun Notification.shortTime(): String? =
        extras.getString(Notification.EXTRA_TEXT)?.replace(' ', ' ')?.replace(' ', ' ')

    // ---------------------------------------------------------------------------------------------
    // Request codes (ROADMAP M6 T4; docs/security-and-privacy.md §6.4)
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `snooze-fire request codes differ across notifications, and from the button actions`() {
        val notificationIdA = ReminderNotifier.notificationId(pendingReminder(1L, LocalDate.of(2026, 6, 29)))
        val notificationIdB = ReminderNotifier.notificationId(pendingReminder(2L, LocalDate.of(2026, 6, 30)))

        val codes =
            setOf(
                ReminderActionIntent.snoozeRequestCode(notificationIdA),
                ReminderActionIntent.doneRequestCode(notificationIdA),
                ReminderActionIntent.snoozeFireRequestCode(notificationIdA),
                ReminderActionIntent.snoozeFireRequestCode(notificationIdB),
            )

        codes shouldHaveSize 4
    }

    @Test
    fun `the snooze-fire alarm targets ReminderActionReceiver, distinct from the reminder alarm's component`() {
        val event = seedAllDay(LocalDate.of(2026, 6, 29))
        val date = LocalDate.of(2026, 6, 29)
        val notificationId = ReminderNotifier.notificationId(pendingReminder(event.id, date))
        val lookupFlags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        val expectedIntent =
            Intent(context, ReminderActionReceiver::class.java).setAction(ReminderActionIntent.ACTION_SNOOZE_FIRE)
        val requestCode = ReminderActionIntent.snoozeFireRequestCode(notificationId)
        PendingIntent.getBroadcast(context, requestCode, expectedIntent, lookupFlags).shouldBeNull()

        scheduler.snooze(event.id, date.toEpochDay(), notificationId)

        onlyAlarm().getType() shouldBe AlarmManager.RTC_WAKEUP
        val registered =
            PendingIntent.getBroadcast(context, requestCode, expectedIntent, lookupFlags).shouldNotBeNull()
        val operation = shadowOf(registered)
        operation.isBroadcast shouldBe true
        (operation.flags and PendingIntent.FLAG_IMMUTABLE) shouldBe PendingIntent.FLAG_IMMUTABLE
        (operation.flags and PendingIntent.FLAG_MUTABLE) shouldBe 0
        val intent = operation.savedIntent
        intent.component shouldBe ComponentName(context, ReminderActionReceiver::class.java)
        intent.action shouldBe ReminderActionIntent.ACTION_SNOOZE_FIRE
        intent.getLongExtra(ReminderActionIntent.EXTRA_EVENT_ID, -1L) shouldBe event.id
        intent.getLongExtra(ReminderActionIntent.EXTRA_OCCURRENCE_EPOCH_DAY, -1L) shouldBe date.toEpochDay()
        intent.getIntExtra(ReminderActionIntent.EXTRA_NOTIFICATION_ID, 0) shouldBe notificationId
    }

    // ---------------------------------------------------------------------------------------------
    // Reboot survival
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `a snooze still ahead survives a reboot, re-armed by a fresh scheduler instance`() {
        val event = seedAllDay(LocalDate.of(2026, 6, 29))
        val date = LocalDate.of(2026, 6, 29)
        val notificationId = ReminderNotifier.notificationId(pendingReminder(event.id, date))
        scheduler.snooze(event.id, date.toEpochDay(), notificationId)
        val fireAt = onlyAlarmTime()
        val snoozeFireIntent =
            Intent(context, ReminderActionReceiver::class.java).setAction(ReminderActionIntent.ACTION_SNOOZE_FIRE)
        val fireIntent =
            PendingIntent
                .getBroadcast(
                    context,
                    ReminderActionIntent.snoozeFireRequestCode(notificationId),
                    snoozeFireIntent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
                ).shouldNotBeNull()
        // Cancel it directly, standing in for a reboot dropping every AlarmManager alarm: a real
        // device cancels the alarm, but the identity a getBroadcast lookup returns is Android's own
        // PendingIntent registry, which a Robolectric-simulated reboot does not clear either.
        context.getSystemService(AlarmManager::class.java).cancel(fireIntent)
        triggerTimeOf(fireIntent).shouldBeNull()

        // A new process builds a fresh scheduler instance, but the same SharedPreferences-backed
        // SnoozeStore — recomputing from it is what re-arms the snooze.
        val rebooted = newScheduler()
        runBlocking { rebooted.reschedule() }

        triggerTimeOf(fireIntent) shouldBe fireAt
    }

    /** The instant [pendingIntent] is armed at, or `null` if it is not currently scheduled. */
    @Suppress("DEPRECATION") // ShadowAlarmManager.ScheduledAlarm.operation has no non-deprecated equivalent.
    private fun triggerTimeOf(pendingIntent: PendingIntent): Instant? =
        alarms.scheduledAlarms.firstOrNull { it.operation == pendingIntent }?.let {
            Instant.ofEpochMilli(
                it.triggerAtMs,
            )
        }

    @Test
    fun `a snooze overdue by less than the grace window fires immediately on the next recompute`() {
        grantNotifications()
        val event = seedAllDay(LocalDate.of(2026, 6, 29), title = "Overdue")
        val date = LocalDate.of(2026, 6, 29)
        val notificationId = ReminderNotifier.notificationId(pendingReminder(event.id, date))
        scheduler.snooze(event.id, date.toEpochDay(), notificationId)

        // As after a reboot that took a few minutes: a fresh process, and the clock has moved past
        // the snoozed instant, but still inside AlarmReminderScheduler.LATE_GRACE.
        clock.set(clock.instant().plus(AlarmReminderScheduler.SNOOZE_DURATION).plus(Duration.ofMinutes(5)))
        val rebooted = newScheduler()

        runBlocking { rebooted.reschedule() }

        notifications.getNotification(notificationId).shouldNotBeNull()
    }

    @Test
    fun `a snooze overdue beyond the grace window is dropped silently`() {
        val event = seedAllDay(LocalDate.of(2026, 6, 29))
        val date = LocalDate.of(2026, 6, 29)
        val notificationId = ReminderNotifier.notificationId(pendingReminder(event.id, date))
        scheduler.snooze(event.id, date.toEpochDay(), notificationId)

        clock.set(
            clock
                .instant()
                .plus(
                    AlarmReminderScheduler.SNOOZE_DURATION,
                ).plus(AlarmReminderScheduler.LATE_GRACE)
                .plusSeconds(1),
        )
        val rebooted = newScheduler()

        runBlocking { rebooted.reschedule() }

        notifications.allNotifications.shouldBeEmpty()
        snoozeStore.all().shouldBeEmpty()
    }

    /** A hand-built [PendingReminder] just to compute the notification id [ReminderNotifier] would use. */
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
