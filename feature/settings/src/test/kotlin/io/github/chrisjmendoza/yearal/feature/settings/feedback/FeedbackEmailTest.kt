package io.github.chrisjmendoza.yearal.feature.settings.feedback

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

/**
 * [sendFeedbackEmail] (ROADMAP M8 T6; docs/security-and-privacy.md §6.3): an `ACTION_SENDTO` intent
 * with a bare `mailto:` data URI restricts the system chooser to email apps and needs no permission.
 * Mirrors [io.github.chrisjmendoza.yearal.feature.converter.ConverterSharingTest]'s shape for the
 * equivalent `ACTION_SEND` share intent.
 */
@RunWith(AndroidJUnit4::class)
class FeedbackEmailTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val address = "chrisjmendoza@gmail.com"
    private val subject = "Yearal feedback (v0.1.0)"
    private val body = "Describe what happened:\n\nDiagnostics (no event content):\nApp version: 0.1.0 (3)"
    private val chooserTitle = "Send feedback"

    @Test
    fun `sends a chooser around an ACTION_SENDTO mailto intent with the expected extras`() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()

        sendFeedbackEmail(activity, address, subject, body, chooserTitle) shouldBe true

        val chooser = shadowOf(application).nextStartedActivity.shouldNotBeNull()
        chooser.action shouldBe Intent.ACTION_CHOOSER
        chooser.getStringExtra(Intent.EXTRA_TITLE) shouldBe chooserTitle
        // From an Activity there is no new task.
        (chooser.flags and Intent.FLAG_ACTIVITY_NEW_TASK) shouldBe 0

        val sendTo = chooser.extraIntent().shouldNotBeNull()
        sendTo.action shouldBe Intent.ACTION_SENDTO
        sendTo.data shouldBe Uri.parse("mailto:")
        sendTo.getStringArrayExtra(Intent.EXTRA_EMAIL)?.toList() shouldBe listOf(address)
        sendTo.getStringExtra(Intent.EXTRA_SUBJECT) shouldBe subject
        sendTo.getStringExtra(Intent.EXTRA_TEXT) shouldBe body
        sendTo.component.shouldBeNull()
        sendTo.extras
            ?.keySet()
            .shouldNotBeNull()
            .shouldBe(setOf(Intent.EXTRA_EMAIL, Intent.EXTRA_SUBJECT, Intent.EXTRA_TEXT))
    }

    @Test
    fun `sending from a non-activity context adds the new-task flag`() {
        sendFeedbackEmail(application, address, subject, body, chooserTitle) shouldBe true

        val chooser = shadowOf(application).nextStartedActivity.shouldNotBeNull()
        (chooser.flags and Intent.FLAG_ACTIVITY_NEW_TASK) shouldBeGreaterThan 0
        chooser.extraIntent()?.getStringExtra(Intent.EXTRA_TEXT) shouldBe body
    }

    // The ActivityNotFoundException fallback (returns false; the caller shows the address as plain
    // text) is not exercised here: Robolectric's default shadow resolves `Intent.createChooser`'s
    // ACTION_CHOOSER intent regardless of whether anything can handle the wrapped `ACTION_SENDTO`
    // intent, same as `shareConversion`'s tests -- neither forces that branch.

    // The typed overload is API 33+; these tests run on the SDK pinned in robolectric.properties (36).
    private fun Intent.extraIntent(): Intent? = getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
}
