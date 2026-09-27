package io.github.chrisjmendoza.yearal.feature.settings.privacy

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.chrisjmendoza.yearal.core.designsystem.adaptive.limitContentWidth
import io.github.chrisjmendoza.yearal.core.designsystem.theme.Dimens
import io.github.chrisjmendoza.yearal.core.designsystem.theme.IfcTheme
import io.github.chrisjmendoza.yearal.core.designsystem.theme.yearalTopAppBarColors
import io.github.chrisjmendoza.yearal.core.navigation.Navigator
import io.github.chrisjmendoza.yearal.feature.settings.R

/**
 * The Privacy screen (docs/FEATURES.md P5; docs/ROADMAP.md M2 T12's in-app half): a truthful,
 * plain-language statement of what the app does and does not do with data, as the app is actually
 * built today. Every claim here is backed by `docs/security-and-privacy.md` (the allow-list in §5, the
 * widget privacy ruling in §3.2, and the "Send feedback" intent in §6.3) or the merged manifest —
 * nothing is aspirational. Kept in step with `docs/privacy-policy.md`, the same claims in the
 * standalone document that will eventually be published on the web (`privacy_policy_body` below).
 * This is the composable `:app` places behind `PrivacyKey`; the back arrow pops through [navigator].
 *
 * @param modifier applied to the screen's root [Scaffold].
 */
@Composable
fun PrivacyRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    PrivacyScreen(onBack = navigator::goBack, modifier = modifier)
}

/**
 * The stateless Privacy screen — the unit for previews, screenshot and Compose tests
 * (docs/ARCHITECTURE.md §4 "State management"). Purely static text: there is nothing here that
 * depends on device state or user settings, so there is no ViewModel.
 *
 * Opts in to the Material 3 experimental marker only because `TopAppBar`'s default arguments
 * (`TopAppBarDefaults`) still carry it.
 *
 * @param onBack the top app bar's back arrow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.privacy_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.privacy_back),
                        )
                    }
                },
                colors = yearalTopAppBarColors(),
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp)
                    // Tablet pass (docs/ARCHITECTURE.md §4 "Adaptive layouts"): a prose page, so it caps
                    // at the narrower Dimens.ReadingMaxWidth rather than ContentMaxWidth.
                    .limitContentWidth(maxWidth = Dimens.ReadingMaxWidth)
                    .testTag(PrivacyScreenTestTags.CONTENT_COLUMN),
        ) {
            SectionHeading(stringResource(R.string.privacy_section_summary))
            BodyParagraph(stringResource(R.string.privacy_summary_body))
            // Tagged (only this one) so a test can measure a `fillMaxWidth()` element genuinely laid
            // out inside the capped column — the column's own tag reports the *window's* width, since
            // `limitContentWidth()`'s wrapContentWidth/widthIn re-centring happens above it, not below.
            HorizontalDivider(modifier = Modifier.testTag(PrivacyScreenTestTags.WIDTH_PROBE))

            SectionHeading(stringResource(R.string.privacy_section_data))
            BodyParagraph(stringResource(R.string.privacy_data_body))
            HorizontalDivider()

            SectionHeading(stringResource(R.string.privacy_section_permissions))
            BodyParagraph(stringResource(R.string.privacy_permission_boot))
            BodyParagraph(stringResource(R.string.privacy_permission_wake))
            BodyParagraph(stringResource(R.string.privacy_permission_notifications))
            BodyParagraph(stringResource(R.string.privacy_permission_exact_alarm))
            BodyParagraph(stringResource(R.string.privacy_permission_none))
            HorizontalDivider()

            SectionHeading(stringResource(R.string.privacy_section_backup))
            BodyParagraph(stringResource(R.string.privacy_backup_body))
            HorizontalDivider()

            SectionHeading(stringResource(R.string.privacy_section_widgets))
            BodyParagraph(stringResource(R.string.privacy_widgets_body))
            HorizontalDivider()

            SectionHeading(stringResource(R.string.privacy_section_feedback))
            BodyParagraph(stringResource(R.string.privacy_feedback_body))
            HorizontalDivider()

            SectionHeading(stringResource(R.string.privacy_section_policy))
            BodyParagraph(stringResource(R.string.privacy_policy_body))
        }
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier =
            Modifier
                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
                .semantics { heading() },
    )
}

@Composable
private fun BodyParagraph(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

/** Test tags [PrivacyScreenTest] needs to reach past the screen's own content. */
internal object PrivacyScreenTestTags {
    /**
     * The scrolling content [Column], capped at [Dimens.ReadingMaxWidth] on a wide window (tablet
     * pass, docs/ARCHITECTURE.md §4 "Adaptive layouts").
     */
    const val CONTENT_COLUMN: String = "privacy:contentColumn"

    /**
     * The first [HorizontalDivider], the one `fillMaxWidth()` element genuinely laid out *inside* the
     * capped column, for a test to measure the real effect of the cap: [CONTENT_COLUMN]'s own bounds
     * report the window's width regardless of the cap, since `limitContentWidth()`'s re-centring
     * modifiers wrap around it, not inside it.
     */
    const val WIDTH_PROBE: String = "privacy:widthProbe"
}

@Preview(name = "Privacy", showBackground = true, heightDp = 1400)
@Composable
internal fun PrivacyScreenPreview() {
    IfcTheme(dynamicColor = false) {
        PrivacyScreen(onBack = {})
    }
}

@Preview(name = "Privacy — dark", showBackground = true, heightDp = 1400, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun PrivacyScreenDarkPreview() {
    IfcTheme(darkTheme = true, dynamicColor = false) {
        PrivacyScreen(onBack = {})
    }
}

@Preview(name = "Privacy — font 2.0", showBackground = true, heightDp = 2800, fontScale = 2f)
@Composable
internal fun PrivacyScreenLargeFontPreview() {
    IfcTheme(dynamicColor = false) {
        PrivacyScreen(onBack = {})
    }
}
