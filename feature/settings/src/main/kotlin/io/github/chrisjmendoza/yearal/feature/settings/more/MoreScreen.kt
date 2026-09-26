package io.github.chrisjmendoza.yearal.feature.settings.more

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.chrisjmendoza.yearal.core.designsystem.theme.IfcTheme
import io.github.chrisjmendoza.yearal.core.designsystem.theme.yearalTopAppBarColors
import io.github.chrisjmendoza.yearal.core.domain.settings.UserSettings
import io.github.chrisjmendoza.yearal.core.navigation.HolidaysKey
import io.github.chrisjmendoza.yearal.core.navigation.LearnKey
import io.github.chrisjmendoza.yearal.core.navigation.Navigator
import io.github.chrisjmendoza.yearal.core.navigation.PrivacyKey
import io.github.chrisjmendoza.yearal.core.navigation.SettingsKey
import io.github.chrisjmendoza.yearal.feature.settings.R
import io.github.chrisjmendoza.yearal.feature.settings.feedback.buildFeedbackBody
import io.github.chrisjmendoza.yearal.feature.settings.feedback.sendFeedbackEmail

/**
 * The More tab's hub (docs/ARCHITECTURE.md §4 "Screens and navigation": More holds Holidays, Settings
 * and Learn/About). This is the composable `:app` places behind `MoreKey`; the Holidays, Settings,
 * Learn and Privacy rows push [HolidaysKey], [SettingsKey], [LearnKey] and [PrivacyKey] through
 * [navigator]. The "Send feedback" row (ROADMAP M8 T6) needs no navigation: it launches an email
 * compose intent directly, so [MoreScreen] is handed [versionCode] and the current [UserSettings],
 * collected here from [MoreViewModel], to build that email's diagnostics.
 *
 * @param appName the launcher label, e.g. `Yearal`; it lives in `:app`'s resources, so the caller
 * passes it rather than the feature duplicating the string.
 * @param versionName the app's `versionName`, e.g. `0.1.0`; supplied by `:app`, which owns the
 * package information (the feature never reads `PackageManager`).
 * @param versionCode the app's `versionCode`, e.g. `3`; supplied by `:app` alongside [versionName], for
 * the feedback email's subject and diagnostics.
 * @param modifier applied to the screen's root [Scaffold].
 */
@Composable
fun MoreRoute(
    navigator: Navigator,
    appName: String,
    versionName: String,
    versionCode: Int,
    modifier: Modifier = Modifier,
    viewModel: MoreViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    MoreScreen(
        appName = appName,
        versionName = versionName,
        versionCode = versionCode,
        settings = settings,
        onHolidaysClick = { navigator.navigate(HolidaysKey) },
        onSettingsClick = { navigator.navigate(SettingsKey) },
        onLearnClick = { navigator.navigate(LearnKey) },
        onPrivacyClick = { navigator.navigate(PrivacyKey) },
        modifier = modifier,
    )
}

/**
 * The stateless More hub — Holidays, Settings, Learn and Privacy rows, a "Send feedback" row (ROADMAP
 * M8 T6) and a non-interactive About row showing [appName] and [versionName] — the unit for previews,
 * screenshot and Compose tests.
 *
 * Opts in to the Material 3 experimental marker only because `TopAppBar`'s default arguments
 * (`TopAppBarDefaults`) still carry it.
 *
 * @param onHolidaysClick the Holidays row's action (docs/FEATURES.md H1, H2, H3, H5; ROADMAP M6 T2).
 *   Browsing and toggling holiday sets now lives only on that screen — Settings links here instead of
 *   duplicating the switches, so there is one control for "which sets are enabled".
 * @param onSettingsClick the Settings row's action.
 * @param onLearnClick the Learn row's action (docs/FEATURES.md L2).
 * @param onPrivacyClick the Privacy row's action (docs/FEATURES.md P5).
 * @param versionCode the app's `versionCode`, folded into the feedback email's subject and diagnostics.
 * @param settings the current [UserSettings], read only for the feedback email's diagnostics (colour
 * source, theme mode, weekday display, enabled-holiday-set count) -- never event content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    appName: String,
    versionName: String,
    versionCode: Int,
    settings: UserSettings,
    onHolidaysClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLearnClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.more_title)) },
                colors = yearalTopAppBarColors(),
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.more_holidays)) },
                supportingContent = { Text(stringResource(R.string.more_holidays_detail)) },
                leadingContent = { HubIcon(Icons.Filled.DateRange) },
                modifier = Modifier.navRowModifier(onClick = onHolidaysClick),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.more_settings)) },
                supportingContent = { Text(stringResource(R.string.more_settings_detail)) },
                leadingContent = { HubIcon(Icons.Filled.Settings) },
                modifier = Modifier.navRowModifier(onClick = onSettingsClick),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.more_learn)) },
                supportingContent = { Text(stringResource(R.string.more_learn_detail)) },
                leadingContent = { HubIcon(Icons.AutoMirrored.Filled.List) },
                modifier = Modifier.navRowModifier(onClick = onLearnClick),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.more_privacy)) },
                supportingContent = { Text(stringResource(R.string.more_privacy_detail)) },
                leadingContent = { HubIcon(Icons.Filled.Lock) },
                modifier = Modifier.navRowModifier(onClick = onPrivacyClick),
            )
            HorizontalDivider()
            FeedbackRow(versionName = versionName, versionCode = versionCode, settings = settings)
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(appName) },
                supportingContent = { Text(stringResource(R.string.more_version, versionName)) },
                leadingContent = { Icon(imageVector = Icons.Filled.Info, contentDescription = null) },
            )
        }
    }
}

/**
 * A row's leading glyph in a 40dp [MaterialTheme.colorScheme.secondaryContainer] circle, tinted
 * [MaterialTheme.colorScheme.onSecondaryContainer] (`docs/design-plan.md` §4.8: "More hub rows get
 * tinted leading icons"). The About row keeps its bare icon — it links nowhere and isn't one of the
 * hub's navigation rows.
 */
@Composable
private fun HubIcon(icon: ImageVector) {
    Box(
        modifier =
            Modifier
                .size(HubIconSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

private val HubIconSize = 40.dp

/**
 * The full-width, ≥48dp-tall [Role.Button] click target every hub row shares — Holidays, Settings,
 * Learn, Privacy and [FeedbackRow] (a11y audit finding #27). Without `.fillMaxWidth()`, a [ListItem]
 * sizes to its content and the tappable area may not span the whole row's visible width, and
 * `.heightIn(min = MinTouchTarget)` is the explicit 48dp floor (docs/ARCHITECTURE.md §4
 * "Accessibility") rather than the Robolectric-inert `Modifier.minimumInteractiveComponentSize()`.
 */
private fun Modifier.navRowModifier(onClick: () -> Unit): Modifier =
    this
        .fillMaxWidth()
        .heightIn(min = MinTouchTarget)
        .clickable(role = Role.Button, onClick = onClick)

/**
 * "Send feedback" (ROADMAP M8 T6): a row shaped like the four above it, but its action is not
 * navigation -- tapping it opens the system email chooser (restricted to email apps by the `mailto:`
 * data URI, `docs/security-and-privacy.md` §6.3) prefilled with the feedback address, a version-stamped
 * subject and a body built by [buildFeedbackBody] from [versionName], [versionCode] and [settings]
 * only -- device, app and settings values, never event or holiday-pack content (CLAUDE.md rule 8).
 * When nothing on the device can handle the intent, [sendFeedbackEmail] returns `false` and this shows
 * the address as plain, selectable text underneath instead of leaving a dead control or crashing.
 */
@Composable
private fun FeedbackRow(
    versionName: String,
    versionCode: Int,
    settings: UserSettings,
) {
    val context = LocalContext.current
    val address = stringResource(R.string.feedback_email)
    val locale = LocalConfiguration.current.locales[0]
    var emailUnavailable by rememberSaveable { mutableStateOf(false) }
    val body =
        buildFeedbackBody(
            promptLine = stringResource(R.string.feedback_body_prompt),
            diagnosticsHeading = stringResource(R.string.feedback_body_diagnostics_heading),
            appVersionLine = stringResource(R.string.feedback_body_app_version, versionName, versionCode),
            androidVersionLine =
                stringResource(R.string.feedback_body_android_version, Build.VERSION.RELEASE, Build.VERSION.SDK_INT),
            deviceLine = stringResource(R.string.feedback_body_device, Build.MANUFACTURER, Build.MODEL),
            localeLine = stringResource(R.string.feedback_body_locale, locale.toLanguageTag()),
            colorSourceLine = stringResource(R.string.feedback_body_color_source, settings.colorSource.name),
            themeModeLine = stringResource(R.string.feedback_body_theme_mode, settings.themeMode.name),
            weekdayDisplayLine = stringResource(R.string.feedback_body_weekday_display, settings.weekdayDisplay.name),
            holidaySetsLine = stringResource(R.string.feedback_body_holiday_sets, settings.enabledHolidaySets.size),
        )
    val subject = stringResource(R.string.feedback_subject, versionName)
    val chooserTitle = stringResource(R.string.more_feedback)
    ListItem(
        headlineContent = { Text(stringResource(R.string.more_feedback)) },
        supportingContent = { Text(stringResource(R.string.more_feedback_detail)) },
        leadingContent = { HubIcon(Icons.AutoMirrored.Filled.Send) },
        modifier =
            Modifier.navRowModifier(onClick = {
                emailUnavailable = !sendFeedbackEmail(context, address, subject, body, chooserTitle)
            }),
    )
    if (emailUnavailable) {
        FeedbackEmailUnavailableNotice(address)
    }
}

/**
 * Shown under the feedback row when [sendFeedbackEmail] found no app to handle the intent: an inline,
 * non-dismissing notice (no snackbar, no toast) naming the address as plain, selectable text so the
 * user can copy it by hand.
 */
@Composable
private fun FeedbackEmailUnavailableNotice(address: String) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.feedback_email_unavailable),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SelectionContainer {
            Text(text = address, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** The 48dp touch-target floor (docs/ARCHITECTURE.md §4 "Accessibility"). */
private val MinTouchTarget = 48.dp

@Preview(name = "More hub", showBackground = true)
@Composable
internal fun MoreScreenPreview() {
    IfcTheme(dynamicColor = false) {
        MoreScreen(
            appName = "Yearal",
            versionName = "0.1.0",
            versionCode = 1,
            settings = UserSettings.DEFAULT,
            onHolidaysClick = {},
            onSettingsClick = {},
            onLearnClick = {},
            onPrivacyClick = {},
        )
    }
}
