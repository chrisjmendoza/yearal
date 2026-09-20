package io.github.chrisjmendoza.yearal.core.designsystem.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.chrisjmendoza.yearal.core.calendar.IfcDate
import io.github.chrisjmendoza.yearal.core.calendar.IfcMonth
import io.github.chrisjmendoza.yearal.core.calendar.IfcYearMonth
import io.github.chrisjmendoza.yearal.core.designsystem.R
import io.github.chrisjmendoza.yearal.core.designsystem.format.IfcDateFormatter
import io.github.chrisjmendoza.yearal.core.designsystem.format.rememberIfcDateFormatter
import java.time.LocalDate

/**
 * Test tags of the Year overview's tiles (docs/ROADMAP.md M3 T2; FEATURES C6), for the tiles that have
 * no reliable text of their own to search by. They sit in the **unmerged** semantics tree, exactly like
 * [MonthGridTestTags] — tests read them with `useUnmergedTree = true`.
 */
object YearOverviewTestTags {
    /**
     * Prefix of a mini-month tile's tag; the full tag appends [IfcMonth.number] (1..13), e.g.
     * `"ifc:yearMiniMonth:7"` for Sol.
     */
    const val MINI_MONTH_TILE_PREFIX: String = "ifc:yearMiniMonth:"
}

private val TilePadding = 12.dp
private val TileContentSpacing = 6.dp
private val TileBorderWidth = 2.dp
private val IntercalaryIconSize = 16.dp
private val IntercalaryRowSpacing = 6.dp
private const val MINI_GRID_ASPECT_RATIO = GRID_COLUMNS.toFloat() / GRID_ROWS.toFloat()
private const val CELL_INSET_FRACTION = 0.12f
private const val EVENT_DOT_RADIUS_FRACTION = 0.16f
private const val TODAY_RING_RADIUS_FRACTION = 0.32f

/**
 * One tile of the Year overview's `LazyVerticalGrid` (docs/FEATURES.md C6; docs/ARCHITECTURE.md §4
 * "Screen behaviors", "Intercalary days in a 7-column grid"; `docs/calendar-spec.md` §7.2): the
 * month's name and a compact 4 × 7 grid of its 28 regular days, drawn with a single [Canvas] rather
 * than 28 [DayCell]s — instantiating, measuring and semantics-attaching 28 real cells per tile (364 for
 * the year) visibly drops frames while scrolling the grid, while one [Canvas] draw per tile is a
 * handful of primitive shape calls with no semantics tree of its own, so the whole screen stays smooth.
 * June in a leap year additionally shows a small non-interactive Leap Day indicator (spec §7.2; the
 * Leap Day belongs to no week, so it is drawn outside the 4 × 7 grid, never as a 29th grid cell) —
 * tapping anywhere in the tile, Leap Day indicator included, opens the same month.
 *
 * The whole tile is **one** semantics node (FEATURES C6's TalkBack requirement: not 28 nodes per
 * month), built the same way [DayCell] merges its own descendants: a single `selectable` plus an
 * explicit [androidx.compose.ui.semantics.SemanticsPropertyReceiver.contentDescription] that replaces
 * whatever the inner `Text`s would otherwise contribute.
 *
 * Today is marked two ways, never by colour alone (docs/ARCHITECTURE.md §4 "Accessibility"): a border
 * around the whole tile when it contains the real today (a regular day or, for June, Leap Day), and a
 * hollow ring around that exact cell in the grid. A day with an event occurrence gets a filled dot
 * (holidays are not shown here — the Year view's presence bitmap is events only, per
 * [io.github.chrisjmendoza.yearal.core.domain.event.ObserveAgendaUseCase.presence]).
 *
 * @param month the month this tile shows.
 * @param today the real today as a Gregorian date, or `null` to mark nothing.
 * @param eventDates dates with at least one event occurrence, from `ObserveAgendaUseCase.presence`.
 * @param onClick invoked when the tile — including its Leap Day indicator — is tapped.
 * @param modifier applied to the tile; it fills the width the grid cell gives it.
 * @param formatter supplies the month name and the day names spoken in the description.
 */
@Composable
fun YearMiniMonthTile(
    month: IfcYearMonth,
    today: LocalDate?,
    eventDates: Set<LocalDate>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    formatter: IfcDateFormatter = rememberIfcDateFormatter(),
) {
    val leapDay = month.trailingIntercalary as? IfcDate.LeapDay
    val regularDays =
        remember(month) { (1..IfcMonth.DAYS_PER_MONTH).map { IfcDate.Regular(month.year, month.month, it) } }
    val regularDates = remember(regularDays) { regularDays.map { it.toLocalDate() } }
    val leapDayDate = remember(leapDay) { leapDay?.toLocalDate() }
    val todayIndex = remember(regularDates, today) { today?.let(regularDates::indexOf)?.takeIf { it >= 0 } }
    val todayIsLeapDay = leapDayDate != null && today == leapDayDate
    val containsToday = todayIndex != null || todayIsLeapDay
    val eventCount =
        remember(regularDates, leapDayDate, eventDates) {
            regularDates.count { it in eventDates } + (if (leapDayDate != null && leapDayDate in eventDates) 1 else 0)
        }
    val todayDayName =
        when {
            todayIsLeapDay -> formatter.formatDay(requireNotNull(leapDay))
            todayIndex != null -> formatter.formatDay(regularDays[todayIndex])
            else -> null
        }

    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    val eventsSentence =
        eventCount.takeIf { it > 0 }?.let { count ->
            pluralStringResource(R.plurals.year_mini_month_events, count, count)
        }
    val leapDaySentence = if (leapDay != null) stringResource(R.string.year_mini_month_leap_day_note) else null
    val todaySentence = todayDayName?.let { name -> stringResource(R.string.year_mini_month_today, name) }
    val description =
        listOfNotNull(formatter.monthTitle(month), leapDaySentence, eventsSentence, todaySentence)
            .joinToString(" ")

    Column(
        modifier =
            modifier
                .clip(shape)
                .then(if (containsToday) Modifier.border(TileBorderWidth, colors.primary, shape) else Modifier)
                .selectable(selected = false, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = description }
                .testTag(YearOverviewTestTags.MINI_MONTH_TILE_PREFIX + month.month.number)
                .padding(TilePadding),
        verticalArrangement = Arrangement.spacedBy(TileContentSpacing),
    ) {
        Text(
            text = formatter.monthName(month.month),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(MINI_GRID_ASPECT_RATIO)) {
            val cellWidth = size.width / GRID_COLUMNS
            val cellHeight = size.height / GRID_ROWS
            val inset = minOf(cellWidth, cellHeight) * CELL_INSET_FRACTION
            for (index in regularDates.indices) {
                val row = index / GRID_COLUMNS
                val column = index % GRID_COLUMNS
                val center = Offset(cellWidth * (column + 0.5f), cellHeight * (row + 0.5f))
                drawRoundRect(
                    color = colors.surfaceVariant,
                    topLeft = Offset(cellWidth * column + inset, cellHeight * row + inset),
                    size = Size(cellWidth - 2 * inset, cellHeight - 2 * inset),
                    cornerRadius = CornerRadius(inset),
                )
                if (regularDates[index] in eventDates) {
                    drawCircle(
                        color = colors.primary,
                        radius = minOf(cellWidth, cellHeight) * EVENT_DOT_RADIUS_FRACTION,
                        center = center,
                    )
                }
                if (regularDates[index] == today) {
                    drawCircle(
                        color = colors.primary,
                        radius = minOf(cellWidth, cellHeight) * TODAY_RING_RADIUS_FRACTION,
                        center = center,
                        style = Stroke(width = TileBorderWidth.toPx()),
                    )
                }
            }
        }
        if (leapDay != null) {
            LeapDayIndicator(
                hasEvent = leapDayDate != null && leapDayDate in eventDates,
                isToday = todayIsLeapDay,
            )
        }
    }
}

/**
 * The non-interactive note that Leap Day belongs to June (spec §7.2): the same intercalary icon as
 * [IntercalaryBand], the label, an event dot and a hollow today ring — never colour alone — but no
 * click target of its own, so June's tile stays one semantics node (see [YearMiniMonthTile]).
 */
@Composable
private fun LeapDayIndicator(
    hasEvent: Boolean,
    isToday: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        horizontalArrangement = Arrangement.spacedBy(IntercalaryRowSpacing),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_intercalary),
            contentDescription = null,
            tint = colors.tertiary,
            modifier =
                Modifier
                    .size(IntercalaryIconSize)
                    .then(
                        if (isToday) {
                            Modifier.border(TileBorderWidth, colors.primary, MaterialTheme.shapes.small)
                        } else {
                            Modifier
                        },
                    ),
        )
        Text(
            text = stringResource(R.string.intercalary_leap_day),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        DayMarks(eventCount = if (hasEvent) 1 else 0, hasHoliday = false)
    }
}

/**
 * The Year overview's fourteenth tile: Year Day, the day that belongs to no month (spec §2.4;
 * FEATURES C6). A sibling of [YearMiniMonthTile] rather than a reuse of [IntercalaryBand] — the band is
 * built to span a month grid's seven columns, and dropping that full-width, filled pill into the year's
 * tile grid put a shape and a colour on screen that nothing around it shared (owner feedback,
 * 2026-09-19). This tile borrows the mini-month's own vocabulary instead: the same [MaterialTheme.shapes]
 * container with no fill, the same `titleSmall` heading, the same today border, and the same
 * [DayMarks] row, so the year reads as one grid.
 *
 * It is still visibly *not* a month. The intercalary icon in [colors.tertiary][ColorScheme.tertiary] —
 * the same icon and tint [LeapDayIndicator] already uses inside June's tile — carries that, and the
 * Gregorian date beneath says which real day it is. Shape and glyph, never colour alone (CLAUDE.md
 * rule 3).
 *
 * @param yearDay the year's [IfcDate.YearDay].
 * @param isToday whether the real today is Year Day, matched on its Gregorian date (CLAUDE.md rule 4).
 * @param hasEvent whether Year Day carries at least one event.
 * @param onClick invoked when the tile is tapped; the Year screen opens December with it.
 * @param modifier applied to the tile.
 * @param formatter formats the label and the Gregorian subtitle.
 */
@Composable
fun YearDayTile(
    yearDay: IfcDate.YearDay,
    isToday: Boolean,
    hasEvent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    formatter: IfcDateFormatter = rememberIfcDateFormatter(),
) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    val description = formatter.dayDescription(yearDay, isToday, if (hasEvent) 1 else 0, null)

    Column(
        modifier =
            modifier
                .clip(shape)
                .then(if (isToday) Modifier.border(TileBorderWidth, colors.primary, shape) else Modifier)
                .selectable(selected = false, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = description }
                .padding(TilePadding),
        verticalArrangement = Arrangement.spacedBy(TileContentSpacing),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(IntercalaryRowSpacing),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_intercalary),
                contentDescription = null,
                tint = colors.tertiary,
                modifier = Modifier.size(IntercalaryIconSize),
            )
            Text(
                text = formatter.formatDay(yearDay),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = formatter.intercalarySubtitle(yearDay),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            maxLines = YEAR_DAY_SUBTITLE_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
        )
        DayMarks(eventCount = if (hasEvent) 1 else 0, hasHoliday = false)
    }
}

/** Lines the Year Day tile's Gregorian subtitle may take before it ellipsises. */
private const val YEAR_DAY_SUBTITLE_MAX_LINES = 2
