package io.github.chrisjmendoza.yearal.core.scheduling.reminder

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.chrisjmendoza.yearal.core.domain.event.Event
import io.github.chrisjmendoza.yearal.core.scheduling.R
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts reminder notifications (FEATURES E4) the way `docs/security-and-privacy.md` §3.3 requires:
 * one channel, `VISIBILITY_PRIVATE` with a **redacted public version** so a lock screen that hides
 * sensitive content shows "Event reminder" and a time and no event text (FEATURES P3), no
 * full-screen intent, and a tap that opens the app through an explicit, immutable `PendingIntent`
 * that carries only the event id (§6.4; CLAUDE.md rule 8) — `:app`'s `IntentRouter` (ROADMAP M4 T10)
 * reads [ReminderIntent.ACTION_OPEN_EVENT] and [ReminderIntent.EXTRA_EVENT_ID] to open that event's
 * own editor instead of the normal start destination.
 *
 * **Snooze and Done** (ROADMAP M6 T4; FEATURES E11) are two more explicit, immutable broadcasts to
 * [ReminderActionReceiver], added to both the private and the public copy — the security doc's own
 * table says an action button "reveal[s] nothing" and is allowed without unlocking, unlike the "Open
 * event" content tap. Each carries only [ReminderActionIntent.EXTRA_EVENT_ID],
 * [ReminderActionIntent.EXTRA_OCCURRENCE_EPOCH_DAY] and [ReminderActionIntent.EXTRA_NOTIFICATION_ID];
 * [AlarmReminderScheduler] is what turns a Snooze tap into a delayed re-post of this same notification.
 *
 * **Notifications are optional, not required.** On API 33 and later, posting without
 * `POST_NOTIFICATIONS` is a no-op: nothing is posted, nothing is logged and nothing throws, and the
 * caller still re-arms its alarm, so the app keeps working exactly as before when the user says no
 * (FEATURES P2). The in-context permission request belongs to the event editor, not here.
 */
@Singleton
internal class ReminderNotifier
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        /**
         * Posts one notification per entry of [due], reading the title from [events].
         *
         * Entries whose event is not in [events] are skipped (it was deleted between the query and
         * the delivery). Ids are stable, so posting the same reminder twice replaces the notification
         * instead of adding one.
         *
         * @param due the reminders whose time has come, in delivery order.
         * @param events the events of [due], by id, as loaded by this same recomputation.
         * @param deviceZone the zone times are shown in.
         */
        fun post(
            due: List<PendingReminder>,
            events: Map<Long, Event>,
            deviceZone: ZoneId,
        ) {
            if (due.isEmpty()) return
            // API 33+ makes notifications a runtime permission. The check is inline so that Android
            // Lint's MissingPermission can see it guarding the notify() call below.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            val manager = NotificationManagerCompat.from(context)
            manager.createNotificationChannel(channel())
            for (reminder in due) {
                val event = events[reminder.eventId] ?: continue
                manager.notify(notificationId(reminder), build(notificationId(reminder), reminder, event, deviceZone))
            }
        }

        /**
         * Re-posts one reminder's notification under an explicit [notificationId] — the same id it
         * had before — rather than one recomputed from [reminder] (ROADMAP M6 T4). Used only when a
         * snoozed reminder's delayed alarm fires: [AlarmReminderScheduler] rebuilds a [PendingReminder]
         * whose `minutesBefore` is a placeholder, because the button that started the snooze carries
         * no lead time (ids only, CLAUDE.md rule 8) — harmless, since [build] never reads it. Same
         * permission guard, same channel, same redaction and the same Snooze/Done actions as [post].
         */
        fun repost(
            notificationId: Int,
            reminder: PendingReminder,
            event: Event,
            deviceZone: ZoneId,
        ) {
            // Inline for the same Lint reason as the check in post().
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            val manager = NotificationManagerCompat.from(context)
            manager.createNotificationChannel(channel())
            manager.notify(notificationId, build(notificationId, reminder, event, deviceZone))
        }

        /** Cancels the notification [notificationId], if one is showing. A no-op otherwise. */
        fun cancel(notificationId: Int) {
            NotificationManagerCompat.from(context).cancel(notificationId)
        }

        /**
         * The channel every reminder is posted on, rebuilt and handed to the platform on each post.
         * `createNotificationChannel` is idempotent — creating an existing channel updates its name
         * and description and never resets what the user changed (importance, sound).
         *
         * `IMPORTANCE_HIGH` so a reminder can be a heads-up notification, which is what a calendar
         * reminder is for. That is a normal high-importance notification, **not** a full-screen
         * intent: Play reserves those for alarm and calling apps
         * (`docs/security-and-privacy.md` §3.3).
         */
        private fun channel(): NotificationChannelCompat =
            NotificationChannelCompat
                .Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(context.getString(R.string.reminder_channel_name))
                .setDescription(context.getString(R.string.reminder_channel_description))
                .build()

        /** The notification for one reminder: title and time only, never the description (§3.3). */
        private fun build(
            notificationId: Int,
            reminder: PendingReminder,
            event: Event,
            deviceZone: ZoneId,
        ): Notification {
            val time = timeText(reminder, deviceZone)
            return baseBuilder(event.id, reminder.occurrenceDate, notificationId)
                .setContentTitle(event.title.ifBlank { context.getString(R.string.reminder_untitled_event) })
                .setContentText(time)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(publicVersion(time, event.id, reminder.occurrenceDate, notificationId))
                .setAutoCancel(true)
                .build()
        }

        /**
         * The redacted copy the system shows instead of the real one when the lock screen hides
         * sensitive content: a fixed localized label and the time, and **no title, notes or
         * location** (FEATURES P3; `docs/security-and-privacy.md` §3.3). Carries the same Snooze/Done
         * actions as the private copy — `docs/security-and-privacy.md` §3.3's own table: an action
         * button "reveal[s] nothing", unlike the content tap.
         */
        private fun publicVersion(
            time: String,
            eventId: Long,
            occurrenceDate: LocalDate,
            notificationId: Int,
        ): Notification =
            baseBuilder(eventId, occurrenceDate, notificationId)
                .setContentTitle(context.getString(R.string.reminder_public_title))
                .setContentText(time)
                .build()

        /**
         * What both copies share: the icon, the tap target for [eventId], and the Snooze/Done actions
         * for [notificationId] and [occurrenceDate] (ROADMAP M6 T4).
         */
        private fun baseBuilder(
            eventId: Long,
            occurrenceDate: LocalDate,
            notificationId: Int,
        ): NotificationCompat.Builder =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_reminder_notification)
                .setShowWhen(false)
                .apply { contentIntent(eventId)?.let(::setContentIntent) }
                .addAction(snoozeAction(eventId, occurrenceDate, notificationId))
                .addAction(doneAction(eventId, occurrenceDate, notificationId))

        /**
         * When the occurrence starts, as the user reads it: a localized short time, or "All day".
         *
         * The device's 12/24-hour override is not consulted; the format follows the locale, like the
         * rest of the app's `java.time` formatting.
         */
        private fun timeText(
            reminder: PendingReminder,
            deviceZone: ZoneId,
        ): String =
            if (reminder.allDay) {
                context.getString(R.string.reminder_all_day)
            } else {
                DateTimeFormatter
                    .ofLocalizedTime(FormatStyle.SHORT)
                    .withLocale(Locale.getDefault())
                    .format(reminder.reference.withZoneSameInstant(deviceZone))
            }

        /**
         * Opens the app's own launcher activity, resolved through `PackageManager` exactly as the
         * widgets do (`:core:scheduling` cannot name `MainActivity`, which lives in `:app`).
         *
         * Explicit, immutable and carrying only [eventId] under [ReminderIntent.EXTRA_EVENT_ID], with
         * [ReminderIntent.ACTION_OPEN_EVENT] as the action — no event title, date or other content
         * rides along (`docs/security-and-privacy.md` §6.4; CLAUDE.md rule 8). `:app`'s `IntentRouter`
         * (ROADMAP M4 T10) reads these to open the event's own editor.
         *
         * The request code is derived from [eventId] (`eventId.hashCode()`), not a single shared
         * constant: two different events must never share one `PendingIntent`, or tapping the older
         * notification would open whichever event most recently rebuilt it
         * (`docs/security-and-privacy.md` §6.4 "a stable per-event request code"). Two reminders on the
         * *same* event naturally resolve to the identical `PendingIntent`, which is harmless — both
         * already point at the same event.
         *
         * `null` only if the platform cannot resolve this app's launcher, in which case the
         * notification is still posted and simply does nothing when tapped — there is no safe
         * implicit intent to fall back to.
         */
        private fun contentIntent(eventId: Long): PendingIntent? {
            val launch =
                context.packageManager
                    .getLaunchIntentForPackage(context.packageName)
                    // Required to start an activity from a notification, outside an activity context.
                    ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ?.setAction(ReminderIntent.ACTION_OPEN_EVENT)
                    ?.putExtra(ReminderIntent.EXTRA_EVENT_ID, eventId)
                    ?: return null
            return PendingIntent.getActivity(
                context,
                eventId.hashCode(),
                launch,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        /**
         * The "Snooze 10 min" action (ROADMAP M6 T4): an explicit, immutable broadcast to
         * [ReminderActionReceiver] carrying only [eventId], [occurrenceDate]'s epoch day and
         * [notificationId] under [ReminderActionIntent]'s extras (CLAUDE.md rule 8). No icon is set
         * (`0`): action icons are not shown on modern Android, so there is nothing to reuse or invent.
         * [ReminderActionIntent.snoozeRequestCode] keeps this from ever sharing a `PendingIntent` with
         * the Done action or with another notification's Snooze action.
         */
        private fun snoozeAction(
            eventId: Long,
            occurrenceDate: LocalDate,
            notificationId: Int,
        ): NotificationCompat.Action =
            NotificationCompat.Action
                .Builder(
                    0,
                    context.getString(R.string.reminder_action_snooze),
                    actionIntent(
                        ReminderActionIntent.ACTION_SNOOZE,
                        eventId,
                        occurrenceDate,
                        notificationId,
                        ReminderActionIntent.snoozeRequestCode(notificationId),
                    ),
                ).build()

        /**
         * The "Done" action (ROADMAP M6 T4): dismisses [notificationId] and changes nothing else — the
         * same extras as [snoozeAction] for a uniform receiver, even though only [notificationId] is
         * read for this action. [ReminderActionIntent.doneRequestCode] keeps it from sharing a
         * `PendingIntent` with the Snooze action on the same notification.
         */
        private fun doneAction(
            eventId: Long,
            occurrenceDate: LocalDate,
            notificationId: Int,
        ): NotificationCompat.Action =
            NotificationCompat.Action
                .Builder(
                    0,
                    context.getString(R.string.reminder_action_done),
                    actionIntent(
                        ReminderActionIntent.ACTION_DONE,
                        eventId,
                        occurrenceDate,
                        notificationId,
                        ReminderActionIntent.doneRequestCode(notificationId),
                    ),
                ).build()

        /** What [snoozeAction] and [doneAction] share, parameterised only by [action] and [requestCode]. */
        private fun actionIntent(
            action: String,
            eventId: Long,
            occurrenceDate: LocalDate,
            notificationId: Int,
            requestCode: Int,
        ): PendingIntent {
            val intent =
                Intent(context, ReminderActionReceiver::class.java)
                    .setAction(action)
                    .putExtra(ReminderActionIntent.EXTRA_EVENT_ID, eventId)
                    .putExtra(ReminderActionIntent.EXTRA_OCCURRENCE_EPOCH_DAY, occurrenceDate.toEpochDay())
                    .putExtra(ReminderActionIntent.EXTRA_NOTIFICATION_ID, notificationId)
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        companion object {
            /** The app's single reminder channel (`docs/security-and-privacy.md` §3.3). */
            const val CHANNEL_ID: String = "reminders"

            /**
             * A stable notification id for a reminder, derived from **ids and dates only**: the same
             * reminder always replaces its own notification instead of adding a second one, across
             * processes and reboots.
             */
            fun notificationId(reminder: PendingReminder): Int {
                var hash = reminder.eventId.hashCode()
                hash = 31 * hash + reminder.occurrenceDate.toEpochDay().hashCode()
                hash = 31 * hash + reminder.minutesBefore
                return hash
            }
        }
    }
