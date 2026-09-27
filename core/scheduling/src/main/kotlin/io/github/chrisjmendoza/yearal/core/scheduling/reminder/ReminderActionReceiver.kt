package io.github.chrisjmendoza.yearal.core.scheduling.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.EntryPointAccessors
import io.github.chrisjmendoza.yearal.core.scheduling.di.SchedulingEntryPoint

/**
 * Receives the Snooze and Done notification-button taps and the delayed alarm a snooze arms (ROADMAP
 * M6 T4; FEATURES E11), the counterpart of [ReminderAlarmReceiver] for these three broadcasts.
 *
 * Not exported and without an intent filter (`docs/security-and-privacy.md` §6.3), so only the app's
 * own explicit `PendingIntent`s reach it — [ReminderNotifier]'s two action buttons and
 * [AlarmReminderScheduler]'s snooze-fire alarm. Reads only [ReminderActionIntent]'s three id extras
 * (CLAUDE.md rule 8): the event id, the occurrence's epoch day, and the notification id. A missing or
 * malformed extra, or an action that is none of [ReminderActionIntent]'s three, is ignored rather than
 * guessed at.
 */
public class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val notificationId =
            intent.getIntExtra(
                ReminderActionIntent.EXTRA_NOTIFICATION_ID,
                ReminderActionIntent.NO_NOTIFICATION_ID,
            )
        if (notificationId == ReminderActionIntent.NO_NOTIFICATION_ID) return
        val handler =
            EntryPointAccessors
                .fromApplication(context, SchedulingEntryPoint::class.java)
                .reminderActionHandler()
        when (intent.action) {
            ReminderActionIntent.ACTION_DONE -> {
                handler.done(notificationId)
            }

            ReminderActionIntent.ACTION_SNOOZE -> {
                val (eventId, occurrenceEpochDay) = intent.eventAndOccurrenceOrNull() ?: return
                handler.snooze(eventId, occurrenceEpochDay, notificationId)
            }

            ReminderActionIntent.ACTION_SNOOZE_FIRE -> {
                val (eventId, occurrenceEpochDay) = intent.eventAndOccurrenceOrNull() ?: return
                // Reads the store and the repository, so it cannot run on the broadcast thread;
                // goAsync() keeps the broadcast (and the process) alive until it finishes, exactly
                // like dispatchReminderRecompute does for the single next-alarm.
                val pendingResult: BroadcastReceiver.PendingResult? = goAsync()
                handler.fireSnooze(eventId, occurrenceEpochDay, notificationId) { pendingResult?.finish() }
            }

            else -> {
                return
            }
        }
    }

    /** [ReminderActionIntent.EXTRA_EVENT_ID] and [ReminderActionIntent.EXTRA_OCCURRENCE_EPOCH_DAY], or `null` if either is absent. */
    private fun Intent.eventAndOccurrenceOrNull(): Pair<Long, Long>? {
        val eventId = getLongExtra(ReminderActionIntent.EXTRA_EVENT_ID, ReminderActionIntent.NO_ID)
        val occurrenceEpochDay =
            getLongExtra(ReminderActionIntent.EXTRA_OCCURRENCE_EPOCH_DAY, ReminderActionIntent.NO_ID)
        if (eventId == ReminderActionIntent.NO_ID || occurrenceEpochDay == ReminderActionIntent.NO_ID) return null
        return eventId to occurrenceEpochDay
    }
}
