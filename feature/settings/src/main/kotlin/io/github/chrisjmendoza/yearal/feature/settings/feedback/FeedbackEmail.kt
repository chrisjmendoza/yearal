package io.github.chrisjmendoza.yearal.feature.settings.feedback

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.core.net.toUri

/*
 * ROADMAP M8 T6: "Send feedback", bug reporting without a backend. `ACTION_SENDTO` with a `mailto:`
 * data URI restricts the system chooser to apps that handle email and needs no permission
 * (docs/security-and-privacy.md §6.3). The intent carries only the feedback address, a subject and the
 * body [buildFeedbackBody] assembled -- no attachment, no URI grant, nothing logged.
 */

/** `mailto:` scheme prefix for the (address-less) intent data URI; the address itself is an extra. */
private const val MAILTO_URI = "mailto:"

/**
 * Opens the system chooser, restricted to email apps, prefilled with [address], [subject] and [body].
 *
 * @return `false` when nothing on the device can handle it ([ActivityNotFoundException]) -- the caller
 * shows [address] as plain, selectable text instead of a dead control.
 */
internal fun sendFeedbackEmail(
    context: Context,
    address: String,
    subject: String,
    body: String,
    chooserTitle: String,
): Boolean {
    val sendTo =
        Intent(Intent.ACTION_SENDTO, MAILTO_URI.toUri())
            .putExtra(Intent.EXTRA_EMAIL, arrayOf(address))
            .putExtra(Intent.EXTRA_SUBJECT, subject)
            .putExtra(Intent.EXTRA_TEXT, body)
    val chooser = Intent.createChooser(sendTo, chooserTitle)
    // Only an Activity context may start an activity without a new task.
    if (context.findActivity() == null) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
        context.startActivity(chooser)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
