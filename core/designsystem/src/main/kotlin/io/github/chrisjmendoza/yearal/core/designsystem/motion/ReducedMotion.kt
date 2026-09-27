package io.github.chrisjmendoza.yearal.core.designsystem.motion

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * Overrides [rememberReducedMotion]'s result for a subtree, bypassing the system-settings read.
 * `null` (the default) means "no override, read the real system setting" — this is what every screen
 * gets in production. A test or `@Preview` supplies `true`/`false` with [CompositionLocalProvider] to
 * pin the value deterministically, since the system setting is not controllable from a plain Compose
 * preview and is awkward to flip mid-test without Robolectric's `Settings.Global` shadow (see
 * [rememberReducedMotion]'s test, `ReducedMotionTest`).
 */
val LocalReducedMotion: ProvidableCompositionLocal<Boolean?> = staticCompositionLocalOf { null }

/**
 * Whether the user has asked the system to remove or shorten animations (Android Settings →
 * Accessibility → "Remove animations", or Developer Options → "Animator duration scale" set to 0.5x
 * or lower is still non-zero and does **not** count — only the exact "off" state, 0f, does, matching
 * what "Remove animations" actually writes). Docs/ARCHITECTURE.md §4 "Accessibility": a decorative
 * animation (a pager scroll that is not itself the thing carrying information, a fade, a size change)
 * is skipped or made instant when this is `true`; a transition that *carries meaning* — a value
 * appearing, an error being announced — is left alone.
 *
 * Backed by [Settings.Global.ANIMATOR_DURATION_SCALE] through [LocalContext]'s `contentResolver`: `0f`
 * means the setting is off, any other value (the default is `1f`) means animations run normally.
 * Re-reads whenever that setting changes while this composable is part of the composition, via a
 * [ContentObserver] registered and unregistered by a [DisposableEffect] — not just once on first
 * composition — so a screen that is already open when the user flips the setting (from Quick Settings
 * or Settings) picks it up without needing to be recreated.
 *
 * [LocalReducedMotion] overrides this entirely when non-`null`, for tests and previews that cannot (or
 * should not) depend on the real system setting.
 *
 * **Trap:** this reads a system *setting*, not a live "is an animation currently suppressed by the
 * system" signal — there is no such per-animation API. Respecting it means branching application code
 * (`if (rememberReducedMotion()) … else …`), not relying on the animation framework to shorten anything
 * on its own.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val override = LocalReducedMotion.current
    if (override != null) return override

    val context = LocalContext.current
    var reduced by remember(context) { mutableStateOf(context.isAnimatorDurationScaleZero()) }

    DisposableEffect(context) {
        val resolver = context.contentResolver
        val observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    reduced = context.isAnimatorDurationScaleZero()
                }
            }
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            // notifyForDescendants =
            false,
            observer,
        )
        onDispose { resolver.unregisterContentObserver(observer) }
    }

    return reduced
}

/** `true` exactly when [Settings.Global.ANIMATOR_DURATION_SCALE] is `0f` ("Remove animations"). */
private fun Context.isAnimatorDurationScaleZero(): Boolean =
    Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
