package io.github.chrisjmendoza.yearal.core.designsystem.explainer

import android.content.res.Configuration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.chrisjmendoza.yearal.core.designsystem.R
import io.github.chrisjmendoza.yearal.core.designsystem.theme.IfcTheme

/**
 * A small, dismissible "what am I looking at?" affordance (`docs/FEATURES.md` L3 "contextual
 * explainers"): a standard-sized [IconButton] that opens a short [AlertDialog] explaining one part of
 * the screen. Built for the two places `docs/FEATURES.md` Part 1 documents real user confusion — the
 * nominal-weekday header row and the intercalary band/floating-day rows on the month grid — but takes no
 * calendar-specific dependency itself, so it is reusable wherever a screen needs the same "info button +
 * short popup" shape.
 *
 * **This component owns no calendar copy.** [title] and [explanation] are supplied entirely by the
 * caller (CLAUDE.md rule 9: every user-visible string lives in resources, but *whose* resources is the
 * caller's choice) — this file only owns its own generic content description template and dismiss label.
 *
 * **Accessibility.** [IconButton] already guarantees the Material 3 48dp minimum touch target
 * (`docs/FEATURES.md` Q4), so no extra sizing is applied here; [ExplainerInfoButtonTest] asserts it
 * directly rather than trusting the default. The button's content description names [title] so multiple
 * explainers on one screen are distinguishable to TalkBack, and Material 3's [AlertDialog] wraps its
 * `text` slot in a scrollable container internally, which is what keeps a long [explanation] from
 * clipping at 200% font scale rather than any measurement done here.
 *
 * **Not a restyle.** This uses [AlertDialog], the same popup component already used elsewhere in this
 * app (e.g. the Settings screen's confirmations) — no new colour, shape or typography is introduced
 * (docs/ROADMAP.md M2 T13 is a later, dedicated design pass).
 *
 * @param title the explainer's short heading, shown as the dialog's title and folded into the button's
 * spoken content description.
 * @param explanation the explainer's body text, one short paragraph.
 * @param modifier applied to the [IconButton].
 */
@Composable
fun ExplainerInfoButton(
    title: String,
    explanation: String,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }, modifier = modifier) {
        Icon(
            painter = painterResource(R.drawable.ic_info),
            contentDescription = stringResource(R.string.explainer_info_content_description, title),
        )
    }
    if (expanded) {
        AlertDialog(
            onDismissRequest = { expanded = false },
            title = { Text(title) },
            text = { Text(explanation) },
            confirmButton = {
                TextButton(onClick = { expanded = false }) {
                    Text(stringResource(R.string.explainer_dismiss))
                }
            },
        )
    }
}

@Preview(name = "Closed", showBackground = true)
@Composable
internal fun ExplainerInfoButtonClosedPreview() {
    IfcTheme(dynamicColor = false) {
        ExplainerInfoButton(
            title = "Why weekdays differ",
            explanation = "The IFC weekday and the real weekday are not the same thing.",
        )
    }
}

@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun ExplainerInfoButtonDarkPreview() {
    IfcTheme(darkTheme = true, dynamicColor = false) {
        ExplainerInfoButton(
            title = "Why weekdays differ",
            explanation = "The IFC weekday and the real weekday are not the same thing.",
        )
    }
}
