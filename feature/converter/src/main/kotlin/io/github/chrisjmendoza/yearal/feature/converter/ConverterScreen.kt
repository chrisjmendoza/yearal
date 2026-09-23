package io.github.chrisjmendoza.yearal.feature.converter

import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.chrisjmendoza.yearal.core.calendar.IfcDate
import io.github.chrisjmendoza.yearal.core.designsystem.format.rememberIfcDateFormatter
import io.github.chrisjmendoza.yearal.core.designsystem.picker.DatePickerRange
import io.github.chrisjmendoza.yearal.core.designsystem.picker.GregorianDatePickerDialog
import io.github.chrisjmendoza.yearal.core.designsystem.picker.IfcDatePicker
import io.github.chrisjmendoza.yearal.core.designsystem.picker.IfcDatePickerValue
import io.github.chrisjmendoza.yearal.core.designsystem.theme.Dimens
import io.github.chrisjmendoza.yearal.core.designsystem.theme.IfcTheme
import io.github.chrisjmendoza.yearal.core.designsystem.theme.YearalTheme
import io.github.chrisjmendoza.yearal.core.designsystem.theme.yearalTopAppBarColors
import io.github.chrisjmendoza.yearal.core.navigation.ConverterKey
import io.github.chrisjmendoza.yearal.core.navigation.DayKey
import io.github.chrisjmendoza.yearal.core.navigation.Navigator
import kotlinx.coroutines.launch
import java.time.LocalDate
import io.github.chrisjmendoza.yearal.core.designsystem.R as DesignSystemR

// 12dp at the sides leaves 336dp on a 360dp phone: exactly seven 48dp day columns in the IFC picker.
private val ScreenHorizontalPadding = 12.dp
private val ScreenBottomPadding = 24.dp
private val SectionSpacing = 16.dp
private val LineSpacing = 4.dp
private val BlockPadding = 16.dp
private val ActionSpacing = 8.dp
private val MinTouchTarget = 48.dp
private val IntercalaryIconSpacing = 4.dp

/**
 * The converter (docs/FEATURES.md D1, D2, D4): collects [ConverterViewModel.uiState] with the lifecycle
 * and renders it through the stateless [ConverterScreen]. This is the composable `:app` places behind
 * [ConverterKey]. Copy and share happen here, where a `Context` is at hand; "Open day" pushes the
 * converted day's [DayKey] through [navigator] (CLAUDE.md rule 10).
 *
 * @param key the entry's key; its `prefillEpochDay` is the initial date when present and in range.
 * @param navigator receives the [DayKey] of "Open day".
 * @param modifier applied to the screen's root [Scaffold].
 */
@Composable
fun ConverterRoute(
    key: ConverterKey,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: ConverterViewModel =
        hiltViewModel<ConverterViewModel, ConverterViewModel.Factory>(
            creationCallback = { factory -> factory.create(key) },
        ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipLabel = stringResource(R.string.converter_clip_label)
    val copied = stringResource(R.string.converter_copied)
    val shareUnavailable = stringResource(R.string.converter_share_unavailable)
    ConverterScreen(
        state = state,
        onDirectionChange = viewModel::setDirection,
        onGregorianDateChange = viewModel::setGregorianDate,
        onIfcInputChange = viewModel::setIfcInput,
        onResetToToday = viewModel::resetToToday,
        onCopy = { text ->
            // Android 13 and later confirm a copy themselves; a second confirmation would be noise.
            val confirm =
                copyConversion(context, clipLabel, text) && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            if (confirm) scope.launch { snackbarHostState.showSnackbar(copied) }
        },
        onShare = { text ->
            if (!shareConversion(context, text)) scope.launch { snackbarHostState.showSnackbar(shareUnavailable) }
        },
        onOpenDay = { day -> navigator.navigate(DayKey(day.toEpochDay())) },
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

/**
 * The stateless converter — the unit for previews, screenshot and Compose tests (docs/ARCHITECTURE.md
 * §4 "State management"). From the top: the direction switch, the active input (a button that opens
 * the Material date picker limited to 1583..9999, or the [IfcDatePicker] with Year Day always and Leap
 * Day only in leap years), and the result: both dates in full, the numeric `IFC YYYY-MM-DD` form, the
 * IFC weekday and the actual weekday on separately labelled lines (spec §4.1; `no IFC weekday` on
 * intercalary days), day and week, the proleptic note for early years, and Copy / Share / Open day.
 * An input that is not a convertible date shows a message instead of a result, and no actions.
 *
 * Only whether the date dialog is open is kept here (saved across recreation); everything else is
 * [state]. Opts in to the Material 3 experimental marker only because `TopAppBar`'s default arguments
 * still carry it.
 *
 * @param onGregorianDateChange receives the date confirmed in the Gregorian picker.
 * @param onIfcInputChange receives the IFC picker's value after each edit.
 * @param onResetToToday the app bar's "Today" action, shown once a date has been chosen.
 * @param onCopy receives the conversion as text; it always contains "IFC" and the Gregorian date.
 * @param onShare receives the same text as [onCopy].
 * @param onOpenDay receives the converted day as a Gregorian date.
 * @param snackbarHostState shows the caller's confirmations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterScreen(
    state: ConverterUiState,
    onDirectionChange: (ConversionDirection) -> Unit,
    onGregorianDateChange: (LocalDate) -> Unit,
    onIfcInputChange: (IfcDatePickerValue) -> Unit,
    onResetToToday: () -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.converter_title)) },
                actions = {
                    if (state is ConverterUiState.Loaded && !state.followsToday) {
                        TextButton(onClick = onResetToToday) { Text(stringResource(R.string.converter_today)) }
                    }
                },
                colors = yearalTopAppBarColors(),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (state) {
            ConverterUiState.Loading -> {
                LoadingContent(Modifier.padding(padding))
            }

            is ConverterUiState.Loaded -> {
                LoadedContent(
                    state = state,
                    onDirectionChange = onDirectionChange,
                    onGregorianDateChange = onGregorianDateChange,
                    onIfcInputChange = onIfcInputChange,
                    onCopy = onCopy,
                    onShare = onShare,
                    onOpenDay = onOpenDay,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier) {
    val loading = stringResource(R.string.converter_loading)
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = loading })
    }
}

@Composable
private fun LoadedContent(
    state: ConverterUiState.Loaded,
    onDirectionChange: (ConversionDirection) -> Unit,
    onGregorianDateChange: (LocalDate) -> Unit,
    onIfcInputChange: (IfcDatePickerValue) -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = ScreenHorizontalPadding, end = ScreenHorizontalPadding, bottom = ScreenBottomPadding),
        verticalArrangement = Arrangement.spacedBy(SectionSpacing),
    ) {
        DirectionSwitch(direction = state.direction, onDirectionChange = onDirectionChange)
        when (state.direction) {
            ConversionDirection.GREGORIAN_TO_IFC -> GregorianInput(state, onGregorianDateChange)
            ConversionDirection.IFC_TO_GREGORIAN -> IfcInput(state.ifcInput, onIfcInputChange)
        }
        SectionHeading(stringResource(R.string.converter_result_heading))
        when (val result = state.result) {
            ConversionResult.Invalid -> InvalidResult()
            is ConversionResult.Converted -> ConvertedResult(state.direction, result, onCopy, onShare, onOpenDay)
        }
    }
}

/**
 * FEATURES D1: the two directions as one single-choice segmented row, plus a swap icon button beside
 * it (docs/design-plan.md §4.6) that flips [direction] the same way tapping the other segment does —
 * the review found the previous glyph read as "refresh" rather than "swap" (ROADMAP R5).
 */
@Composable
private fun DirectionSwitch(
    direction: ConversionDirection,
    onDirectionChange: (ConversionDirection) -> Unit,
) {
    SectionHeading(stringResource(R.string.converter_direction_heading))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ActionSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
            ConversionDirection.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = direction == option,
                    onClick = { onDirectionChange(option) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = ConversionDirection.entries.size),
                    modifier = Modifier.heightIn(min = MinTouchTarget),
                ) {
                    Text(text = stringResource(option.labelRes()))
                }
            }
        }
        IconButton(
            onClick = { onDirectionChange(direction.opposite()) },
            modifier = Modifier.size(MinTouchTarget),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_swap),
                contentDescription = stringResource(R.string.converter_swap_direction),
            )
        }
    }
}

/** The segmented button's label for this direction. */
private fun ConversionDirection.labelRes(): Int =
    when (this) {
        ConversionDirection.GREGORIAN_TO_IFC -> R.string.converter_direction_gregorian_to_ifc
        ConversionDirection.IFC_TO_GREGORIAN -> R.string.converter_direction_ifc_to_gregorian
    }

/** The other direction — swapping shows the same result the user would get by tapping that segment. */
private fun ConversionDirection.opposite(): ConversionDirection =
    when (this) {
        ConversionDirection.GREGORIAN_TO_IFC -> ConversionDirection.IFC_TO_GREGORIAN
        ConversionDirection.IFC_TO_GREGORIAN -> ConversionDirection.GREGORIAN_TO_IFC
    }

/** The Gregorian input: a button showing the date, which opens the Material picker (1583..9999). */
@Composable
private fun GregorianInput(
    state: ConverterUiState.Loaded,
    onGregorianDateChange: (LocalDate) -> Unit,
) {
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    val description = stringResource(R.string.converter_gregorian_input_description, state.gregorianInputLabel)
    SectionHeading(stringResource(R.string.converter_gregorian_input_heading))
    OutlinedButton(
        onClick = { pickerOpen = true },
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = MinTouchTarget)
                .semantics { contentDescription = description },
    ) {
        Text(text = state.gregorianInputLabel)
    }
    if (pickerOpen) {
        GregorianDatePickerDialog(
            initialDate = state.gregorianInput,
            onConfirm = { date ->
                pickerOpen = false
                onGregorianDateChange(date)
            },
            onDismiss = { pickerOpen = false },
        )
    }
}

/** The IFC input: the reusable [IfcDatePicker] — Year Day always offered, Leap Day only in leap years. */
@Composable
private fun IfcInput(
    value: IfcDatePickerValue,
    onIfcInputChange: (IfcDatePickerValue) -> Unit,
) {
    SectionHeading(stringResource(R.string.converter_ifc_input_heading))
    IfcDatePicker(value = value, onValueChange = onIfcInputChange)
}

/**
 * Shown instead of a result when the active input is not a date the converter accepts (spec §7.1); no
 * actions follow it. Announced as a polite live region so a screen reader reports it as the input
 * changes, without interrupting whatever the user is doing.
 */
@Composable
private fun InvalidResult() {
    Text(
        text =
            stringResource(
                R.string.converter_invalid,
                DatePickerRange.MIN_YEAR.toString(),
                DatePickerRange.MAX_YEAR.toString(),
            ),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

/**
 * The result card (docs/design-plan.md §4.6): a `YearalTheme.colors.heroContainer` card, the same
 * component as the Today hero, "so the answer looks like an answer". Inside it: the intercalary icon on Year Day
 * and Leap Day, both dates as eyebrow-labelled ("IFC" / "Gregorian") pairs with the converted one shown
 * larger, the numeric form with its `IFC` prefix, the weekday block (its sage container kept) and day
 * and week. The proleptic note and the actions sit below the card. **Copy and Share send the identical
 * text**, built from `R.string.converter_share_text` as
 * `IFC {ifcLong} ({numeric}) = Gregorian {gregorianLong}` — e.g. `IFC September 8, 2026
 * (IFC 2026-10-08) = Gregorian Thursday, September 17, 2026` — so the shared text always carries the
 * `IFC` marker and the Gregorian date (CLAUDE.md rule 5) before it reaches [copyConversion] or
 * [shareConversion].
 */
@Composable
private fun ConvertedResult(
    direction: ConversionDirection,
    result: ConversionResult.Converted,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
) {
    val shareText = stringResource(R.string.converter_share_text, result.ifcLong, result.numeric, result.gregorianLong)

    Surface(
        color = YearalTheme.colors.heroContainer,
        contentColor = YearalTheme.colors.onHero,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(Dimens.SpaceL),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
        ) {
            // The answer (what the direction converted to) sits first and larger; the restated input
            // follows, smaller — the same order the plain-text lines used before this card existed, so
            // a newcomer always sees the answer first regardless of which calendar they typed into.
            when (direction) {
                ConversionDirection.GREGORIAN_TO_IFC -> {
                    IfcField(result, emphasized = true)
                    GregorianField(result, emphasized = false)
                }

                ConversionDirection.IFC_TO_GREGORIAN -> {
                    GregorianField(result, emphasized = true)
                    IfcField(result, emphasized = false)
                }
            }
            WeekdayBlock(result)
            Text(text = result.dayAndWeek, style = MaterialTheme.typography.bodyLarge)
        }
    }
    if (result.showProlepticNote) {
        Text(
            text = stringResource(R.string.converter_proleptic_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ActionSpacing),
    ) {
        Button(onClick = { onCopy(shareText) }, modifier = Modifier.heightIn(min = MinTouchTarget)) {
            Icon(
                painter = painterResource(R.drawable.ic_copy),
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.converter_copy))
        }
        Button(onClick = { onShare(shareText) }, modifier = Modifier.heightIn(min = MinTouchTarget)) {
            Icon(
                imageVector = Icons.Filled.Share,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.converter_share))
        }
        OutlinedButton(
            onClick = { onOpenDay(result.gregorianDate) },
            modifier = Modifier.heightIn(min = MinTouchTarget),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_open),
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.converter_open_day))
        }
    }
}

/**
 * The IFC side of the result card: the intercalary icon on Year Day and Leap Day, the "IFC" eyebrow and
 * [ConversionResult.Converted.ifcLong], then the numeric `IFC` form — the numeric line travels with this
 * field, not with the card as a whole, so it stays adjacent to the value it restates when [IfcField] and
 * [GregorianField] swap order by direction.
 */
@Composable
private fun IfcField(
    result: ConversionResult.Converted,
    emphasized: Boolean,
) {
    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(IntercalaryIconSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (result.date.isIntercalary) {
                Icon(
                    painter = painterResource(DesignSystemR.drawable.ic_intercalary),
                    contentDescription = null,
                    tint = YearalTheme.colors.intercalary,
                )
            }
            ResultField(
                eyebrow = stringResource(R.string.converter_eyebrow_ifc),
                value = result.ifcLong,
                description = stringResource(R.string.converter_result_ifc, result.ifcLong),
                emphasized = emphasized,
            )
        }
        Text(text = result.numeric, style = MaterialTheme.typography.bodyLarge)
    }
}

/** The Gregorian side of the result card: the "Gregorian" eyebrow and [ConversionResult.Converted.gregorianLong]. */
@Composable
private fun GregorianField(
    result: ConversionResult.Converted,
    emphasized: Boolean,
) {
    ResultField(
        eyebrow = stringResource(R.string.converter_eyebrow_gregorian),
        value = result.gregorianLong,
        description = stringResource(R.string.converter_result_gregorian, result.gregorianLong),
        emphasized = emphasized,
    )
}

/**
 * One eyebrow-labelled value in the result card (docs/design-plan.md §3.1 "Typography", §4.6): a small
 * uppercase caption ("IFC", "Gregorian") naming the calendar, then the date — larger
 * ([MaterialTheme.typography.headlineSmall]) when [emphasized] (the direction's answer), smaller
 * ([MaterialTheme.typography.titleMedium]) otherwise (the input restated). The pair merges into one
 * semantics node carrying [description] ("IFC: September 8, 2026"), the same wording the plain text
 * line used before this card existed, so a screen reader still hears the eyebrow and the value as one
 * fact rather than two.
 */
@Composable
private fun ResultField(
    eyebrow: String,
    value: String,
    description: String,
    emphasized: Boolean,
) {
    val locale = LocalLocale.current.platformLocale
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Text(text = eyebrow.uppercase(locale), style = MaterialTheme.typography.labelSmall)
        Text(
            text = value,
            style = if (emphasized) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
        )
    }
}

/**
 * Both weekdays, each line labelled by the formatter (spec §4.1 item 4) and merged into one spoken
 * description, "IFC Sunday, actual Thursday" (§4.1 item 7), so neither can be mistaken for the other.
 * The sage container is [YearalTheme]'s weekday-nominal token pair rather than a raw Material role, so
 * it stays in step with the rest of the app's "IFC weekday" surfaces (docs/design-plan.md §4.6); the
 * actual-weekday line keeps its own, still-legible token instead of a plain grey (design-plan §4.8).
 */
@Composable
private fun WeekdayBlock(result: ConversionResult.Converted) {
    Surface(
        color = YearalTheme.colors.weekdayNominalContainer,
        contentColor = YearalTheme.colors.onWeekdayNominalContainer,
        shape = MaterialTheme.shapes.medium,
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { contentDescription = result.weekdaysDescription },
    ) {
        Column(
            modifier = Modifier.padding(BlockPadding),
            verticalArrangement = Arrangement.spacedBy(LineSpacing),
        ) {
            Text(text = result.nominalWeekday, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = result.actualWeekday,
                style = MaterialTheme.typography.bodyLarge,
                color = YearalTheme.colors.weekdayActualText,
            )
        }
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.semantics { heading() },
    )
}

// Previews — one per date shape (CLAUDE.md rule 6), both directions, the invalid state and 200% font.
// Dynamic colour is off for determinism.

/** Gregorian 2026-09-17 → IFC September 8, 2026 (the spec's worked example, §4.1). */
@Preview(name = "Gregorian to IFC", showBackground = true)
@Preview(name = "Gregorian to IFC, dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Gregorian to IFC, font 2.0", showBackground = true, fontScale = 2f)
@Composable
internal fun ConverterGregorianToIfcPreview() {
    ConverterPreview(ConversionDirection.GREGORIAN_TO_IFC, LocalDate.of(2026, 9, 17))
}

/** Leap Day 2028 → Gregorian Saturday, June 17, 2028. */
@Preview(name = "IFC to Gregorian, Leap Day", showBackground = true, heightDp = 1200)
@Preview(name = "IFC to Gregorian, Leap Day, font 2.0", showBackground = true, heightDp = 2000, fontScale = 2f)
@Composable
internal fun ConverterLeapDayPreview() {
    ConverterPreview(ConversionDirection.IFC_TO_GREGORIAN, LocalDate.of(2028, 6, 17))
}

/** Year Day 1900 → Gregorian Monday, December 31, 1900, with the proleptic note. */
@Preview(name = "Year Day with proleptic note", showBackground = true)
@Preview(name = "Year Day, dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Year Day, font 2.0", showBackground = true, heightDp = 1400, fontScale = 2f)
@Composable
internal fun ConverterYearDayPreview() {
    ConverterPreview(ConversionDirection.GREGORIAN_TO_IFC, LocalDate.of(1900, 12, 31))
}

/** The IFC year is being typed: no result, no actions. */
@Preview(name = "Invalid IFC year", showBackground = true, heightDp = 1200)
@Composable
internal fun ConverterInvalidPreview() {
    ConverterPreview(
        direction = ConversionDirection.IFC_TO_GREGORIAN,
        day = LocalDate.of(2026, 9, 17),
        draft = IfcDatePickerValue.of(IfcDate.from(LocalDate.of(2026, 9, 17))).withYearText("158"),
    )
}

@Composable
private fun ConverterPreview(
    direction: ConversionDirection,
    day: LocalDate,
    draft: IfcDatePickerValue? = null,
) {
    IfcTheme(dynamicColor = false) {
        ConverterScreen(
            state =
                buildConverterUiState(
                    today = LocalDate.of(2026, 9, 17),
                    input = ConverterInput(direction = direction, chosen = day, ifcDraft = draft),
                    formatter = rememberIfcDateFormatter(),
                ),
            onDirectionChange = {},
            onGregorianDateChange = {},
            onIfcInputChange = {},
            onResetToToday = {},
            onCopy = {},
            onShare = {},
            onOpenDay = {},
        )
    }
}
