package io.github.chrisjmendoza.yearal.core.scheduling.reminder

/**
 * The typed contract of a reminder notification's two action buttons — Snooze and Done (ROADMAP M6
 * T4; FEATURES E11's snooze half) — and of the delayed alarm a snooze arms. All three are explicit
 * broadcasts to [ReminderActionReceiver], carrying **ids only** (CLAUDE.md rule 8;
 * `docs/security-and-privacy.md` §6.4): the event id, the occurrence's start date as an epoch day, and
 * the id of the notification the action was posted on. Nothing about the event's title, time text or
 * reminder lead time ever rides along — [ReminderNotifier] and [AlarmReminderScheduler] recompute all
 * of that from the store when they need it, exactly like the content tap ([ReminderIntent]) and the
 * single next-alarm ([AlarmReminderScheduler]) already do.
 *
 * ## Request codes
 *
 * [requestCode] combines a small per-action discriminant with [EXTRA_NOTIFICATION_ID] (already unique
 * per event, occurrence and reminder lead time — [ReminderNotifier.notificationId]) with the same
 * polynomial-hash shape [ReminderNotifier.notificationId] itself uses. Two different notifications
 * therefore never share a Snooze, Done, or snooze-fire `PendingIntent`, and the three actions never
 * share one either, so tapping one can never silently overwrite another's extras
 * (`docs/security-and-privacy.md` §6.4 "a stable per-event request code"). None of this needs to avoid
 * [AlarmReminderScheduler.REQUEST_CODE] or [io.github.chrisjmendoza.yearal.core.scheduling.DayRolloverScheduler]'s
 * own constant: `PendingIntent` matching also compares the target component, and every alarm this
 * module arms targets a different receiver class.
 */
internal object ReminderActionIntent {
    /** Action of the "Snooze 10 min" notification button tap. */
    const val ACTION_SNOOZE: String =
        "io.github.chrisjmendoza.yearal.core.scheduling.action.REMINDER_SNOOZE"

    /** Action of the "Done" notification button tap: dismiss only, no data change. */
    const val ACTION_DONE: String =
        "io.github.chrisjmendoza.yearal.core.scheduling.action.REMINDER_DONE"

    /** Action of the alarm a snooze arms, delivered [AlarmReminderScheduler.SNOOZE_DURATION] later. */
    const val ACTION_SNOOZE_FIRE: String =
        "io.github.chrisjmendoza.yearal.core.scheduling.action.REMINDER_SNOOZE_FIRE"

    /** Extra name for the event id ([Long]); see the class KDoc. */
    const val EXTRA_EVENT_ID: String = "io.github.chrisjmendoza.yearal.extra.EVENT_ID"

    /**
     * Extra name for the occurrence's start date ([Long] epoch day, [java.time.LocalDate.toEpochDay])
     * — together with [EXTRA_EVENT_ID] it identifies which occurrence to re-describe when a snooze
     * fires, exactly the identity [PendingReminder.occurrenceDate] already carries.
     */
    const val EXTRA_OCCURRENCE_EPOCH_DAY: String =
        "io.github.chrisjmendoza.yearal.extra.OCCURRENCE_EPOCH_DAY"

    /** Extra name for the id of the notification the action was posted on ([Int]). */
    const val EXTRA_NOTIFICATION_ID: String = "io.github.chrisjmendoza.yearal.extra.NOTIFICATION_ID"

    /** Sentinel `getLongExtra` default for an absent [EXTRA_EVENT_ID] / [EXTRA_OCCURRENCE_EPOCH_DAY]. */
    const val NO_ID: Long = Long.MIN_VALUE

    /** Sentinel `getIntExtra` default for an absent [EXTRA_NOTIFICATION_ID]. */
    const val NO_NOTIFICATION_ID: Int = 0

    /** Discriminants combined into [requestCode]; distinct, and never `0`. */
    private const val DISCRIMINANT_SNOOZE = 1
    private const val DISCRIMINANT_DONE = 2
    private const val DISCRIMINANT_SNOOZE_FIRE = 3

    /** The request code of the Snooze button's `PendingIntent` for a notification id. */
    fun snoozeRequestCode(notificationId: Int): Int = requestCode(DISCRIMINANT_SNOOZE, notificationId)

    /** The request code of the Done button's `PendingIntent` for a notification id. */
    fun doneRequestCode(notificationId: Int): Int = requestCode(DISCRIMINANT_DONE, notificationId)

    /** The request code of the snooze-fire alarm's `PendingIntent` for a notification id. */
    fun snoozeFireRequestCode(notificationId: Int): Int = requestCode(DISCRIMINANT_SNOOZE_FIRE, notificationId)

    /** See the class KDoc "Request codes". */
    private fun requestCode(
        discriminant: Int,
        notificationId: Int,
    ): Int {
        var hash = discriminant
        hash = 31 * hash + notificationId
        return hash
    }
}
