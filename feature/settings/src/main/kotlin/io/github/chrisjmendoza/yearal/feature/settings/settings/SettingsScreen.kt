package io.github.chrisjmendoza.yearal.feature.settings.settings

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.chrisjmendoza.yearal.core.designsystem.theme.IfcTheme
import io.github.chrisjmendoza.yearal.core.designsystem.theme.colorSchemes
import io.github.chrisjmendoza.yearal.core.designsystem.theme.yearalTopAppBarColors
import io.github.chrisjmendoza.yearal.core.domain.settings.ColorPalette
import io.github.chrisjmendoza.yearal.core.domain.settings.ColorSource
import io.github.chrisjmendoza.yearal.core.domain.settings.ThemeMode
import io.github.chrisjmendoza.yearal.core.domain.settings.UserSettings
import io.github.chrisjmendoza.yearal.core.domain.settings.WeekdayDisplay
import io.github.chrisjmendoza.yearal.core.domain.settings.WidgetTheme
import io.github.chrisjmendoza.yearal.core.navigation.HolidaysKey
import io.github.chrisjmendoza.yearal.core.navigation.Navigator
import io.github.chrisjmendoza.yearal.feature.settings.R
import kotlin.math.roundToInt

/**
 * The Settings screen (docs/FEATURES.md W1, W2, H5): collects [SettingsViewModel.uiState] with the
 * lifecycle and renders it through the stateless [SettingsScreen]. This is the composable `:app`
 * places behind `SettingsKey`; the back arrow pops through [navigator], and the "Holiday sets" row
 * pushes [HolidaysKey] (ROADMAP M6 T2 — that screen, not this one, now owns the pack switches).
 *
 * @param modifier applied to the screen's root [Scaffold].
 */
@Composable
fun SettingsRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onBack = navigator::goBack,
        onWeekdayDisplaySelected = viewModel::setWeekdayDisplay,
        onThemeModeSelected = viewModel::setThemeMode,
        onColorSourceSelected = viewModel::setColorSource,
        onPaletteSelected = viewModel::setPalette,
        onPureBlackChanged = viewModel::setPureBlack,
        onTodayWidgetThemeSelected = viewModel::setTodayWidgetTheme,
        onMonthWidgetThemeSelected = viewModel::setMonthWidgetTheme,
        onWidgetBackgroundOpacityChanged = viewModel::setWidgetBackgroundOpacity,
        onOpenHolidays = { navigator.navigate(HolidaysKey) },
        onRequestDeleteAllData = viewModel::requestDeleteAllData,
        onContinueDeleteAllData = viewModel::continueDeleteAllData,
        onCancelDeleteAllData = viewModel::cancelDeleteAllData,
        onConfirmDeleteAllData = viewModel::confirmDeleteAllData,
        onDismissDeleteAllDataDone = viewModel::dismissDeleteAllDataDone,
        modifier = modifier,
    )
}

/**
 * The stateless Settings screen — the unit for previews, screenshot and Compose tests
 * (docs/ARCHITECTURE.md §4 "State management"). Sections: the weekday-header mode as a radio group
 * with a reminder that IFC weekdays are not real ones (calendar-spec §4.1), an **Appearance** section
 * (`docs/design-plan.md` §5) covering theme, colour source, palette, pure black and the two widgets'
 * light/dark override plus background opacity, a "Holiday sets" row that opens the Holidays screen
 * (FEATURES H5; ROADMAP M6 T2 — the switches themselves live only there now), and "Delete all data"
 * (FEATURES W6) behind a two-step destructive confirmation. Every control reflects
 * [SettingsUiState.Loaded.settings] (or [SettingsUiState.Loaded.deleteAllDataStep]) and reports a
 * change through its callback; nothing is stored locally.
 *
 * Opts in to the Material 3 experimental marker only because `TopAppBar`'s default arguments
 * (`TopAppBarDefaults`) still carry it.
 *
 * @param onBack the top app bar's back arrow.
 * @param onColorSourceSelected the colour-source segmented row's action (design-plan §5.1); the
 * `DYNAMIC` segment is shown disabled below API 31 but the callback is still wired, so a caller never
 * needs to special-case it.
 * @param onPaletteSelected a palette swatch's action (design-plan §5.2); the row is disabled while
 * [ColorSource.DYNAMIC] is active and the platform actually honours it (API 31+) — below that, dynamic
 * colour silently falls back to the palette (e.g. [ColorSource.DYNAMIC] restored from a backup on an
 * older device), so the row stays enabled and this can still fire.
 * @param onPureBlackChanged the AMOLED pure-black switch's action (design-plan §5.3).
 * @param onTodayWidgetThemeSelected the Today widget's light/dark/follow-app segmented row (design-plan
 * §5.6).
 * @param onMonthWidgetThemeSelected the Month widget's equivalent row.
 * @param onWidgetBackgroundOpacityChanged the widget background opacity slider's action, already
 * rounded to the nearest step.
 * @param onOpenHolidays the "Holiday sets" row's action.
 * @param onRequestDeleteAllData opens the first "Delete all data" confirmation.
 * @param onContinueDeleteAllData moves from the first confirmation to the final one.
 * @param onCancelDeleteAllData backs out of either confirmation.
 * @param onConfirmDeleteAllData accepted the final confirmation; erases everything.
 * @param onDismissDeleteAllDataDone dismisses the completion notice.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onWeekdayDisplaySelected: (WeekdayDisplay) -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onColorSourceSelected: (ColorSource) -> Unit,
    onPaletteSelected: (ColorPalette) -> Unit,
    onPureBlackChanged: (Boolean) -> Unit,
    onTodayWidgetThemeSelected: (WidgetTheme) -> Unit,
    onMonthWidgetThemeSelected: (WidgetTheme) -> Unit,
    onWidgetBackgroundOpacityChanged: (Int) -> Unit,
    onOpenHolidays: () -> Unit,
    modifier: Modifier = Modifier,
    onRequestDeleteAllData: () -> Unit = {},
    onContinueDeleteAllData: () -> Unit = {},
    onCancelDeleteAllData: () -> Unit = {},
    onConfirmDeleteAllData: () -> Unit = {},
    onDismissDeleteAllDataDone: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                        )
                    }
                },
                colors = yearalTopAppBarColors(),
            )
        },
    ) { padding ->
        when (state) {
            SettingsUiState.Loading -> {
                LoadingContent(Modifier.padding(padding))
            }

            is SettingsUiState.Loaded -> {
                LoadedContent(
                    state = state,
                    onWeekdayDisplaySelected = onWeekdayDisplaySelected,
                    onThemeModeSelected = onThemeModeSelected,
                    onColorSourceSelected = onColorSourceSelected,
                    onPaletteSelected = onPaletteSelected,
                    onPureBlackChanged = onPureBlackChanged,
                    onTodayWidgetThemeSelected = onTodayWidgetThemeSelected,
                    onMonthWidgetThemeSelected = onMonthWidgetThemeSelected,
                    onWidgetBackgroundOpacityChanged = onWidgetBackgroundOpacityChanged,
                    onOpenHolidays = onOpenHolidays,
                    onRequestDeleteAllData = onRequestDeleteAllData,
                    modifier = Modifier.padding(padding),
                )
                when (state.deleteAllDataStep) {
                    DeleteAllDataStep.NONE -> {
                        Unit
                    }

                    DeleteAllDataStep.CONFIRM_FIRST -> {
                        DeleteAllDataFirstDialog(onContinue = onContinueDeleteAllData, onCancel = onCancelDeleteAllData)
                    }

                    DeleteAllDataStep.CONFIRM_SECOND -> {
                        DeleteAllDataFinalDialog(onConfirm = onConfirmDeleteAllData, onCancel = onCancelDeleteAllData)
                    }

                    DeleteAllDataStep.DONE -> {
                        DeleteAllDataDoneDialog(onDismiss = onDismissDeleteAllDataDone)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier) {
    val loading = stringResource(R.string.settings_loading)
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = loading })
    }
}

@Composable
private fun LoadedContent(
    state: SettingsUiState.Loaded,
    onWeekdayDisplaySelected: (WeekdayDisplay) -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onColorSourceSelected: (ColorSource) -> Unit,
    onPaletteSelected: (ColorPalette) -> Unit,
    onPureBlackChanged: (Boolean) -> Unit,
    onTodayWidgetThemeSelected: (WidgetTheme) -> Unit,
    onMonthWidgetThemeSelected: (WidgetTheme) -> Unit,
    onWidgetBackgroundOpacityChanged: (Int) -> Unit,
    onOpenHolidays: () -> Unit,
    onRequestDeleteAllData: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
    ) {
        WeekdaySection(state.settings.weekdayDisplay, onWeekdayDisplaySelected)
        HorizontalDivider()
        AppearanceSection(
            state = state,
            onThemeModeSelected = onThemeModeSelected,
            onColorSourceSelected = onColorSourceSelected,
            onPaletteSelected = onPaletteSelected,
            onPureBlackChanged = onPureBlackChanged,
            onTodayWidgetThemeSelected = onTodayWidgetThemeSelected,
            onMonthWidgetThemeSelected = onMonthWidgetThemeSelected,
            onWidgetBackgroundOpacityChanged = onWidgetBackgroundOpacityChanged,
        )
        HorizontalDivider()
        HolidaysLinkSection(onOpenHolidays)
        HorizontalDivider()
        DataSection(onRequestDeleteAllData)
    }
}

/**
 * FEATURES W6: "Delete all data", the honest answer to "how do I erase my data"
 * (`docs/security-and-privacy.md` §2.4). Destructive by wording and icon, not colour alone — the row
 * itself opens the first of two confirmations, never erasing anything by a single tap.
 */
@Composable
private fun DataSection(onRequestDeleteAllData: () -> Unit) {
    SectionHeading(stringResource(R.string.settings_section_data))
    ListItem(
        leadingContent = {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        headlineContent = {
            Text(stringResource(R.string.settings_delete_all_data), color = MaterialTheme.colorScheme.error)
        },
        supportingContent = { Text(stringResource(R.string.settings_delete_all_data_detail)) },
        modifier =
            Modifier
                .fillMaxWidth()
                .clickableRole(onClick = onRequestDeleteAllData),
    )
}

/** A full-width, TalkBack-reachable [Role.Button] click target for a [ListItem] row. */
private fun Modifier.clickableRole(onClick: () -> Unit): Modifier =
    this.then(
        Modifier.selectable(selected = false, role = Role.Button, onClick = onClick),
    )

/** The first "Delete all data" confirmation: what will be erased. */
@Composable
private fun DeleteAllDataFirstDialog(
    onContinue: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.settings_delete_all_data_confirm1_title)) },
        text = { Text(stringResource(R.string.settings_delete_all_data_confirm1_text)) },
        confirmButton = {
            TextButton(onClick = onContinue) { Text(stringResource(R.string.settings_delete_all_data_continue)) }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.settings_cancel)) } },
    )
}

/** The final, unmistakably destructive "Delete all data" confirmation: this cannot be undone. */
@Composable
private fun DeleteAllDataFinalDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.settings_delete_all_data_confirm2_title)) },
        text = { Text(stringResource(R.string.settings_delete_all_data_confirm2_text)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(R.string.settings_delete_all_data),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.settings_cancel)) } },
    )
}

/** Confirms the erase finished. */
@Composable
private fun DeleteAllDataDoneDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_delete_all_data_done_title)) },
        text = { Text(stringResource(R.string.settings_delete_all_data_done_text)) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_ok)) } },
    )
}

/** FEATURES W1: `BOTH` / `ACTUAL` / `NOMINAL`, with the §4.1 reminder above the options. */
@Composable
private fun WeekdaySection(
    selected: WeekdayDisplay,
    onSelected: (WeekdayDisplay) -> Unit,
) {
    SectionHeading(stringResource(R.string.settings_section_weekdays))
    SectionInfo(stringResource(R.string.settings_weekday_info))
    Column(modifier = Modifier.selectableGroup()) {
        // Listed in the order of FEATURES W1: both (default) / actual only / nominal IFC only.
        RadioRow(
            title = stringResource(R.string.settings_weekday_both),
            detail = stringResource(R.string.settings_weekday_both_detail),
            selected = selected == WeekdayDisplay.BOTH,
            onClick = { onSelected(WeekdayDisplay.BOTH) },
        )
        RadioRow(
            title = stringResource(R.string.settings_weekday_actual),
            detail = stringResource(R.string.settings_weekday_actual_detail),
            selected = selected == WeekdayDisplay.ACTUAL,
            onClick = { onSelected(WeekdayDisplay.ACTUAL) },
        )
        RadioRow(
            title = stringResource(R.string.settings_weekday_nominal),
            detail = stringResource(R.string.settings_weekday_nominal_detail),
            selected = selected == WeekdayDisplay.NOMINAL,
            onClick = { onSelected(WeekdayDisplay.NOMINAL) },
        )
    }
}

/**
 * The Appearance section (`docs/design-plan.md` §5; FEATURES W2, W6): theme mode, colour source,
 * palette, pure black, and the two widgets' light/dark override plus background opacity — replaces the
 * old Theme section, which only had the mode radios and a dynamic-colour switch.
 */
@Composable
private fun AppearanceSection(
    state: SettingsUiState.Loaded,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onColorSourceSelected: (ColorSource) -> Unit,
    onPaletteSelected: (ColorPalette) -> Unit,
    onPureBlackChanged: (Boolean) -> Unit,
    onTodayWidgetThemeSelected: (WidgetTheme) -> Unit,
    onMonthWidgetThemeSelected: (WidgetTheme) -> Unit,
    onWidgetBackgroundOpacityChanged: (Int) -> Unit,
) {
    SectionHeading(stringResource(R.string.settings_section_appearance))
    ThemeModeGroup(state.settings.themeMode, onThemeModeSelected)
    ColorSourceRow(state, onColorSourceSelected)
    PaletteRow(
        settings = state.settings,
        enabled = state.settings.colorSource == ColorSource.BRAND || !state.dynamicColorSupported,
        onSelected = onPaletteSelected,
    )
    SwitchRow(
        title = stringResource(R.string.settings_pure_black),
        detail = stringResource(R.string.settings_pure_black_detail),
        checked = state.settings.pureBlack,
        enabled = true,
        onCheckedChange = onPureBlackChanged,
    )
    GroupHeading(stringResource(R.string.settings_section_widgets))
    WidgetThemeRow(
        label = stringResource(R.string.settings_widget_today_label),
        selected = state.settings.todayWidgetTheme,
        onSelected = onTodayWidgetThemeSelected,
        testTagPrefix = SettingsTestTags.TODAY_WIDGET_THEME_PREFIX,
    )
    WidgetThemeRow(
        label = stringResource(R.string.settings_widget_month_label),
        selected = state.settings.monthWidgetTheme,
        onSelected = onMonthWidgetThemeSelected,
        testTagPrefix = SettingsTestTags.MONTH_WIDGET_THEME_PREFIX,
    )
    WidgetBackgroundSlider(
        percent = state.settings.widgetBackgroundOpacity,
        onChange = onWidgetBackgroundOpacityChanged,
    )
}

/**
 * The System / Light / Dark radio group, each option with a one-line explanation. Each row also
 * carries a [SettingsTestTags.THEME_MODE_PREFIX] test tag: the widget-theme rows below reuse the
 * literal words "Light" and "Dark" for their own segments, so a test can't tell these three rows apart
 * by visible text alone once the whole screen is on screen together.
 */
@Composable
private fun ThemeModeGroup(
    selected: ThemeMode,
    onSelected: (ThemeMode) -> Unit,
) {
    Column(modifier = Modifier.selectableGroup()) {
        RadioRow(
            title = stringResource(R.string.settings_theme_system),
            detail = stringResource(R.string.settings_theme_system_detail),
            selected = selected == ThemeMode.SYSTEM,
            onClick = { onSelected(ThemeMode.SYSTEM) },
            testTag = SettingsTestTags.THEME_MODE_PREFIX + ThemeMode.SYSTEM.name,
        )
        RadioRow(
            title = stringResource(R.string.settings_theme_light),
            detail = stringResource(R.string.settings_theme_light_detail),
            selected = selected == ThemeMode.LIGHT,
            onClick = { onSelected(ThemeMode.LIGHT) },
            testTag = SettingsTestTags.THEME_MODE_PREFIX + ThemeMode.LIGHT.name,
        )
        RadioRow(
            title = stringResource(R.string.settings_theme_dark),
            detail = stringResource(R.string.settings_theme_dark_detail),
            selected = selected == ThemeMode.DARK,
            onClick = { onSelected(ThemeMode.DARK) },
            testTag = SettingsTestTags.THEME_MODE_PREFIX + ThemeMode.DARK.name,
        )
    }
}

/**
 * Where the colour scheme comes from (design-plan §5.1): a two-way [ColorSource] segmented row,
 * replacing the earlier boolean switch. The `DYNAMIC` segment is disabled below API 31, with the same
 * "not available" line the old switch used to show.
 */
@Composable
private fun ColorSourceRow(
    state: SettingsUiState.Loaded,
    onSelected: (ColorSource) -> Unit,
) {
    GroupHeading(stringResource(R.string.settings_color_source_label))
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        ColorSource.entries.forEachIndexed { index, option ->
            val enabled = option != ColorSource.DYNAMIC || state.dynamicColorSupported
            SegmentedButton(
                selected = state.settings.colorSource == option,
                onClick = { onSelected(option) },
                enabled = enabled,
                shape = SegmentedButtonDefaults.itemShape(index = index, count = ColorSource.entries.size),
                modifier = Modifier.heightIn(min = MinTouchTarget),
            ) {
                Text(text = stringResource(option.labelRes()))
            }
        }
    }
    if (!state.dynamicColorSupported) {
        SectionInfo(stringResource(R.string.settings_color_source_unavailable))
    }
}

private fun ColorSource.labelRes(): Int =
    when (this) {
        ColorSource.BRAND -> R.string.settings_color_source_brand
        ColorSource.DYNAMIC -> R.string.settings_color_source_dynamic
    }

/** Shared with [PalettePreviewStrip], whose content description names the selected palette. */
internal fun ColorPalette.labelRes(): Int =
    when (this) {
        ColorPalette.TEAL -> R.string.settings_palette_teal
        ColorPalette.SOL -> R.string.settings_palette_sol
        ColorPalette.NIGHT -> R.string.settings_palette_night
        ColorPalette.MOSS -> R.string.settings_palette_moss
        ColorPalette.ROSE -> R.string.settings_palette_rose
        ColorPalette.INK -> R.string.settings_palette_ink
    }

/**
 * The six curated [ColorPalette] swatches (design-plan §5.2), each a 48dp circle filled with the
 * palette's light `primary` and a smaller inner dot of its light `tertiary`, labelled by name; the
 * selected swatch shows a check mark. The whole row is disabled — dimmed and not clickable, with an
 * explanatory subtitle — while [enabled] is false, i.e. while [ColorSource.DYNAMIC] is the active
 * colour source *and* the platform actually honours it. Below API 31, dynamic colour is unavailable and
 * the theme falls back to the palette regardless of the stored [ColorSource] (e.g. [ColorSource.DYNAMIC]
 * restored from a backup made on a newer device), so a palette choice still has a visible effect there
 * and the caller passes [enabled] as `true` in that case. Beneath the swatches, [PalettePreviewStrip]
 * shows the live effect of [settings] regardless of [enabled] — design-plan §4.8's wave-3 follow-up,
 * so a user sees the palette (or Material You) before committing to it.
 */
@Composable
private fun PaletteRow(
    settings: UserSettings,
    enabled: Boolean,
    onSelected: (ColorPalette) -> Unit,
) {
    GroupHeading(stringResource(R.string.settings_palette_label))
    if (!enabled) {
        SectionInfo(stringResource(R.string.settings_palette_disabled_detail))
    }
    FlowRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .selectableGroup()
                .alpha(if (enabled) 1f else DISABLED_ALPHA),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ColorPalette.entries.forEach { palette ->
            PaletteSwatch(
                palette = palette,
                selected = palette == settings.palette,
                enabled = enabled,
                onClick = { onSelected(palette) },
            )
        }
    }
    PalettePreviewStrip(settings = settings, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun PaletteSwatch(
    palette: ColorPalette,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val light = palette.colorSchemes().light
    val label = stringResource(palette.labelRes())
    val checkTint = if (light.primary.luminance() > CHECK_TINT_LUMINANCE_THRESHOLD) Color.Black else Color.White
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier.selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            ),
    ) {
        Box(
            modifier =
                Modifier
                    .padding(4.dp)
                    .size(PaletteSwatchSize)
                    .clip(CircleShape)
                    .background(light.primary),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = checkTint)
            } else {
                Box(
                    modifier =
                        Modifier
                            .size(PaletteSwatchInnerDotSize)
                            .clip(CircleShape)
                            .background(light.tertiary),
                )
            }
        }
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}

/**
 * A three-way Follow app / Light / Dark segmented row for one widget's [WidgetTheme] (design-plan
 * §5.6). [testTagPrefix] disambiguates the Today and Month rows' identical "Light"/"Dark" segments
 * (and [ThemeModeGroup]'s own) for tests, since none of them are unique text on a screen that shows
 * every one of them at once.
 */
@Composable
private fun WidgetThemeRow(
    label: String,
    selected: WidgetTheme,
    onSelected: (WidgetTheme) -> Unit,
    testTagPrefix: String,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 4.dp),
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        WidgetTheme.entries.forEachIndexed { index, option ->
            SegmentedButton(
                selected = selected == option,
                onClick = { onSelected(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = WidgetTheme.entries.size),
                modifier = Modifier.heightIn(min = MinTouchTarget).testTag(testTagPrefix + option.name),
            ) {
                Text(text = stringResource(option.labelRes()))
            }
        }
    }
}

private fun WidgetTheme.labelRes(): Int =
    when (this) {
        WidgetTheme.FOLLOW_APP -> R.string.settings_widget_theme_follow
        WidgetTheme.LIGHT -> R.string.settings_widget_theme_light
        WidgetTheme.DARK -> R.string.settings_widget_theme_dark
    }

/**
 * The widget background opacity slider, 0–100% in steps of 5 (design-plan §5.6). [onChange] receives
 * the value already rounded to the nearest step, since Material 3's [Slider] snaps the drag position
 * before invoking its callback whenever [Slider]'s `steps` is nonzero.
 *
 * The thumb tracks an in-drag [mutableIntStateOf] rather than binding straight to [percent], so it
 * moves smoothly on every `onValueChange` callback during a drag instead of waiting for a round trip
 * through the DataStore-backed [percent]; that local value is re-seeded from [percent] whenever the
 * stored value itself changes (i.e. it is keyed on it via `remember(percent)`). [onChange] — which
 * persists — fires only from `onValueChangeFinished`, once per completed drag or discrete step, instead
 * of on every intermediate `onValueChange`, which would otherwise write to the store up to 20 times for
 * one drag from 0 to 100.
 */
@Composable
private fun WidgetBackgroundSlider(
    percent: Int,
    onChange: (Int) -> Unit,
) {
    var dragValue by remember(percent) { mutableIntStateOf(percent) }
    val accessibleLabel = stringResource(R.string.settings_widget_background_content_description)
    Text(
        text = stringResource(R.string.settings_widget_background_label, dragValue),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(horizontal = 16.dp).padding(top = 12.dp),
    )
    Slider(
        value = dragValue.toFloat(),
        onValueChange = { dragValue = it.roundToInt() },
        onValueChangeFinished = { onChange(dragValue) },
        valueRange = MIN_WIDGET_OPACITY..MAX_WIDGET_OPACITY,
        steps = WIDGET_OPACITY_STEPS,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .semantics { contentDescription = accessibleLabel }
                .testTag(SettingsTestTags.WIDGET_BACKGROUND_SLIDER),
    )
}

/**
 * Test tags for controls [SettingsScreenTest] can't reach reliably by text or role alone — mostly
 * because [ThemeModeGroup] and both [WidgetThemeRow]s legitimately show the same words ("Light",
 * "Dark") on the same screen.
 */
internal object SettingsTestTags {
    /** The widget background opacity [Slider], which carries no text or content description of its own. */
    const val WIDGET_BACKGROUND_SLIDER: String = "settings:widgetBackgroundSlider"

    /** Prefix of each [ThemeModeGroup] row's tag; suffixed with the [ThemeMode]'s enum name. */
    const val THEME_MODE_PREFIX: String = "settings:themeMode:"

    /** Prefix of each Today-widget segment's tag; suffixed with the [WidgetTheme]'s enum name. */
    const val TODAY_WIDGET_THEME_PREFIX: String = "settings:todayWidgetTheme:"

    /** Prefix of each Month-widget segment's tag; suffixed with the [WidgetTheme]'s enum name. */
    const val MONTH_WIDGET_THEME_PREFIX: String = "settings:monthWidgetTheme:"
}

/** A small, muted heading for a control group inside a section (e.g. "Colour source", "Widgets"). */
@Composable
private fun GroupHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

/** The 48dp touch-target floor (docs/ARCHITECTURE.md §4 "Accessibility"). */
private val MinTouchTarget = 48.dp
private val PaletteSwatchSize = 48.dp
private val PaletteSwatchInnerDotSize = 16.dp
private const val CHECK_TINT_LUMINANCE_THRESHOLD = 0.5f
private const val MIN_WIDGET_OPACITY = 0f
private const val MAX_WIDGET_OPACITY = 100f

/** Steps between 0 and 100 in increments of 5: 21 stops (0, 5, 10, …, 100), 19 of them between the ends. */
private const val WIDGET_OPACITY_STEPS = 19

/**
 * FEATURES H5: a single row linking to the Holidays screen, which now owns browsing and toggling every
 * bundled set (ROADMAP M6 T2). This replaces the per-pack switches that used to live in this section —
 * duplicating them here and there was a maintenance trap, and both screens read the same
 * [io.github.chrisjmendoza.yearal.core.domain.settings.SettingsRepository.settings] flow regardless of
 * which one changes it.
 */
@Composable
private fun HolidaysLinkSection(onOpenHolidays: () -> Unit) {
    SectionHeading(stringResource(R.string.settings_section_holidays))
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_holidays_row_title)) },
        supportingContent = { Text(stringResource(R.string.settings_holidays_row_detail)) },
        modifier = Modifier.fillMaxWidth().clickableRole(onClick = onOpenHolidays),
    )
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
private fun SectionInfo(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

/**
 * One option of a radio group. The whole row is the selectable so the tap target spans its width and
 * TalkBack reads title, detail and state as one item; the [RadioButton] itself is decorative.
 *
 * @param testTag an optional test tag, for a group whose options' visible text isn't unique on its own
 * once the rest of the screen is showing (e.g. [ThemeModeGroup], whose "Light"/"Dark" text also names
 * [WidgetThemeRow] segments elsewhere on the same screen).
 */
@Composable
private fun RadioRow(
    title: String,
    detail: String?,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String? = null,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (detail != null) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * A list row with a trailing [Switch]. The row is the toggleable (one semantics node, full-width tap
 * target); the switch is decorative. A disabled row keeps its stored value visible but dims its text.
 */
@Composable
private fun SwitchRow(
    title: String,
    detail: String?,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors =
        if (enabled) {
            ListItemDefaults.colors()
        } else {
            ListItemDefaults.colors(
                headlineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA),
                supportingColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = DISABLED_ALPHA),
            )
        }
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = detail?.let { { Text(it) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) },
        colors = colors,
        modifier =
            Modifier.toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
    )
}

/** Material's opacity for disabled content. */
private const val DISABLED_ALPHA = 0.38f

// Previews — Roborazzi's preview scanner captures every @Preview once docs/ROADMAP.md M2 T10 records
// the goldens; dynamic colour is off for determinism.

@Preview(name = "Light", showBackground = true)
@Composable
internal fun SettingsScreenLightPreview() {
    SettingsPreview(darkTheme = false)
}

@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun SettingsScreenDarkPreview() {
    SettingsPreview(darkTheme = true)
}

@Preview(name = "Font 2.0", showBackground = true, fontScale = 2f)
@Composable
internal fun SettingsScreenLargeFontPreview() {
    SettingsPreview(darkTheme = false)
}

/** A device below API 31: the dynamic-colour row is disabled with its "not available" subtitle. */
@Preview(name = "No dynamic colour", showBackground = true)
@Composable
internal fun SettingsScreenNoDynamicColorPreview() {
    SettingsPreview(darkTheme = false, dynamicColorSupported = false)
}

@Composable
private fun SettingsPreview(
    darkTheme: Boolean,
    dynamicColorSupported: Boolean = true,
) {
    IfcTheme(darkTheme = darkTheme, dynamicColor = false) {
        SettingsScreen(
            state =
                SettingsUiState.Loaded(
                    settings = UserSettings.DEFAULT,
                    dynamicColorSupported = dynamicColorSupported,
                ),
            onBack = {},
            onWeekdayDisplaySelected = {},
            onThemeModeSelected = {},
            onColorSourceSelected = {},
            onPaletteSelected = {},
            onPureBlackChanged = {},
            onTodayWidgetThemeSelected = {},
            onMonthWidgetThemeSelected = {},
            onWidgetBackgroundOpacityChanged = {},
            onOpenHolidays = {},
        )
    }
}
