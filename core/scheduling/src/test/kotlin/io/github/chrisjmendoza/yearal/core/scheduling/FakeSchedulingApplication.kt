package io.github.chrisjmendoza.yearal.core.scheduling

import android.app.Application
import dagger.hilt.internal.GeneratedComponent
import dagger.hilt.internal.GeneratedComponentManager
import io.github.chrisjmendoza.yearal.core.scheduling.di.SchedulingEntryPoint
import io.github.chrisjmendoza.yearal.core.scheduling.reminder.ReminderActionHandler
import io.github.chrisjmendoza.yearal.core.scheduling.reminder.ReminderBroadcastHandler

/**
 * A hand-written stand-in for the `@HiltAndroidApp` application, so the receivers run their real
 * `EntryPointAccessors.fromApplication` lookup in a Robolectric test without Hilt code generation.
 * `EntryPoints.get` accepts any application that is a [GeneratedComponentManager] whose component is a
 * [GeneratedComponent] implementing the requested entry point. The real graph (including the empty
 * listener multibinding) is covered by the `:app` test against `IfcApplication`.
 */
class FakeSchedulingApplication :
    Application(),
    GeneratedComponentManager<Any> {
    /** Set by the test before any broadcast is delivered. */
    internal lateinit var component: FakeSchedulingComponent

    override fun generatedComponent(): Any = component
}

/**
 * The slice of the singleton component the receivers use.
 *
 * @param reminderHandler `null` for a test that only exercises the rollover receivers; asking for it
 *   then fails loudly rather than silently doing nothing.
 * @param reminderActionHandler `null` for a test that does not exercise the Snooze/Done receiver;
 *   asking for it then fails loudly rather than silently doing nothing.
 */
internal class FakeSchedulingComponent(
    private val handler: RolloverBroadcastHandler,
    private val reminderHandler: ReminderBroadcastHandler? = null,
    private val reminderActionHandler: ReminderActionHandler? = null,
) : SchedulingEntryPoint,
    GeneratedComponent {
    override fun rolloverBroadcastHandler(): RolloverBroadcastHandler = handler

    override fun reminderBroadcastHandler(): ReminderBroadcastHandler =
        checkNotNull(reminderHandler) { "This test did not install a ReminderBroadcastHandler" }

    override fun reminderActionHandler(): ReminderActionHandler =
        checkNotNull(reminderActionHandler) { "This test did not install a ReminderActionHandler" }
}
