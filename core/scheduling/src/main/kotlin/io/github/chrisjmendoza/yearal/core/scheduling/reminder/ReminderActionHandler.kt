package io.github.chrisjmendoza.yearal.core.scheduling.reminder

import io.github.chrisjmendoza.yearal.core.scheduling.di.RolloverCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * What [ReminderActionReceiver] does once it has validated its intent (ROADMAP M6 T4; FEATURES E11):
 * Snooze and Done run straight through — [AlarmReminderScheduler.snooze] and
 * [AlarmReminderScheduler.dismiss] only touch `SharedPreferences`, `NotificationManager` and
 * `AlarmManager`, none of which suspend — and the delayed snooze-fire alarm's recomputation runs on
 * the process-lifetime rollover scope under the same budget [ReminderBroadcastHandler] uses, since it
 * reads the event repository and cannot run on the broadcast thread.
 */
internal class ReminderActionHandler
    @Inject
    constructor(
        private val scheduler: AlarmReminderScheduler,
        @param:RolloverCoroutineScope private val scope: CoroutineScope,
    ) {
        /** The Done button: dismiss only, no data change. */
        fun done(notificationId: Int) {
            scheduler.dismiss(notificationId)
        }

        /** The Snooze button: cancels the current notification and arms a delayed re-post. */
        fun snooze(
            eventId: Long,
            occurrenceEpochDay: Long,
            notificationId: Int,
        ) {
            scheduler.snooze(eventId, occurrenceEpochDay, notificationId)
        }

        /**
         * The snooze-fire alarm: re-describes the occurrence from the repository and re-posts the
         * notification. See the class KDoc for why this, alone of the three, needs the coroutine scope.
         *
         * @param onFinished called exactly once when the recomputation has finished, failed or been
         *   cancelled at [ReminderBroadcastHandler.RECOMPUTE_BUDGET]; the receiver passes
         *   `PendingResult.finish()` here.
         */
        fun fireSnooze(
            eventId: Long,
            occurrenceEpochDay: Long,
            notificationId: Int,
            onFinished: () -> Unit,
        ) {
            scope.launch {
                try {
                    withTimeoutOrNull(ReminderBroadcastHandler.RECOMPUTE_BUDGET.toMillis()) {
                        scheduler.fireSnooze(eventId, occurrenceEpochDay, notificationId)
                    }
                } finally {
                    onFinished()
                }
            }
        }
    }
