package io.github.chrisjmendoza.yearal.feature.calendar.month

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.chrisjmendoza.yearal.core.calendar.IfcDate
import io.github.chrisjmendoza.yearal.core.designsystem.format.rememberIfcDateFormatter
import io.github.chrisjmendoza.yearal.core.designsystem.theme.Dimens
import io.github.chrisjmendoza.yearal.core.designsystem.theme.IfcTheme
import io.github.chrisjmendoza.yearal.core.designsystem.theme.YearalTheme
import io.github.chrisjmendoza.yearal.feature.calendar.R
import io.github.chrisjmendoza.yearal.feature.calendar.agenda.AgendaItemUi
import io.github.chrisjmendoza.yearal.feature.calendar.common.HolidayDiamondMark
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import io.github.chrisjmendoza.yearal.core.designsystem.R as DesignSystemR

/** Test tag of [IntercalaryHeader]'s surface, present only on Year Day and Leap Day. */
const val DAY_INTERCALARY_HEADER_TEST_TAG: String = "ifc:dayIntercalaryHeader"

private val ChipHorizontalPadding = 12.dp
private val ChipVerticalPadding = 4.dp
private val ColorDotSize = 12.dp
private val AgendaRowSpacing = 12.dp
private val IntercalaryIconSize = 24.dp
private val HolidayRowSpacing = 8.dp
private val LineSpacing = 4.dp

/**
 * The day card anchoring the Month grid (`docs/design-plan.md` §4.2, owner note 2; owner request "the
 * day card merge": the popup Day detail this superseded is gone, so this card is now the *whole* day
 * detail, not just a summary with a "Details" button onward). Shows [MonthUiState.dayDetail] — built
 * for [MonthUiState.summaryDate], the selected day or today when nothing is selected — in full: both
 * dates (IFC long and numeric, CLAUDE.md rule 5; Gregorian long), a "Today" badge, both labelled
 * weekdays (CLAUDE.md rule 3), day/week/quarter, holidays, events (each row tappable, long-press or its
 * TalkBack action requests delete, FEATURES E1) and the "Add event" / "Open in converter" actions
 * (FEATURES D1). Year Day and Leap Day get [IntercalaryHeader] instead of the plain heading (CLAUDE.md
 * rule 6).
 *
 * A single card on [YearalTheme.colors]' `cardContainer`, so the space below the grid reads as one
 * cohesive block, at both compact/medium widths (below [MonthGrid][io.github.chrisjmendoza.yearal.core.designsystem.calendar.MonthGrid])
 * and expanded widths (as the whole detail pane, `MonthListDetailScreen`).
 *
 * @param state the Month pager's state; [MonthUiState.dayDetail] is already evaluated for
 * [MonthUiState.summaryDate].
 * @param onEventClick invoked with an agenda row's event id.
 * @param onAddEvent invoked by the "Add event" action.
 * @param onOpenInConverter invoked by the "Open in converter" action.
 * @param onRequestDelete invoked with an agenda row to open its delete confirmation.
 * @param onConfirmDelete invoked when the delete confirmation is accepted.
 * @param onCancelDelete invoked when the delete confirmation is dismissed.
 * @param modifier applied to the card.
 */
@Composable
fun DayCard(
    state: MonthUiState,
    onEventClick: (Long) -> Unit,
    onAddEvent: () -> Unit,
    onOpenInConverter: () -> Unit,
    onRequestDelete: (AgendaItemUi) -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val detail = state.dayDetail
    Surface(
        color = YearalTheme.colors.cardContainer,
        contentColor = YearalTheme.colors.onCard,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(Dimens.SpaceM),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
        ) {
            if (detail == null) {
                Text(
                    text = stringResource(R.string.month_summary_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            if (detail.date is IfcDate.Regular) {
                Text(
                    text = detail.ifcLong,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = detail.numeric,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.day_gregorian, detail.gregorianLong),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (detail.isToday) {
                    TodayBadge()
                }
                WeekdayBlock(detail)
            } else {
                IntercalaryHeader(detail)
            }

            Text(
                text = stringResource(R.string.day_day_week_quarter, detail.dayAndWeek, detail.quarter),
                style = MaterialTheme.typography.bodyLarge,
            )

            SummarySection(title = stringResource(R.string.day_holidays)) {
                if (detail.holidays.isEmpty()) {
                    Text(
                        text = stringResource(R.string.month_summary_holidays_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(HolidayRowSpacing)) {
                        for (holiday in detail.holidays) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(HolidayRowSpacing),
                            ) {
                                HolidayDiamondMark()
                                Text(text = holiday, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }

            SummarySection(title = stringResource(R.string.day_events)) {
                if (detail.agenda.isEmpty()) {
                    Text(
                        text = stringResource(R.string.month_summary_events_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(AgendaRowSpacing)) {
                        for (item in detail.agenda) {
                            AgendaRow(
                                item = item,
                                onClick = { onEventClick(item.eventId) },
                                onRequestDelete = { onRequestDelete(item) },
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
                FilledTonalButton(onClick = onAddEvent) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(ChipHorizontalPadding))
                    Text(text = stringResource(R.string.day_add_event))
                }
                OutlinedButton(onClick = onOpenInConverter) {
                    Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(ChipHorizontalPadding))
                    Text(text = stringResource(R.string.day_open_in_converter))
                }
            }

            if (detail.pendingDelete != null) {
                DeleteAgendaItemDialog(
                    item = detail.pendingDelete,
                    onConfirm = onConfirmDelete,
                    onDismiss = onCancelDelete,
                )
            }
        }
    }
}

/** One labelled block of the card: a `labelSmall` eyebrow heading over [content]. */
@Composable
private fun SummarySection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

/** A static "Today" pill, deliberately not a button: it states a fact and does nothing (no dead controls). */
@Composable
internal fun TodayBadge() {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = stringResource(R.string.day_today),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = ChipHorizontalPadding, vertical = ChipVerticalPadding),
        )
    }
}

/**
 * Both weekdays, each line labelled by the formatter (spec §4.1 item 4) and merged into one spoken
 * description, "IFC Sunday, actual Thursday" (§4.1 item 7), so neither can be mistaken for the other.
 */
@Composable
internal fun WeekdayBlock(state: DayDetailUi) {
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
            modifier = Modifier.padding(Dimens.SpaceM),
            verticalArrangement = Arrangement.spacedBy(LineSpacing),
        ) {
            Text(text = state.nominalWeekday, style = MaterialTheme.typography.bodyLarge)
            Text(text = state.actualWeekday, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/**
 * The header for Year Day and Leap Day (`docs/design-plan.md` §4.4): an `intercalaryContainer`
 * surface with the [io.github.chrisjmendoza.yearal.core.designsystem.R.drawable.ic_intercalary] icon
 * tinted [YearalTheme.colors]' `intercalary`, the date, and [WeekdayBlock]'s existing "no IFC weekday"
 * explanation nested inside it — the same content a regular day's header shows, just gathered under
 * one amber container instead of sitting on the bare card background, so the day reads as visibly
 * different the moment the card shows it.
 */
@Composable
internal fun IntercalaryHeader(state: DayDetailUi) {
    Surface(
        color = YearalTheme.colors.intercalaryContainer,
        contentColor = YearalTheme.colors.onIntercalaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().testTag(DAY_INTERCALARY_HEADER_TEST_TAG),
    ) {
        Column(
            modifier = Modifier.padding(Dimens.SpaceM),
            verticalArrangement = Arrangement.spacedBy(LineSpacing),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(DesignSystemR.drawable.ic_intercalary),
                    contentDescription = null,
                    tint = YearalTheme.colors.intercalary,
                    modifier = Modifier.size(IntercalaryIconSize),
                )
                Spacer(modifier = Modifier.width(ChipHorizontalPadding))
                Text(
                    text = state.ifcLong,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
            }
            Text(text = state.numeric, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = stringResource(R.string.day_gregorian, state.gregorianLong),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (state.isToday) {
                TodayBadge()
            }
            WeekdayBlock(state)
        }
    }
}

/**
 * One row of the day's agenda (FEATURES C5): a coloured dot (never colour alone — the title and time
 * carry the same information in text), the title with a localized placeholder when blank, and "All
 * day" or the locale-formatted time range. Tapping the row invokes [onClick] with nothing but the
 * event id already bound by the caller (CLAUDE.md rule 8).
 *
 * Long-pressing the row, or its TalkBack custom action (`AccessibilityAction.ACTION_LONG_CLICK`'s
 * spoken-menu equivalent — reachable without a long press), invokes [onRequestDelete]: "delete this
 * occurrence" for a recurring event, a plain delete otherwise ([AgendaItemUi.isRecurring]).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AgendaRow(
    item: AgendaItemUi,
    onClick: () -> Unit,
    onRequestDelete: () -> Unit,
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
    val deleteActionLabel =
        stringResource(
            if (item.isRecurring) R.string.day_delete_occurrence_action else R.string.day_delete_event_action,
        )
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onRequestDelete)
                .semantics(mergeDescendants = true) {
                    contentDescription = "$title, $timeLabel"
                    customActions =
                        listOf(
                            CustomAccessibilityAction(deleteActionLabel) {
                                onRequestDelete()
                                true
                            },
                        )
                },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ChipHorizontalPadding),
    ) {
        Box(
            modifier =
                Modifier
                    .size(ColorDotSize)
                    .background(Color(item.colorArgb), CircleShape),
        )
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
 * Confirms deleting [item]: "delete this occurrence" wording and an undo mention for a recurring
 * event ([AgendaItemUi.isRecurring]), a plain and permanent "delete event" otherwise (FEATURES E1).
 */
@Composable
internal fun DeleteAgendaItemDialog(
    item: AgendaItemUi,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val titleRes = if (item.isRecurring) R.string.day_delete_occurrence_title else R.string.day_delete_event_title
    val textRes = if (item.isRecurring) R.string.day_delete_occurrence_text else R.string.day_delete_event_text
    val actionRes = if (item.isRecurring) R.string.day_delete_occurrence_action else R.string.day_delete_event_action
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = { Text(stringResource(textRes)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(actionRes)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.day_delete_cancel)) } },
    )
}

// Previews of the card — one per date shape (CLAUDE.md rule 6), each at light, dark and 200% font
// scale. Dynamic colour is off for determinism.

/** IFC December 23, 2026 = Gregorian Friday, December 25, 2026, with its holiday. */
@Preview(name = "Regular day — light", showBackground = true)
@Composable
internal fun DayCardRegularPreview() {
    DayCardPreview(
        day = LocalDate.of(2026, 12, 25),
        today = LocalDate.of(2026, 12, 25),
        holidays = listOf("Christmas Day"),
    )
}

@Preview(name = "Regular day — dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun DayCardRegularDarkPreview() {
    DayCardPreview(
        day = LocalDate.of(2026, 12, 25),
        today = LocalDate.of(2026, 12, 25),
        holidays = listOf("Christmas Day"),
        darkTheme = true,
    )
}

@Preview(name = "Regular day — 200% font", showBackground = true, fontScale = 2f)
@Composable
internal fun DayCardRegularLargeFontPreview() {
    DayCardPreview(
        day = LocalDate.of(2026, 12, 25),
        today = LocalDate.of(2026, 12, 25),
        holidays = listOf("Christmas Day"),
    )
}

/** Leap Day 2028 = Gregorian Saturday, June 17, 2028. */
@Preview(name = "Leap Day — light", showBackground = true)
@Composable
internal fun DayCardLeapDayPreview() {
    DayCardPreview(day = LocalDate.of(2028, 6, 17), today = LocalDate.of(2026, 9, 17), holidays = listOf("Leap Day"))
}

@Preview(name = "Leap Day — dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun DayCardLeapDayDarkPreview() {
    DayCardPreview(
        day = LocalDate.of(2028, 6, 17),
        today = LocalDate.of(2026, 9, 17),
        holidays = listOf("Leap Day"),
        darkTheme = true,
    )
}

@Preview(name = "Leap Day — 200% font", showBackground = true, fontScale = 2f)
@Composable
internal fun DayCardLeapDayLargeFontPreview() {
    DayCardPreview(day = LocalDate.of(2028, 6, 17), today = LocalDate.of(2026, 9, 17), holidays = listOf("Leap Day"))
}

/** Year Day 2026 = Gregorian Thursday, December 31, 2026. */
@Preview(name = "Year Day — light", showBackground = true)
@Composable
internal fun DayCardYearDayPreview() {
    DayCardPreview(
        day = LocalDate.of(2026, 12, 31),
        today = LocalDate.of(2026, 9, 17),
        holidays = listOf("Year Day", "New Year’s Eve"),
    )
}

@Preview(name = "Year Day — dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun DayCardYearDayDarkPreview() {
    DayCardPreview(
        day = LocalDate.of(2026, 12, 31),
        today = LocalDate.of(2026, 9, 17),
        holidays = listOf("Year Day", "New Year’s Eve"),
        darkTheme = true,
    )
}

@Preview(name = "Year Day — 200% font", showBackground = true, fontScale = 2f)
@Composable
internal fun DayCardYearDayLargeFontPreview() {
    DayCardPreview(
        day = LocalDate.of(2026, 12, 31),
        today = LocalDate.of(2026, 9, 17),
        holidays = listOf("Year Day", "New Year’s Eve"),
    )
}

@Composable
private fun DayCardPreview(
    day: LocalDate,
    today: LocalDate,
    holidays: List<String>,
    darkTheme: Boolean = false,
) {
    IfcTheme(darkTheme = darkTheme, dynamicColor = false) {
        val formatter = rememberIfcDateFormatter()
        val detail = buildDayDetailUi(day, today, formatter, holidays)
        DayCard(
            state = MonthUiState(currentPage = 0, today = today, todayPage = 0, selected = day, dayDetail = detail),
            onEventClick = {},
            onAddEvent = {},
            onOpenInConverter = {},
            onRequestDelete = {},
            onConfirmDelete = {},
            onCancelDelete = {},
        )
    }
}
