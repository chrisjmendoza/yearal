package io.github.chrisjmendoza.yearal.feature.calendar.month

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.chrisjmendoza.yearal.core.calendar.IfcDate
import io.github.chrisjmendoza.yearal.core.calendar.IfcMonth
import io.github.chrisjmendoza.yearal.core.calendar.IfcYearMonth
import io.github.chrisjmendoza.yearal.core.designsystem.adaptive.WindowWidthClass
import io.github.chrisjmendoza.yearal.core.designsystem.adaptive.currentWindowWidthClass
import io.github.chrisjmendoza.yearal.core.designsystem.calendar.MonthGrid
import io.github.chrisjmendoza.yearal.core.designsystem.calendar.MonthGridTestTags
import io.github.chrisjmendoza.yearal.core.designsystem.explainer.ExplainerInfoButton
import io.github.chrisjmendoza.yearal.core.designsystem.format.rememberIfcDateFormatter
import io.github.chrisjmendoza.yearal.core.designsystem.picker.DatePickerRange
import io.github.chrisjmendoza.yearal.core.designsystem.picker.GregorianDatePickerDialog
import io.github.chrisjmendoza.yearal.core.designsystem.picker.IfcDatePicker
import io.github.chrisjmendoza.yearal.core.designsystem.picker.rememberIfcDatePickerState
import io.github.chrisjmendoza.yearal.core.designsystem.theme.Dimens
import io.github.chrisjmendoza.yearal.core.designsystem.theme.IfcTheme
import io.github.chrisjmendoza.yearal.core.designsystem.theme.PillShape
import io.github.chrisjmendoza.yearal.core.navigation.ConverterKey
import io.github.chrisjmendoza.yearal.core.navigation.DayKey
import io.github.chrisjmendoza.yearal.core.navigation.EventEditorKey
import io.github.chrisjmendoza.yearal.core.navigation.MonthKey
import io.github.chrisjmendoza.yearal.core.navigation.Navigator
import io.github.chrisjmendoza.yearal.core.navigation.YearKey
import io.github.chrisjmendoza.yearal.feature.calendar.R
import io.github.chrisjmendoza.yearal.feature.calendar.day.DayDetailState
import io.github.chrisjmendoza.yearal.feature.calendar.day.DayViewModel
import io.github.chrisjmendoza.yearal.feature.calendar.day.rememberDayDetailState
import kotlinx.coroutines.launch
import java.time.LocalDate
import io.github.chrisjmendoza.yearal.core.designsystem.R as DesignSystemR

private val PageHorizontalPadding = 16.dp
private val JumpChooserOptionSpacing = 8.dp
private val JumpChooserMinTouchTarget = 48.dp

/**
 * A floor on the selected-day summary's height (`docs/design-plan.md` §4.2, owner note 2: "fills the
 * space below"), so a tall screen with a short summary still reads as one anchored page rather than
 * the grid floating above dead space. Not a hard fill: the page column scrolls, so a summary with more
 * content than this — or a grid grown tall by a large font scale — is never clipped (design-plan §2,
 * "200% font scale never clips").
 */
private val SummaryMinHeight = 220.dp

/**
 * The Calendar tab's Month pager (docs/FEATURES.md C1, C3, C5, C7; docs/ARCHITECTURE.md §4 "Adaptive
 * layouts"): collects [MonthViewModel.uiState] with the lifecycle and renders it through the
 * stateless [CalendarScreen]. This is the composable `:app` places behind [MonthKey].
 *
 * The ViewModel is created for [key]'s month through [MonthViewModel.Factory] (the month is clamped
 * by [MonthPages.monthOf]). [currentWindowWidthClass] decides the layout (docs/ROADMAP.md M3 T4):
 *
 * - **Compact and medium:** a tap on a day selects it and pushes [DayKey] with the day's Gregorian
 *   epoch day (CLAUDE.md rule 4) — unchanged from before this task, so the sheet still works exactly
 *   as it did.
 * - **Expanded:** a tap on a day only calls [MonthViewModel.select]; no navigation happens; instead a
 *   second, [DayViewModel] — created only while there is a selection, keyed by the selected epoch day
 *   so switching days creates a fresh instance rather than reusing a stale one — feeds the list-detail
 *   pane's own [DayDetailState] directly, and [BackHandler] clears the selection instead of leaving
 *   the tab (docs/ARCHITECTURE.md §4). "Add event" and "Open in converter" still navigate: they open a
 *   different tab, which is unrelated to Calendar's own list-detail split.
 *
 * Tapping the month heading zooms out to [YearKey] (ARCHITECTURE §4) in both layouts; the
 * jump-to-date action lets the user pick either calendar and pushes the [MonthKey] of the chosen date
 * (FEATURES C7).
 *
 * @param key the month to open on.
 * @param navigator where the title tap, the jump-to-date action, and (compact/medium only) a day tap
 * navigate; also where the expanded detail pane's "Add event" / "Open in converter" navigate.
 * @param modifier applied to the screen's root.
 */
@Composable
fun MonthRoute(
    key: MonthKey,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: MonthViewModel =
        hiltViewModel<MonthViewModel, MonthViewModel.Factory>(
            creationCallback = { factory -> factory.create(MonthPages.monthOf(key)) },
        ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val widthClass = currentWindowWidthClass()
    val selectedEpochDay = state.selected?.toEpochDay()

    // The detail pane's own ViewModel exists only while there is a selection at expanded widths — an
    // explicit `key` (not the default call-site key) so picking a different day creates a fresh
    // instance instead of reusing the previous day's (this composable itself never gets a new Nav3
    // entry to do that for us, unlike DayRoute).
    val dayViewModel =
        if (widthClass == WindowWidthClass.EXPANDED && selectedEpochDay != null) {
            hiltViewModel<DayViewModel, DayViewModel.Factory>(
                key = "month-day-detail-$selectedEpochDay",
                creationCallback = { factory -> factory.create(selectedEpochDay) },
            )
        } else {
            null
        }
    val dayDetail: DayDetailState? = dayViewModel?.let { rememberDayDetailState(it) }

    // FEATURES C5 / docs/ROADMAP.md M3 T4: back clears the selection instead of popping the tab, but
    // only while there is a selection to clear — otherwise back falls through to Nav3 as usual.
    BackHandler(enabled = widthClass == WindowWidthClass.EXPANDED && state.selected != null) {
        viewModel.clearSelection()
    }

    CalendarScreen(
        widthClass = widthClass,
        monthState = state,
        onPageChanged = viewModel::showPage,
        onTitleClick = { year -> navigator.navigate(YearKey(year)) },
        onJumpToDate = { date ->
            val month = IfcYearMonth.from(IfcDate.from(date))
            navigator.navigate(MonthKey(month.year, month.month.number))
        },
        onDayClick = { date ->
            val gregorian = date.toLocalDate()
            viewModel.select(gregorian)
            navigator.navigate(DayKey(gregorian.toEpochDay()))
        },
        onSelectDay = { date -> viewModel.select(date.toLocalDate()) },
        dayState = dayDetail?.uiState,
        dayCallbacks =
            DayDetailCallbacks(
                onEventClick = { eventId -> navigator.navigate(EventEditorKey(eventId = eventId)) },
                onAddEvent = {
                    selectedEpochDay?.let { epochDay -> navigator.navigate(EventEditorKey(prefillEpochDay = epochDay)) }
                },
                onOpenInConverter = {
                    selectedEpochDay?.let { epochDay -> navigator.navigate(ConverterKey(prefillEpochDay = epochDay)) }
                },
                onRequestDelete = dayViewModel?.let { vm -> vm::requestDelete } ?: {},
                onConfirmDelete = dayViewModel?.let { vm -> vm::confirmDelete } ?: {},
                onCancelDelete = dayViewModel?.let { vm -> vm::cancelDelete } ?: {},
                onClose = viewModel::clearSelection,
            ),
        modifier = modifier,
        snackbarHostState = dayDetail?.snackbarHostState,
    )
}

/**
 * The stateless Month screen — the unit for previews, screenshot and Compose tests
 * (docs/ARCHITECTURE.md §4 "State management").
 *
 * A [HorizontalPager] over every month of [MonthPages] (spec §7.1: 1583..9999), one [MonthGrid] per
 * page, keeping one page warm on each side (ARCHITECTURE §3.4). Each page is **anchored**
 * (`docs/design-plan.md` §4.2, owner note 2): the grid at the top with `showTitle = false` — the app
 * bar above is the page's one title, a [MonthTitlePill] showing the currently visible month; tapping
 * it invokes [onTitleClick] with the year, the zoom-out to the Year view (docs/ARCHITECTURE.md §4;
 * docs/ROADMAP.md M3 T2) — and a [SelectedDaySummary] filling the rest of the page below it, so the
 * grid never floats above dead space. The app bar also carries the weekday-header
 * [ExplainerInfoButton] `showTitle = false` hides along with the grid's own heading (`MonthGrid`'s
 * KDoc calls this trap out by name), a jump-to-date action (FEATURES C7) and the "Today" action,
 * shown only while the pager is away from today's month, which animates the pager to
 * [MonthUiState.todayPage]. Leap Day and Year Day are the grid's band and reach [onDayClick] like any
 * cell (spec §7.2).
 *
 * Opts in to the Material 3 experimental marker only because `TopAppBar`'s default arguments still
 * carry it.
 *
 * @param state what to show.
 * @param onPageChanged invoked with the page the pager settles towards, immediately on first
 * composition and on every change; the ViewModel evaluates holidays around it.
 * @param onDayClick invoked with the tapped day — a regular cell or the intercalary band.
 * @param modifier applied to the screen's root.
 * @param onTitleClick invoked with the currently visible page's IFC year when the app bar title is
 * tapped.
 * @param onJumpToDate invoked with the date chosen from the jump-to-date action, in either calendar.
 * @param pagerState the pager's state; defaults to a remembered state opened on
 * [MonthUiState.currentPage]. Overridable so tests can drive the pager directly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthScreen(
    state: MonthUiState,
    onPageChanged: (Int) -> Unit,
    onDayClick: (IfcDate) -> Unit,
    modifier: Modifier = Modifier,
    onTitleClick: (Int) -> Unit = {},
    onJumpToDate: (LocalDate) -> Unit = {},
    pagerState: PagerState = rememberPagerState(initialPage = state.currentPage) { MonthPages.COUNT },
) {
    val currentOnPageChanged by rememberUpdatedState(onPageChanged)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page -> currentOnPageChanged(page) }
    }
    val scope = rememberCoroutineScope()
    val todayPage = state.todayPage
    val formatter = rememberIfcDateFormatter()
    var jumpChooserVisible by rememberSaveable { mutableStateOf(false) }
    var jumpGregorianVisible by rememberSaveable { mutableStateOf(false) }
    var jumpIfcVisible by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    val visibleMonth = MonthPages.monthAt(pagerState.currentPage)
                    MonthTitlePill(
                        text = formatter.monthTitle(visibleMonth),
                        onClick = { onTitleClick(visibleMonth.year) },
                    )
                },
                actions = {
                    ExplainerInfoButton(
                        title = stringResource(DesignSystemR.string.weekday_explainer_title),
                        explanation = stringResource(DesignSystemR.string.weekday_explainer_body),
                        modifier = Modifier.testTag(MonthGridTestTags.WEEKDAY_EXPLAINER),
                    )
                    IconButton(onClick = { jumpChooserVisible = true }) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = stringResource(R.string.month_jump_to_date),
                        )
                    }
                    if (todayPage != null && todayPage != pagerState.currentPage) {
                        TextButton(onClick = { scope.launch { pagerState.animateScrollToPage(todayPage) } }) {
                            Text(stringResource(R.string.month_today))
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
            )
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(padding).fillMaxSize(),
            beyondViewportPageCount = 1,
            key = { page -> page },
        ) { page ->
            val month = MonthPages.monthAt(page)
            // Anchor the grid (docs/design-plan.md §4.2, owner note 2): the grid stays at the top of
            // the page, unweighted, and the selected-day summary fills the rest — only the summary
            // scrolls on its own, so the grid's own fixed 4x7 shape never moves.
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                MonthGrid(
                    month = month,
                    today = state.today,
                    selected = state.selected,
                    weekdayDisplay = state.weekdayDisplay,
                    onDayClick = onDayClick,
                    eventCounts = state.eventCountsByMonth[month].orEmpty(),
                    holidays = state.holidaysByMonth[month].orEmpty(),
                    showTitle = false,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = PageHorizontalPadding),
                )
                // The summary itself is not page-specific (it follows state.selected/state.today, not
                // this page's month), but beyondViewportPageCount keeps up to three pages composed at
                // once — rendering it on every one of them would put three identical, off-screen copies
                // in the semantics tree. Only the page the pager is actually settled on shows it.
                if (page == pagerState.currentPage) {
                    SelectedDaySummary(
                        state = state,
                        formatter = formatter,
                        onDetailsClick = {
                            state.summaryDate?.let { date -> onDayClick(IfcDate.from(date)) }
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PageHorizontalPadding, vertical = Dimens.SpaceM)
                                .heightIn(min = SummaryMinHeight),
                    )
                }
            }
        }
    }

    // Jump to date (FEATURES C7): a small chooser, then the picker for the chosen calendar, mirroring
    // the event editor's "either calendar" pattern (docs/ARCHITECTURE.md §4 "Converter") without
    // depending on :feature:events (CLAUDE.md rule 10 — features never depend on other features).
    val jumpInitialDate = state.today ?: DatePickerRange.firstDate
    if (jumpChooserVisible) {
        JumpToDateChooserDialog(
            onPickGregorian = {
                jumpChooserVisible = false
                jumpGregorianVisible = true
            },
            onPickIfc = {
                jumpChooserVisible = false
                jumpIfcVisible = true
            },
            onDismiss = { jumpChooserVisible = false },
        )
    }
    if (jumpGregorianVisible) {
        GregorianDatePickerDialog(
            initialDate = jumpInitialDate,
            onConfirm = { date ->
                jumpGregorianVisible = false
                onJumpToDate(date)
            },
            onDismiss = { jumpGregorianVisible = false },
        )
    }
    if (jumpIfcVisible) {
        JumpToDateIfcDialog(
            initialDate = jumpInitialDate,
            onConfirm = { date ->
                jumpIfcVisible = false
                onJumpToDate(date)
            },
            onDismiss = { jumpIfcVisible = false },
        )
    }
}

/**
 * The app-bar title (`docs/design-plan.md` §4.2, owner notes 3–4): a visibly tappable pill — the
 * month heading plus a trailing chevron — that zooms out to the Year view. Content-described "Show
 * year" (a new string) so TalkBack announces it as a control, not a plain heading; still a heading
 * ([Role.Button] plus [androidx.compose.ui.semantics.heading]) so [onClick]'s target reads as the
 * page's own title, exactly as the grid's own (now hidden, `showTitle = false`) heading used to.
 */
@Composable
private fun MonthTitlePill(
    text: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .clip(PillShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .selectable(selected = false, role = Role.Button, onClick = onClick)
                .semantics(mergeDescendants = true) { heading() }
                .padding(horizontal = Dimens.SpaceM, vertical = Dimens.SpaceS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium)
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = stringResource(R.string.month_show_year),
        )
    }
}

/** Offers a choice of calendar before the jump-to-date pickers open (FEATURES C7). */
@Composable
private fun JumpToDateChooserDialog(
    onPickGregorian: () -> Unit,
    onPickIfc: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.month_jump_choose_calendar_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(JumpChooserOptionSpacing)) {
                TextButton(
                    onClick = onPickGregorian,
                    modifier = Modifier.fillMaxWidth().heightIn(min = JumpChooserMinTouchTarget),
                ) {
                    Text(stringResource(R.string.month_jump_pick_gregorian))
                }
                TextButton(
                    onClick = onPickIfc,
                    modifier = Modifier.fillMaxWidth().heightIn(min = JumpChooserMinTouchTarget),
                ) {
                    Text(stringResource(R.string.month_jump_pick_ifc))
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.month_jump_cancel)) } },
    )
}

/** The IFC picker step of the jump-to-date action, wrapped in a dialog like the event editor's own. */
@Composable
private fun JumpToDateIfcDialog(
    initialDate: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberIfcDatePickerState(initialDate = IfcDate.from(initialDate))
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { IfcDatePicker(state = pickerState) },
        confirmButton = {
            TextButton(
                onClick = { pickerState.date?.let { onConfirm(it.toLocalDate()) } },
                enabled = pickerState.date != null,
            ) {
                Text(stringResource(R.string.month_jump_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.month_jump_cancel)) } },
    )
}

// Previews — one per band shape (CLAUDE.md rule 6): none, Leap Day, Year Day, each at light, dark and
// 200% font scale. Roborazzi's preview scanner captures every @Preview once docs/ROADMAP.md M2 T10
// records the goldens; dynamic colour is off for determinism. Today is the spec §4.1 worked example,
// Gregorian September 17, 2026.

/** Sol 2026: no intercalary day, so the band slot shows the Gregorian span. */
@Preview(name = "Sol 2026 — light", showBackground = true)
@Composable
internal fun MonthScreenSolPreview() {
    MonthPreview(IfcYearMonth(2026, IfcMonth.SOL))
}

@Preview(name = "Sol 2026 — dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun MonthScreenSolDarkPreview() {
    MonthPreview(IfcYearMonth(2026, IfcMonth.SOL), darkTheme = true)
}

@Preview(name = "Sol 2026 — 200% font", showBackground = true, fontScale = 2f)
@Composable
internal fun MonthScreenSolLargeFontPreview() {
    MonthPreview(IfcYearMonth(2026, IfcMonth.SOL))
}

/** June 2028: a leap year, so the Leap Day band follows the fourth week. */
@Preview(name = "June 2028 (Leap Day) — light", showBackground = true)
@Composable
internal fun MonthScreenLeapDayPreview() {
    MonthPreview(IfcYearMonth(2028, IfcMonth.JUNE))
}

@Preview(name = "June 2028 (Leap Day) — dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun MonthScreenLeapDayDarkPreview() {
    MonthPreview(IfcYearMonth(2028, IfcMonth.JUNE), darkTheme = true)
}

@Preview(name = "June 2028 (Leap Day) — 200% font", showBackground = true, fontScale = 2f)
@Composable
internal fun MonthScreenLeapDayLargeFontPreview() {
    MonthPreview(IfcYearMonth(2028, IfcMonth.JUNE))
}

/** December 2026: the Year Day band, with the December holidays of the default packs. */
@Preview(name = "December 2026 (Year Day) — light", showBackground = true)
@Composable
internal fun MonthScreenYearDayPreview() {
    MonthPreview(
        month = IfcYearMonth(2026, IfcMonth.DECEMBER),
        holidays = DecemberHolidaysPreview,
    )
}

@Preview(name = "December 2026 (Year Day) — dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun MonthScreenYearDayDarkPreview() {
    MonthPreview(
        month = IfcYearMonth(2026, IfcMonth.DECEMBER),
        holidays = DecemberHolidaysPreview,
        darkTheme = true,
    )
}

@Preview(name = "December 2026 (Year Day) — 200% font", showBackground = true, fontScale = 2f)
@Composable
internal fun MonthScreenYearDayLargeFontPreview() {
    MonthPreview(
        month = IfcYearMonth(2026, IfcMonth.DECEMBER),
        holidays = DecemberHolidaysPreview,
    )
}

private val DecemberHolidaysPreview =
    mapOf(
        LocalDate.of(2026, 12, 24) to "Christmas Eve",
        LocalDate.of(2026, 12, 25) to "Christmas Day",
        LocalDate.of(2026, 12, 31) to "Year Day, New Year’s Eve",
    )

@Composable
private fun MonthPreview(
    month: IfcYearMonth,
    holidays: Map<LocalDate, String> = emptyMap(),
    darkTheme: Boolean = false,
) {
    val today = LocalDate.of(2026, 9, 17)
    IfcTheme(darkTheme = darkTheme, dynamicColor = false) {
        MonthScreen(
            state =
                MonthUiState(
                    currentPage = MonthPages.pageOf(month),
                    today = today,
                    todayPage = MonthPages.pageOf(IfcYearMonth.from(IfcDate.from(today))),
                    selected = null,
                    holidaysByMonth = mapOf(month to holidays),
                ),
            onPageChanged = {},
            onDayClick = {},
        )
    }
}
