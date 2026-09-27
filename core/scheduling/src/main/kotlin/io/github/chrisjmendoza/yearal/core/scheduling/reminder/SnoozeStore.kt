package io.github.chrisjmendoza.yearal.core.scheduling.reminder

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the reminders that are currently snoozed (ROADMAP M6 T4; FEATURES E11), so that
 * [AlarmReminderScheduler] can re-arm them after a reboot — which cancels every `AlarmManager` alarm,
 * including a snooze's — the same way it already re-arms the single next-alarm.
 *
 * ## Why a new store, and why this shape
 *
 * `:core:scheduling` has no persistence today and, by the module boundary rule
 * (`docs/ARCHITECTURE.md` §2), may never depend on `:core:data`: Room and the settings `DataStore`
 * both live there and are off limits. A plain `SharedPreferences` file is the least invasive option
 * that actually survives a reboot — no new Gradle dependency, no cross-module edge, just the Android
 * framework API this module already uses for `Context`. The alternative the task brief allows, an
 * in-memory store lost on reboot, was rejected because this was easy to do properly instead.
 *
 * Only **ids, an epoch day and an epoch-milli instant** are stored (CLAUDE.md rule 8): no event
 * title, no notification text. One entry is one row of `"eventId:occurrenceEpochDay:fireAtEpochMilli"`
 * keyed by the (stable, per-notification) key [keyFor]; a malformed row — there should never be one,
 * short of external tampering with the app's private prefs file — is skipped rather than crashing.
 */
@Singleton
internal class SnoozeStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val prefs: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        /**
         * One snoozed reminder: the notification it belongs to, the event and occurrence it
         * describes, and the instant it should fire again.
         */
        data class Entry(
            val notificationId: Int,
            val eventId: Long,
            val occurrenceEpochDay: Long,
            val fireAt: Instant,
        )

        /** Persists [entry], replacing any existing entry for the same [Entry.notificationId]. */
        fun put(entry: Entry) {
            prefs.edit {
                putString(
                    keyFor(entry.notificationId),
                    "${entry.eventId}$SEPARATOR${entry.occurrenceEpochDay}$SEPARATOR${entry.fireAt.toEpochMilli()}",
                )
            }
        }

        /** Removes the entry for [notificationId], if any. A no-op when there is none. */
        fun remove(notificationId: Int) {
            prefs.edit { remove(keyFor(notificationId)) }
        }

        /** Every persisted entry, in no particular order. Malformed rows are silently skipped. */
        fun all(): List<Entry> = prefs.all.mapNotNull { (key, value) -> parse(key, value) }

        private fun parse(
            key: String,
            value: Any?,
        ): Entry? {
            val notificationId = key.removePrefix(KEY_PREFIX).takeIf { it != key }?.toIntOrNull() ?: return null
            val parts = (value as? String)?.split(SEPARATOR) ?: return null
            if (parts.size != 3) return null
            val eventId = parts[0].toLongOrNull() ?: return null
            val occurrenceEpochDay = parts[1].toLongOrNull() ?: return null
            val fireAtMillis = parts[2].toLongOrNull() ?: return null
            return Entry(notificationId, eventId, occurrenceEpochDay, Instant.ofEpochMilli(fireAtMillis))
        }

        private fun keyFor(notificationId: Int): String = "$KEY_PREFIX$notificationId"

        companion object {
            /** This module's own preferences file; nothing else writes to it. */
            const val PREFS_NAME: String = "io.github.chrisjmendoza.yearal.core.scheduling.reminder_snoozes"

            private const val KEY_PREFIX = "snooze_"
            private const val SEPARATOR = ":"
        }
    }
