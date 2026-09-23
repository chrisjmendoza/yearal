package io.github.chrisjmendoza.yearal.feature.calendar.month

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.chrisjmendoza.yearal.core.calendar.IfcDate
import io.github.chrisjmendoza.yearal.core.designsystem.format.IfcDateFormatter
import io.github.chrisjmendoza.yearal.core.designsystem.theme.Dimens
import io.github.chrisjmendoza.yearal.core.designsystem.theme.YearalTheme
import io.github.chrisjmendoza.yearal.feature.calendar.R
import io.github.chrisjmendoza.yearal.feature.calendar.agenda.AgendaItemUi
import io.github.chrisjmendoza.yearal.feature.calendar.common.HolidayDiamondMark
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val SwatchSize = 10.dp

/**
 * Anchors the Month grid (`docs/design-plan.md` §4.2, owner note 2): the summary of
 * [MonthUiState.summaryDate] — the selected day, or today when nothing is selected — in both
 * calendars, its holidays ([HolidayDiamondMark] rows) and events (colour-swatch rows), and a
 * "Details" action that opens the same Day detail a grid cell tap opens.
 *
 * A single card on [YearalTheme.colors]' `cardContainer`, so the space below the grid reads as one
 * cohesive block rather than the "floating grid" the design review flagged.
 *
 * @param state the Month pager's state; [MonthUiState.summaryHolidays] and
 * [MonthUiState.summaryAgenda] are already evaluated for [MonthUiState.summaryDate].
 * @param formatter formats the summary date in both calendars.
 * @param onDetailsClick invoked when the "Details" action is tapped; the caller binds this to the
 * same callback a grid cell tap uses, so behaviour matches the current width class automatically.
 * @param modifier applied to the card.
 */
@Composable
fun SelectedDaySummary(
    state: MonthUiState,
    formatter: IfcDateFormatter,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val date = state.summaryDate
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
            if (date == null) {
                Text(
                    text = stringResource(R.string.month_summary_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }
            val ifcDate = IfcDate.from(date)
            Text(
                text = formatter.formatLong(ifcDate),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = formatter.formatGregorianLong(date),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SummarySection(title = stringResource(R.string.day_holidays)) {
                if (state.summaryHolidays.isEmpty()) {
                    Text(
                        text = stringResource(R.string.month_summary_holidays_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
                        for (holiday in state.summaryHolidays) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
                            ) {
                                HolidayDiamondMark()
                                Text(text = holiday, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            SummarySection(title = stringResource(R.string.day_events)) {
                if (state.summaryAgenda.isEmpty()) {
                    Text(
                        text = stringResource(R.string.month_summary_events_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
                        for (item in state.summaryAgenda) {
                            SummaryAgendaRow(item)
                        }
                    }
                }
            }

            FilledTonalButton(onClick = onDetailsClick) {
                Text(stringResource(R.string.month_summary_details))
            }
        }
    }
}

/** One labelled block of the summary: a `labelSmall` eyebrow heading over [content]. */
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

/** A read-only agenda row: the event's colour swatch, its title and its time (FEATURES C4). */
@Composable
private fun SummaryAgendaRow(item: AgendaItemUi) {
    val title = item.title.ifBlank { stringResource(R.string.agenda_untitled_event) }
    val timeLabel =
        if (item.isAllDay) {
            stringResource(R.string.agenda_all_day)
        } else {
            val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            stringResource(
                R.string.agenda_time_range,
                formatter.format(item.startTime ?: LocalTime.MIDNIGHT),
                formatter.format(item.endTime ?: LocalTime.MIDNIGHT),
            )
        }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
    ) {
        Box(modifier = Modifier.size(SwatchSize).background(Color(item.colorArgb), CircleShape))
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = timeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
