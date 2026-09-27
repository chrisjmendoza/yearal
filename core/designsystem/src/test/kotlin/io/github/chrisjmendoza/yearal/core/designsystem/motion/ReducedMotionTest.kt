package io.github.chrisjmendoza.yearal.core.designsystem.motion

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [rememberReducedMotion] against [Settings.Global.ANIMATOR_DURATION_SCALE] (docs/ARCHITECTURE.md §4
 * "Accessibility", a11y audit finding #22): `0f` reads as reduced, any other value does not, a change
 * to the setting while the composable stays in composition is picked up through its [android.database.ContentObserver]
 * without recomposing for an unrelated reason first, and [LocalReducedMotion] overrides the system read
 * entirely.
 */
@RunWith(AndroidJUnit4::class)
class ReducedMotionTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun putAnimatorDurationScale(scale: Float) {
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, scale)
    }

    /** Simulates the system notifying observers, which Robolectric's [Settings.Global] shadow does not. */
    private fun notifyAnimatorDurationScaleChanged() {
        context.contentResolver.notifyChange(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            null,
        )
    }

    private fun show(): () -> Boolean {
        var result: Boolean? = null
        compose.setContent {
            result = rememberReducedMotion()
        }
        compose.waitForIdle()
        return { checkNotNull(result) }
    }

    @Test
    fun `the default animator duration scale (1x) is not reduced motion`() {
        putAnimatorDurationScale(1f)

        show()() shouldBe false
    }

    @Test
    fun `an animator duration scale of exactly zero is reduced motion`() {
        putAnimatorDurationScale(0f)

        show()() shouldBe true
    }

    @Test
    fun `a non-zero scale below 1x is still not reduced motion`() {
        // Only the exact "Remove animations" off-state (0f) counts — a merely shortened scale (e.g.
        // Developer Options' 0.5x) is not the same setting and must not be conflated with it.
        putAnimatorDurationScale(0.5f)

        show()() shouldBe false
    }

    @Test
    fun `a change to the setting while composed is picked up through the content observer`() {
        putAnimatorDurationScale(1f)
        val reduced = show()
        reduced() shouldBe false

        putAnimatorDurationScale(0f)
        notifyAnimatorDurationScaleChanged()
        compose.waitForIdle()

        reduced() shouldBe true
    }

    @Test
    fun `a change back to non-zero is picked up the same way`() {
        putAnimatorDurationScale(0f)
        val reduced = show()
        reduced() shouldBe true

        putAnimatorDurationScale(1f)
        notifyAnimatorDurationScaleChanged()
        compose.waitForIdle()

        reduced() shouldBe false
    }

    @Test
    fun `LocalReducedMotion overrides the system setting when true`() {
        putAnimatorDurationScale(1f)
        var result: Boolean? = null
        compose.setContent {
            CompositionLocalProvider(LocalReducedMotion provides true) {
                result = rememberReducedMotion()
            }
        }
        compose.waitForIdle()

        result shouldBe true
    }

    @Test
    fun `LocalReducedMotion overrides the system setting when false`() {
        putAnimatorDurationScale(0f)
        var result: Boolean? = null
        compose.setContent {
            CompositionLocalProvider(LocalReducedMotion provides false) {
                result = rememberReducedMotion()
            }
        }
        compose.waitForIdle()

        result shouldBe false
    }

    @Test
    fun `no override reads the real system setting`() {
        putAnimatorDurationScale(0f)
        var result: Boolean? = null
        compose.setContent {
            CompositionLocalProvider(LocalReducedMotion provides null) {
                result = rememberReducedMotion()
            }
        }
        compose.waitForIdle()

        result shouldBe true
    }
}
