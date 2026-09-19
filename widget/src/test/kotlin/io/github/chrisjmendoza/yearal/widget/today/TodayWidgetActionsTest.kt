package io.github.chrisjmendoza.yearal.widget.today

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.chrisjmendoza.yearal.widget.WidgetIntents
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/**
 * The widget's tap action (FEATURES S5; `docs/security-and-privacy.md` §6.4: every `PendingIntent` is
 * explicit and carries IDs only, never content — here, nothing at all). `:widget` cannot depend on
 * `:app`'s `MainActivity` at compile time (docs/ARCHITECTURE.md §2), so [launchAppIntent] must resolve
 * the launcher through [android.content.pm.PackageManager] rather than naming a class.
 *
 * This module's own test manifest declares no launcher activity (only `:app`'s does), so the fake
 * launcher activity below is registered with Robolectric's shadow package manager purely so
 * `getLaunchIntentForPackage` has something to resolve — exactly what the real `:app` manifest already
 * provides for the production widget.
 */
@RunWith(AndroidJUnit4::class)
class TodayWidgetActionsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun registerAFakeLauncherActivity() {
        val component = ComponentName(context.packageName, FAKE_MAIN_ACTIVITY)
        val activityInfo =
            ActivityInfo().apply {
                packageName = component.packageName
                name = component.className
                applicationInfo = context.applicationInfo
            }
        val shadowPackageManager = shadowOf(context.packageManager)
        shadowPackageManager.addOrUpdateActivity(activityInfo)
        shadowPackageManager.addIntentFilterForActivity(
            component,
            IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) },
        )
    }

    @Test
    fun `the launch intent is explicit -- its component is already resolved`() {
        val intent = launchAppIntent(context).shouldNotBeNull()

        intent.component.shouldNotBeNull()
        intent.component!!.packageName shouldBe context.packageName
        intent.component!!.className shouldBe FAKE_MAIN_ACTIVITY
    }

    @Test
    fun `the launch intent carries no extras`() {
        val intent = launchAppIntent(context).shouldNotBeNull()

        (intent.extras == null || intent.extras!!.isEmpty) shouldBe true
    }

    @Test
    fun `the launch intent starts a new task, since it is not launched from an activity`() {
        val intent = launchAppIntent(context).shouldNotBeNull()

        (intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) shouldBe Intent.FLAG_ACTIVITY_NEW_TASK
    }

    // -- ROADMAP M3 T5: the typed launch-intent helpers IntentRouter reads ---------------------------

    @Test
    fun `the Today widget's launch intent carries the open-today action and no extras`() {
        val intent = todayLaunchIntent(context).shouldNotBeNull()

        intent.action shouldBe WidgetIntents.ACTION_OPEN_TODAY
        (intent.extras == null || intent.extras!!.isEmpty) shouldBe true
        intent.component.shouldNotBeNull()
    }

    @Test
    fun `the Month widget's whole-widget launch intent carries the open-month action and no extras`() {
        val intent = monthLaunchIntent(context).shouldNotBeNull()

        intent.action shouldBe WidgetIntents.ACTION_OPEN_MONTH
        (intent.extras == null || intent.extras!!.isEmpty) shouldBe true
    }

    @Test
    fun `a day cell's launch intent carries the open-day action and only the epoch day`() {
        val intent = dayLaunchIntent(context, 20_713L).shouldNotBeNull()

        intent.action shouldBe WidgetIntents.ACTION_OPEN_DAY
        intent.extras?.keySet() shouldBe setOf(WidgetIntents.EXTRA_EPOCH_DAY)
        intent.getLongExtra(WidgetIntents.EXTRA_EPOCH_DAY, -1L) shouldBe 20_713L
        intent.data.shouldBeNull()
    }

    @Test
    fun `two different days carry two different epoch-day extras`() {
        val first = dayLaunchIntent(context, 1L).shouldNotBeNull()
        val second = dayLaunchIntent(context, 2L).shouldNotBeNull()

        first.getLongExtra(WidgetIntents.EXTRA_EPOCH_DAY, -1L) shouldBe 1L
        second.getLongExtra(WidgetIntents.EXTRA_EPOCH_DAY, -1L) shouldBe 2L
    }

    private companion object {
        const val FAKE_MAIN_ACTIVITY = "io.github.chrisjmendoza.yearal.MainActivity"
    }
}
