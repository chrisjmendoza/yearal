package io.github.chrisjmendoza.yearal.feature.calendar.today

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.chrisjmendoza.yearal.core.designsystem.format.rememberIfcDateFormatter
import io.github.chrisjmendoza.yearal.core.designsystem.theme.Dimens
import io.github.chrisjmendoza.yearal.core.designsystem.theme.IfcTheme
import io.github.chrisjmendoza.yearal.core.designsystem.theme.PillShape
import io.github.chrisjmendoza.yearal.core.designsystem.theme.YearalTheme
import io.github.chrisjmendoza.yearal.core.navigation.EventEditorKey
import io.github.chrisjmendoza.yearal.core.navigation.Navigator
import io.github.chrisjmendoza.yearal.feature.calendar.R
import io.github.chrisjmendoza.yearal.feature.calendar.agenda.AgendaItemUi
import io.github.chrisjmendoza.yearal.feature.calendar.common.HolidayDiamondMark
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import io.github.chrisjmendoza.yearal.core.designsystem.R as DesignSystemR

private val IntercalaryIconSize = 20.dp

/**
 * The Today tab (docs/FEATURES.md T1–T4, T6): collects [TodayViewModel.uiState] with the lifecycle
 * and renders it through the stateless [TodayScreen]. This is the composable `:app` places behind
 * `TodayKey`. Tapping an agenda row pushes [EventEditorKey] with the event's id only (CLAUDE.md rule
 * 8); holidays stay non-tappable (docs/ROADMAP.md, left over from M4 T7).
 *
 * @param navigator where an agenda row tap navigates.
 * @param modifier applied to the screen's root; the screen adds its own safe-drawing insets.
 */
@Composable
fun TodayRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TodayScreen(
        state = state,
        onAgendaItemClick = { eventId -> navigator.navigate(EventEditorKey(eventId = eventId)) },
        modifier = modifier,
    )
}

/**
 * The stateless Today screen — the unit for previews, screenshot and Compose tests
 * (docs/ARCHITECTURE.md §4 "State management").
 *
 * A hero card (`docs/design-plan.md` §4.1) on [YearalTheme.colors]' `heroContainer`/`onHero` carries
 * today's IFC weekday above the IFC date in `displayMedium`, an "IFC"/"Gregorian" eyebrow pair, the weekday block, the
 * day/week/quarter line and the year-progress bar. Below it: an amber intercalary countdown chip, a
 * Holidays card and an Events card, both showing a quiet line rather than vanishing when there is
 * nothing to show (§4.1).
 *
 * @param onAgendaItemClick invoked with an agenda row's event id (FEATURES T5); holidays are plain text.
 */
@Composable
fun TodayScreen(
    state: TodayUiState,
    modifier: Modifier = Modifier,
    onAgendaItemClick: (Long) -> Unit = {},
) {
    when (state) {
        TodayUiState.Loading -> LoadingContent(modifier)
        is TodayUiState.Loaded -> LoadedContent(state, modifier, onAgendaItemClick)
    }
}

@Composable
private fun LoadingContent(modifier: Modifier) {
    val loading = stringResource(R.string.today_loading)
    Box(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = loading })
    }
}

@Composable
private fun LoadedContent(
    state: TodayUiState.Loaded,
    modifier: Modifier,
    onAgendaItemClick: (Long) -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.SpaceL, vertical = Dimens.SpaceXl),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceL),
    ) {
        HeroCard(state)

        state.countdown?.let { countdown -> IntercalaryChip(countdown) }

        HolidaysCard(state.holidays, state.nextHolidayDays, state.nextHolidayName)

        EventsCard(state.agenda, onAgendaItemClick)
    }
}

/**
 * The hero card (`docs/design-plan.md` §4.1): today's IFC weekday above the IFC date in
 * `displayMedium`, the "IFC" / "Gregorian"
 * eyebrow pair (the review's acceptance test — "what Gregorian date is this?" — answered at a glance),
 * the weekday block, the day/week/quarter line and the tinted year-progress bar.
 */
@Composable
private fun HeroCard(state: TodayUiState.Loaded) {
    Surface(
        color = YearalTheme.colors.heroContainer,
        contentColor = YearalTheme.colors.onHero,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(Dimens.SpaceL),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
        ) {
            // The IFC weekday, bare, so the hero reads as one IFC date (owner ruling, 2026-09-25). TalkBack
            // speaks the labelled form ("IFC weekday: Sunday") so it is never heard as the real weekday,
            // which stays in the Gregorian line and the weekday block. Absent on Year Day and Leap Day.
            state.heroWeekday?.let { weekday ->
                Text(
                    text = weekday,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { contentDescription = state.nominalWeekday },
                )
            }
            Text(
                text = state.heroDate,
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.semantics { heading() },
            )
            EyebrowValue(stringResource(R.string.eyebrow_ifc), state.numericDate)
            EyebrowValue(stringResource(R.string.eyebrow_gregorian), state.gregorianLongDate)

            Spacer(modifier = Modifier.height(Dimens.SpaceS))

            WeekdayBlock(state)

            Spacer(modifier = Modifier.height(Dimens.SpaceS))

            Text(
                text = stringResource(R.string.today_day_week_quarter, state.dayAndWeek, state.quarter),
                style = MaterialTheme.typography.bodyLarge,
            )
            // clearAndSetSemantics on the wrapper is the single spoken node for both children
            // (a11y audit finding #21): without it, the indicator's own contentDescription and the
            // visible Text below it — the identical "71% of the year" string — were two separate
            // semantics nodes, so TalkBack spoke the same phrase twice back to back.
            Column(
                modifier = Modifier.clearAndSetSemantics { contentDescription = state.yearProgressLabel },
            ) {
                LinearProgressIndicator(
                    progress = { state.yearProgress },
                    trackColor = YearalTheme.colors.onHero.copy(alpha = PROGRESS_TRACK_ALPHA),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = state.yearProgressLabel,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/** One eyebrow (`labelSmall`, uppercased) over its value, e.g. "IFC" over "IFC 2026-10-08". */
@Composable
private fun EyebrowValue(
    eyebrow: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs / 2)) {
        Text(text = eyebrow.uppercase(), style = MaterialTheme.typography.labelSmall)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

/** The intercalary countdown as an amber pill chip (`docs/design-plan.md` §4.1). */
@Composable
private fun IntercalaryChip(countdown: String) {
    Surface(
        color = YearalTheme.colors.intercalaryContainer,
        contentColor = YearalTheme.colors.onIntercalaryContainer,
        shape = PillShape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.SpaceM, vertical = Dimens.SpaceS),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
        ) {
            Icon(
                painter = painterResource(DesignSystemR.drawable.ic_intercalary),
                contentDescription = null,
                tint = YearalTheme.colors.intercalary,
                modifier = Modifier.size(IntercalaryIconSize),
            )
            Text(text = countdown, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/**
 * Today's holidays (`docs/design-plan.md` §4.1): a card whose rows carry a leading
 * [HolidayDiamondMark]; a quiet line when there are none, rather than hiding the card. The "next
 * holiday" line (FEATURES T5) joins this card rather than standing alone.
 */
@Composable
private fun HolidaysCard(
    holidays: List<String>,
    nextHolidayDays: Int?,
    nextHolidayName: String?,
) {
    SectionCard(title = stringResource(R.string.today_holidays)) {
        if (holidays.isEmpty()) {
            Text(
                text = stringResource(R.string.today_holidays_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
                for (holiday in holidays) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
                    ) {
                        HolidayDiamondMark()
                        Text(text = holiday, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        if (nextHolidayDays != null && nextHolidayName != null) {
            Text(
                text =
                    pluralStringResource(
                        R.plurals.today_next_holiday,
                        nextHolidayDays,
                        nextHolidayDays,
                        nextHolidayName,
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Today's events (`docs/design-plan.md` §4.1): a card whose rows keep their colour swatch and gain a
 * hairline between rows; a quiet line when there are none.
 */
@Composable
private fun EventsCard(
    agenda: List<AgendaItemUi>,
    onAgendaItemClick: (Long) -> Unit,
) {
    SectionCard(title = stringResource(R.string.today_agenda_heading)) {
        if (agenda.isEmpty()) {
            Text(
                text = stringResource(R.string.today_agenda_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column {
                agenda.forEachIndexed { index, item ->
                    if (index > 0) HorizontalDivider()
                    TodayAgendaRow(item, onClick = { onAgendaItemClick(item.eventId) })
                }
            }
        }
    }
}

/** A card on [YearalTheme.colors]' `cardContainer` with a `labelSmall` eyebrow heading. */
@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Surface(
        color = YearalTheme.colors.cardContainer,
        contentColor = YearalTheme.colors.onCard,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(Dimens.SpaceM),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.semantics { heading() },
            )
            content()
        }
    }
}

/**
 * One row of today's agenda (FEATURES T5): a coloured dot (never colour alone — the title and time
 * carry the same information in text), the title with a localized placeholder when blank, and "All
 * day" or the locale-formatted time range. Tapping the row invokes [onClick] with nothing but the
 * event id already bound by the caller (CLAUDE.md rule 8), same as the Day detail's agenda row;
 * holidays elsewhere on this screen stay plain text, since they have nothing to open.
 */
@Composable
private fun TodayAgendaRow(
    item: AgendaItemUi,
    onClick: () -> Unit,
) {
    val title = item.title.ifBlank { stringResource(R.string.agenda_untitled_event) }
    val timeLabel =
        if (item.isAllDay) {
            stringResource(R.string.agenda_all_day)
        } else {
            val locale = LocalConfiguration.current.locales[0]
            val timeFormatter =
                remember(locale) { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale) }
            stringResource(
                R.string.agenda_time_range,
                timeFormatter.format(item.startTime ?: LocalTime.MIDNIGHT),
                timeFormatter.format(item.endTime ?: LocalTime.MIDNIGHT),
            )
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(vertical = Dimens.SpaceS)
                .semantics(mergeDescendants = true) {
                    contentDescription = "$title, $timeLabel"
                },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
    ) {
        Box(modifier = Modifier.size(12.dp).background(Color(item.colorArgb), CircleShape))
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = timeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Both weekdays, each line labelled by the formatter (spec §4.1 item 4) and merged into one spoken
 * description, "IFC Sunday, actual Thursday" (§4.1 item 7), so neither can be mistaken for the other.
 * Keeps its sage `secondaryContainer` fill (design-plan §4.1: "keep its sage container").
 */
@Composable
private fun WeekdayBlock(state: TodayUiState.Loaded) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { contentDescription = state.weekdaysDescription },
    ) {
        Column(
            modifier = Modifier.padding(Dimens.SpaceL),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
        ) {
            Text(text = state.nominalWeekday, style = MaterialTheme.typography.bodyLarge)
            Text(text = state.actualWeekday, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/** Alpha of the year-progress bar's track, tinted on top of [YearalTheme.colors]' `onHero`. */
private const val PROGRESS_TRACK_ALPHA = 0.24f

// Previews — one per date shape (CLAUDE.md rule 6), each at light, dark and 200% font scale, dynamic
// colour off for determinism (docs/design-plan.md §2).

/** IFC September 8, 2026 = Gregorian Thursday, September 17, 2026 (the spec §4.1 worked example). */
@Preview(name = "Regular day — light", showBackground = true)
@Composable
internal fun TodayScreenRegularPreview() {
    TodayPreview(LocalDate.of(2026, 9, 17))
}

@Preview(name = "Regular day — dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun TodayScreenRegularDarkPreview() {
    TodayPreview(LocalDate.of(2026, 9, 17), darkTheme = true)
}

@Preview(name = "Regular day — 200% font", showBackground = true, fontScale = 2f)
@Composable
internal fun TodayScreenRegularLargeFontPreview() {
    TodayPreview(LocalDate.of(2026, 9, 17))
}

/** Leap Day 2028 = Gregorian Saturday, June 17, 2028. */
@Preview(name = "Leap Day — light", showBackground = true)
@Composable
internal fun TodayScreenLeapDayPreview() {
    TodayPreview(LocalDate.of(2028, 6, 17))
}

@Preview(name = "Leap Day — dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun TodayScreenLeapDayDarkPreview() {
    TodayPreview(LocalDate.of(2028, 6, 17), darkTheme = true)
}

@Preview(name = "Leap Day — 200% font", showBackground = true, fontScale = 2f)
@Composable
internal fun TodayScreenLeapDayLargeFontPreview() {
    TodayPreview(LocalDate.of(2028, 6, 17))
}

/** Year Day 2026 = Gregorian Thursday, December 31, 2026. */
@Preview(name = "Year Day — light", showBackground = true)
@Composable
internal fun TodayScreenYearDayPreview() {
    TodayPreview(LocalDate.of(2026, 12, 31))
}

@Preview(name = "Year Day — dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun TodayScreenYearDayDarkPreview() {
    TodayPreview(LocalDate.of(2026, 12, 31), darkTheme = true)
}

@Preview(name = "Year Day — 200% font", showBackground = true, fontScale = 2f)
@Composable
internal fun TodayScreenYearDayLargeFontPreview() {
    TodayPreview(LocalDate.of(2026, 12, 31))
}

@Composable
private fun TodayPreview(
    today: LocalDate,
    darkTheme: Boolean = false,
) {
    IfcTheme(darkTheme = darkTheme, dynamicColor = false) {
        val formatter = rememberIfcDateFormatter()
        TodayScreen(state = buildTodayUiState(today, formatter))
    }
}
