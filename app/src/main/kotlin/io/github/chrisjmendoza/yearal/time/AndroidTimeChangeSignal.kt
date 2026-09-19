package io.github.chrisjmendoza.yearal.time

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.chrisjmendoza.yearal.core.domain.TimeChangeSignal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Android half of [TimeChangeSignal]: fires on the three broadcasts a running screen cannot see
 * any other way, and on every activity resume, exactly what `docs/ARCHITECTURE.md` §4 "State
 * management" specifies and ROADMAP R1 asks for.
 *
 * Two independent sources feed the same [changes] flow:
 * - A context-registered [BroadcastReceiver] for `ACTION_TIME_CHANGED`, `ACTION_TIMEZONE_CHANGED` and
 *   `ACTION_DATE_CHANGED`. The first two are on the implicit-broadcast exemption list; `DATE_CHANGED`
 *   is not, which is exactly why it can only ever be observed this way, never from a manifest receiver
 *   (`docs/security-and-privacy.md` §6.3; contrast [io.github.chrisjmendoza.yearal.core.scheduling.SystemEventReceiver],
 *   which handles the same first two actions from the manifest for when nothing is running). Registered
 *   with `RECEIVER_NOT_EXPORTED` through [ContextCompat.registerReceiver], which also covers API 26–32,
 *   where the flag constant does not exist as a manifest attribute but the compat call still applies it.
 * - Every activity resume, through [Application.ActivityLifecycleCallbacks]: coming back to the
 *   foreground is itself a reason to recompute, independent of any broadcast (the change could have
 *   happened while this process was frozen and missed the broadcast entirely, or in Doze).
 *   `androidx.lifecycle:lifecycle-process` is not a dependency of this module (CLAUDE.md "no new
 *   dependency ... without ... justification"), so this uses the plain `Application` callback instead
 *   of `ProcessLifecycleOwner`; the effect for this purpose is the same, since every resume of every
 *   activity is a valid moment to recompute "today" and firing on each one, not merely the first
 *   foregrounding, is harmless (`RealDateTicker` only ever emits when the recomputed date differs).
 *
 * **Construction does no registration.** [start] does, and is meant to be called exactly once, after
 * this object is fully constructed — the constructor never hands a reference to itself, or to anything
 * capturing `this` before its fields are initialised, to `registerReceiver` or
 * `registerActivityLifecycleCallbacks`. [IfcApplication][io.github.chrisjmendoza.yearal.IfcApplication]
 * calls [start] once from `onCreate`, the same place it arms the day-rollover alarm.
 *
 * Spec: `docs/calendar-spec.md` §7.8; `docs/ARCHITECTURE.md` §4 "State management".
 */
@Singleton
public class AndroidTimeChangeSignal
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : TimeChangeSignal,
        Application.ActivityLifecycleCallbacks {
        private val signal = MutableSharedFlow<Unit>(extraBufferCapacity = EXTRA_BUFFER_CAPACITY)

        /** Emits once per broadcast or resume described on the class. Never completes. */
        override val changes: Flow<Unit> = signal.asSharedFlow()

        @Volatile
        private var started = false

        private val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    receiverContext: Context,
                    intent: Intent,
                ) {
                    if (intent.action in WATCHED_ACTIONS) signal.tryEmit(Unit)
                }
            }

        /**
         * Registers the context-registered receiver and this as an activity-lifecycle callback for the
         * rest of the process's life. Idempotent: only the first call does anything, so it is safe to
         * call from a place that might run more than once.
         */
        public fun start() {
            if (started) return
            started = true
            val filter =
                IntentFilter().apply {
                    addAction(Intent.ACTION_TIME_CHANGED)
                    addAction(Intent.ACTION_TIMEZONE_CHANGED)
                    addAction(Intent.ACTION_DATE_CHANGED)
                }
            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            (context.applicationContext as Application).registerActivityLifecycleCallbacks(this)
        }

        override fun onActivityResumed(activity: Activity) {
            signal.tryEmit(Unit)
        }

        override fun onActivityCreated(
            activity: Activity,
            savedInstanceState: Bundle?,
        ) {
        }

        override fun onActivityStarted(activity: Activity) {}

        override fun onActivityPaused(activity: Activity) {}

        override fun onActivityStopped(activity: Activity) {}

        override fun onActivitySaveInstanceState(
            activity: Activity,
            outState: Bundle,
        ) {
        }

        override fun onActivityDestroyed(activity: Activity) {}

        private companion object {
            /**
             * Generous on purpose: a firing only ever means "recompute", never carries data, so
             * buffering many is free and [io.github.chrisjmendoza.yearal.core.domain.RealDateTicker]
             * conflates them anyway. What matters is that [signal].tryEmit never fails from
             * `onReceive`/`onActivityResumed`, which must not suspend.
             */
            const val EXTRA_BUFFER_CAPACITY = 8

            /** Actions this receiver reacts to; anything else reaching [onReceive] is ignored. */
            val WATCHED_ACTIONS =
                setOf(Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_DATE_CHANGED)
        }
    }
