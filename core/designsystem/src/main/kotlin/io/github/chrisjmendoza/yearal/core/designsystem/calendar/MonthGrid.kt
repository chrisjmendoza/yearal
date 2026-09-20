package io.github.chrisjmendoza.yearal.core.designsystem.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.chrisjmendoza.yearal.core.calendar.IfcDate
import io.github.chrisjmendoza.yearal.core.calendar.IfcMonth
import io.github.chrisjmendoza.yearal.core.calendar.IfcYearMonth
import io.github.chrisjmendoza.yearal.core.designsystem.R
import io.github.chrisjmendoza.yearal.core.designsystem.explainer.ExplainerInfoButton
import io.github.chrisjmendoza.yearal.core.designsystem.format.rememberIfcDateFormatter
import io.github.chrisjmendoza.yearal.core.domain.settings.WeekdayDisplay
import java.time.LocalDate

/** Rows of an IFC month grid: four whole weeks (spec §2.2). */
const val GRID_ROWS: Int = IfcMonth.DAYS_PER_MONTH / GRID_COLUMNS

private val TitleBottomPadding = 8.dp

/**
 * One IFC month as the perpetual 4 × 7 grid (FEATURES C1–C4; spec §7.2; docs/ARCHITECTURE.md §4):
 * the month title as a heading (`Sol 2028`), the [WeekdayHeaders], exactly [GRID_ROWS] rows of
 * [GRID_COLUMNS] [DayCell]s, then the band slot — an [IntercalaryBand] for June in leap years and for
 * December, otherwise an [IntercalaryPlaceholder] of the same height, so a pager of months never
 * jumps.
 *
 * Today and the selection are matched by **Gregorian** date (CLAUDE.md rule 4): a `today` in another
 * month simply marks nothing. Event counts and holidays are likewise keyed by the Gregorian date of
 * each day, which is what the agenda query returns (ARCHITECTURE §3.4). Every date shown is built by
 * `:core:calendar`; the grid computes none.
 *
 * The grid is a semantics traversal group; the cells are a plain `Column` of `Row`s, which keeps
 * screenshot and semantics tests simple.
 *
 * **Contextual explainers (FEATURES L3).** Two [ExplainerInfoButton]s answer the two beliefs
 * `docs/FEATURES.md` Part 1 records as a steady source of "this is wrong" reviews. Neither sits
 * *inside* the row it explains: the seven weekday headers and the 28 cells share one set of column
 * widths, so a 48dp button among them would pull the headers out of alignment with the days beneath.
 * The weekday explainer therefore goes at the end of the title row, immediately above the headers it
 * describes, where the heading leaves the space free; the intercalary explainer goes beside the band,
 * which spans all seven columns and so aligns to nothing. The band and its [IntercalaryPlaceholder]
 * are wrapped in one full-width slot ([MonthGridTestTags.INTERCALARY_SLOT]) that is the same size in
 * every month, which is what keeps a pager of months from jumping — the band itself is narrower than
 * the placeholder by the width of the button beside it.
 *
 * @param month the month to show.
 * @param today the real today as a Gregorian date, or `null` to mark no cell.
 * @param selected the selected day as a Gregorian date, or `null` for no selection.
 * @param weekdayDisplay which header rows to show (the user setting).
 * @param onDayClick invoked with the tapped day — a [IfcDate.Regular] cell, or the band's
 * [IfcDate.LeapDay] / [IfcDate.YearDay].
 * @param modifier applied to the grid; it fills the available width itself.
 * @param eventCounts number of events per Gregorian date; absent dates have none.
 * @param holidays holiday name per Gregorian date; absent dates have none.
 */
@Composable
fun MonthGrid(
    month: IfcYearMonth,
    today: LocalDate?,
    selected: LocalDate?,
    weekdayDisplay: WeekdayDisplay,
    onDayClick: (IfcDate) -> Unit,
    modifier: Modifier = Modifier,
    eventCounts: Map<LocalDate, Int> = emptyMap(),
    holidays: Map<LocalDate, String> = emptyMap(),
) {
    val formatter = rememberIfcDateFormatter()
    // The 28 (IfcDate, LocalDate) pairs depend only on `month`, but `today`, `selected`, `eventCounts`
    // and `holidays` are also parameters of this composable — so a change to any one of them (e.g. a
    // day tap changing `selected` while the pager keeps three months warm, ARCHITECTURE §3.4) recomposes
    // this whole function, not just the affected DayCell. Without memoizing this list, that means
    // reconstructing and re-validating all 28 IfcDate.Regular instances and their toLocalDate()
    // conversions, on every warm page, for every unrelated state change — real per-recomposition cost
    // that strong-skipping mode does not remove, because it lives in this function's own body rather
    // than in a skippable child call (docs/ROADMAP.md compose-perf pass). `remember(month)` computes it
    // once per page and reuses it across every other recomposition of the same month.
    val cellDates =
        remember(month) {
            List(IfcMonth.DAYS_PER_MONTH) { index ->
                val date = IfcDate.Regular(month.year, month.month, index + 1)
                date to date.toLocalDate()
            }
        }
    Column(modifier = modifier.fillMaxWidth().semantics { isTraversalGroup = true }) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = TitleBottomPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatter.monthTitle(month),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            ExplainerInfoButton(
                title = stringResource(R.string.weekday_explainer_title),
                explanation = stringResource(R.string.weekday_explainer_body),
                modifier = Modifier.testTag(MonthGridTestTags.WEEKDAY_EXPLAINER),
            )
        }
        WeekdayHeaders(month = month, display = weekdayDisplay)
        for (row in 0 until GRID_ROWS) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (column in 0 until GRID_COLUMNS) {
                    val (date, gregorian) = cellDates[row * GRID_COLUMNS + column]
                    DayCell(
                        date = date,
                        gregorian = gregorian,
                        isToday = gregorian == today,
                        isSelected = gregorian == selected,
                        eventCount = eventCounts[gregorian] ?: 0,
                        holidayName = holidays[gregorian],
                        onClick = { onDayClick(date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().testTag(MonthGridTestTags.INTERCALARY_SLOT),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (val intercalary = month.trailingIntercalary) {
                null -> {
                    IntercalaryPlaceholder(month = month, modifier = Modifier.weight(1f))
                }

                else -> {
                    val gregorian = intercalary.toLocalDate()
                    IntercalaryBand(
                        day = intercalary,
                        isToday = gregorian == today,
                        isSelected = gregorian == selected,
                        eventCount = eventCounts[gregorian] ?: 0,
                        holidayName = holidays[gregorian],
                        onClick = { onDayClick(intercalary) },
                        modifier = Modifier.weight(1f),
                    )
                    ExplainerInfoButton(
                        title = stringResource(R.string.intercalary_explainer_title),
                        explanation = stringResource(R.string.intercalary_explainer_body),
                        modifier = Modifier.testTag(MonthGridTestTags.INTERCALARY_EXPLAINER),
                    )
                }
            }
        }
    }
}
