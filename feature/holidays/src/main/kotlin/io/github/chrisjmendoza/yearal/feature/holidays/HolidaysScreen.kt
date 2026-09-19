package io.github.chrisjmendoza.yearal.feature.holidays

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.chrisjmendoza.yearal.core.designsystem.format.IfcDateFormatter
import io.github.chrisjmendoza.yearal.core.designsystem.format.rememberIfcDateFormatter
import io.github.chrisjmendoza.yearal.core.designsystem.theme.IfcTheme
import io.github.chrisjmendoza.yearal.core.navigation.DayKey
import io.github.chrisjmendoza.yearal.core.navigation.Navigator

/**
 * The Holidays screen (docs/FEATURES.md H1, H2, H3, H5, H7; ROADMAP M6 T2): collects
 * [HolidaysViewModel.uiState] with the lifecycle and renders it through the stateless
 * [HolidaysScreen]. This is the composable `:app` places behind `HolidaysKey`. Tapping a row of the
 * year list pushes [DayKey] for that occurrence's date through [navigator] (CLAUDE.md rule 10).
 *
 * @param navigator receives the [DayKey] of a tapped occurrence.
 * @param modifier applied to the screen's root [Scaffold].
 */
@Composable
fun HolidaysRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: HolidaysViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HolidaysScreen(
        state = state,
        onBack = navigator::goBack,
        onHolidaySetEnabledChanged = viewModel::setHolidaySetEnabled,
        onGoToYear = viewModel::goToYear,
        onRowClick = { epochDay -> navigator.navigate(DayKey(epochDay)) },
        modifier = modifier,
    )
}

/**
 * The stateless Holidays screen — the unit for previews, screenshot and Compose tests
 * (docs/ARCHITECTURE.md §4 "State management"). Two sections: every bundled set with a switch, its
 * region, how many holidays it defines and, when the pack carries them, its sources
 * (`docs/holidays-and-import.md` §2.4); and the chosen year's holidays from the enabled sets, grouped by
 * IFC month, each row showing the holiday's name, its IFC date in long and numeric form, and its
 * Gregorian date with its real weekday. An empty year list (every set disabled) shows an explanatory
 * message instead of nothing.
 *
 * @param onHolidaySetEnabledChanged receives the set id (`HolidaySet.id`) and the new state.
 * @param onGoToYear receives the year to show next, from the previous/next actions.
 * @param onRowClick receives a tapped occurrence's date as a Gregorian epoch day.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolidaysScreen(
    state: HolidaysUiState,
    onBack: () -> Unit,
    onHolidaySetEnabledChanged: (id: String, enabled: Boolean) -> Unit,
    onGoToYear: (Int) -> Unit,
    onRowClick: (epochDay: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.holidays_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.holidays_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        when (state) {
            HolidaysUiState.Loading -> {
                LoadingContent(Modifier.padding(padding))
            }

            is HolidaysUiState.Loaded -> {
                LoadedContent(
                    state = state,
                    onHolidaySetEnabledChanged = onHolidaySetEnabledChanged,
                    onGoToYear = onGoToYear,
                    onRowClick = onRowClick,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

// A plain scrollable Column, not a LazyColumn: a year's holidays from a handful of packs is at most a
// few dozen rows (matching SettingsScreen, ConverterScreen and MoreScreen's own choice), and it composes
// every row eagerly, so performScrollTo() in tests and TalkBack's linear traversal both see the whole
// screen rather than only whatever a LazyColumn happened to have realized near the viewport.
@Composable
private fun LoadedContent(
    state: HolidaysUiState.Loaded,
    onHolidaySetEnabledChanged: (id: String, enabled: Boolean) -> Unit,
    onGoToYear: (Int) -> Unit,
    onRowClick: (epochDay: Long) -> Unit,
    modifier: Modifier,
) {
    val formatter = rememberIfcDateFormatter()
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
    ) {
        SectionHeading(stringResource(R.string.holidays_section_sets))
        SectionInfo(stringResource(R.string.holidays_sets_info))
        state.sets.forEach { set ->
            HolidaySetItem(row = set, onCheckedChange = { enabled -> onHolidaySetEnabledChanged(set.id, enabled) })
        }
        HorizontalDivider()
        SectionHeading(stringResource(R.string.holidays_section_year))
        YearNav(state = state, onGoToYear = onGoToYear, formatter = formatter)
        if (state.groups.isEmpty()) {
            EmptyYearList()
        } else {
            state.groups.forEach { group ->
                SectionHeading(group.monthLabel)
                group.rows.forEach { row -> HolidayOccurrenceItem(row = row, onClick = onRowClick) }
            }
        }
    }
}

/** FEATURES H1, H2, H3, H5: one bundled set's switch, region, holiday count and sources. */
@Composable
private fun HolidaySetItem(
    row: HolidaySetRow,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(row.name) },
        supportingContent = {
            Column {
                row.region?.let { Text(stringResource(R.string.holidays_set_region, it)) }
                Text(pluralStringResource(R.plurals.holidays_set_count, row.holidayCount, row.holidayCount))
                row.sources?.let {
                    Text(
                        text = stringResource(R.string.holidays_set_sources, it),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        trailingContent = { Switch(checked = row.enabled, onCheckedChange = null) },
        modifier =
            Modifier
                .fillMaxWidth()
                .toggleable(value = row.enabled, role = Role.Switch, onValueChange = onCheckedChange),
    )
}

/** FEATURES C7-style year paging, clamped at [io.github.chrisjmendoza.yearal.core.designsystem.picker.DatePickerRange]. */
@Composable
private fun YearNav(
    state: HolidaysUiState.Loaded,
    onGoToYear: (Int) -> Unit,
    formatter: IfcDateFormatter,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onGoToYear(state.year - 1) }, enabled = state.canGoPreviousYear) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.holidays_year_previous),
            )
        }
        Text(
            text = formatter.formatNumber(state.year),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onGoToYear(state.year + 1) }, enabled = state.canGoNextYear) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.holidays_year_next),
            )
        }
    }
}

/** One occurrence: name, IFC long and numeric form, Gregorian date with its real weekday. */
@Composable
private fun HolidayOccurrenceItem(
    row: HolidayOccurrenceRow,
    onClick: (Long) -> Unit,
) {
    ListItem(
        headlineContent = { Text(row.name) },
        supportingContent = {
            Column {
                Text(stringResource(R.string.holidays_row_ifc_line, row.ifcLong, row.ifcNumeric))
                Text(row.gregorianLong, style = MaterialTheme.typography.bodySmall)
            }
        },
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = { onClick(row.epochDay) })
                .semantics(mergeDescendants = true) { contentDescription = row.description },
    )
}

@Composable
private fun EmptyYearList() {
    Text(
        text = stringResource(R.string.holidays_empty_state),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
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

// Previews — Roborazzi's preview scanner captures every @Preview once docs/ROADMAP.md M2 T10 records
// the goldens; dynamic colour is off for determinism.

@Preview(name = "Holidays, 2026", showBackground = true, heightDp = 1400)
@Composable
internal fun HolidaysScreenPreview() {
    IfcTheme(dynamicColor = false) {
        HolidaysScreen(
            state = previewState,
            onBack = {},
            onHolidaySetEnabledChanged = { _, _ -> },
            onGoToYear = {},
            onRowClick = {},
        )
    }
}

@Preview(name = "Every set disabled", showBackground = true)
@Composable
internal fun HolidaysScreenEmptyPreview() {
    IfcTheme(dynamicColor = false) {
        HolidaysScreen(
            state = previewState.copy(sets = previewState.sets.map { it.copy(enabled = false) }, groups = emptyList()),
            onBack = {},
            onHolidaySetEnabledChanged = { _, _ -> },
            onGoToYear = {},
            onRowClick = {},
        )
    }
}

private val previewState =
    HolidaysUiState.Loaded(
        sets =
            listOf(
                HolidaySetRow(
                    id = "ifc",
                    name = "International Fixed Calendar",
                    region = null,
                    holidayCount = 3,
                    sources = null,
                    enabled = true,
                ),
                HolidaySetRow(
                    id = "us",
                    name = "United States",
                    region = "United States",
                    holidayCount = 24,
                    sources = "5 U.S.C. § 6103",
                    enabled = true,
                ),
                HolidaySetRow(
                    id = "religious-christian",
                    name = "Christian (Easter family)",
                    region = null,
                    holidayCount = 6,
                    sources = null,
                    enabled = false,
                ),
            ),
        year = 2026,
        groups =
            listOf(
                HolidayMonthGroup(
                    monthLabel = "January",
                    rows =
                        listOf(
                            HolidayOccurrenceRow(
                                epochDay = 20089,
                                name = "New Year's Day",
                                ifcLong = "January 1, 2026",
                                ifcNumeric = "IFC 2026-01-01",
                                gregorianLong = "Thursday, January 1, 2026",
                                description =
                                    "New Year's Day. IFC January 1, 2026 · IFC 2026-01-01. " +
                                        "Gregorian Thursday, January 1, 2026.",
                            ),
                        ),
                ),
                HolidayMonthGroup(
                    monthLabel = "December",
                    rows =
                        listOf(
                            HolidayOccurrenceRow(
                                epochDay = 20453,
                                name = "Year Day",
                                ifcLong = "Year Day, 2026",
                                ifcNumeric = "IFC 2026-13-29",
                                gregorianLong = "Thursday, December 31, 2026",
                                description =
                                    "Year Day. IFC Year Day, 2026 · IFC 2026-13-29. " +
                                        "Gregorian Thursday, December 31, 2026.",
                            ),
                        ),
                ),
            ),
    )
