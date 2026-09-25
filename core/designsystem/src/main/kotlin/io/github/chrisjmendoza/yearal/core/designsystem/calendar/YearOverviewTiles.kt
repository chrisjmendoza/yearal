package io.github.chrisjmendoza.yearal.core.designsystem.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import io.github.chrisjmendoza.yearal.core.calendar.IfcDate
import io.github.chrisjmendoza.yearal.core.calendar.IfcMonth
import io.github.chrisjmendoza.yearal.core.calendar.IfcYearMonth
import io.github.chrisjmendoza.yearal.core.designsystem.R
import io.github.chrisjmendoza.yearal.core.designsystem.format.IfcDateFormatter
import io.github.chrisjmendoza.yearal.core.designsystem.format.rememberIfcDateFormatter
import io.github.chrisjmendoza.yearal.core.designsystem.theme.Dimens
import io.github.chrisjmendoza.yearal.core.designsystem.theme.PillShape
import io.github.chrisjmendoza.yearal.core.designsystem.theme.YearalTheme
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

    /** Tag of the mini grid inside a tile — the `Canvas` the 28 day squares are drawn on. */
    const val MINI_GRID: String = "ifc:yearMiniGrid"
}

// TilePadding and TileBorderWidth come from Dimens (docs/design-plan.md §3.1) rather than their own
// literals now that the tile is a card sharing the grid's own spacing and today-ring scale.
private val TilePadding = Dimens.SpaceM
private val TileContentSpacing = 6.dp
private val TileBorderWidth = Dimens.TodayRingWidth
private val IntercalaryIconSize = 16.dp
private val IntercalaryRowSpacing = 6.dp
private val LeapDayPillHorizontalPadding = Dimens.SpaceS
private val LeapDayPillVerticalPadding = Dimens.SpaceXs
private const val MINI_GRID_ASPECT_RATIO = GRID_COLUMNS.toFloat() / GRID_ROWS.toFloat()

// Geometry of one mini-grid square, all as fractions of the square's shorter side so the same drawing
// holds from the grid's 160dp minimum tile (about 19dp squares) up to a tablet's. The layout inside a
// square mirrors DayCell top to bottom — the day number, then the marks row — just without the
// Gregorian corner number, which has no room here.
private const val CELL_INSET_FRACTION = 0.06f
private const val CELL_CORNER_FRACTION = 0.12f

/** Cap on the day number's font size: half the square, so two digits never touch the marks row. */
private const val NUMBER_SIZE_FRACTION = 0.5f
private const val NUMBER_CENTER_FRACTION = 0.38f
private const val MARK_ROW_CENTER_FRACTION = 0.80f
private const val MARK_RADIUS_FRACTION = 0.10f

/** A diamond's half-diagonal relative to the dot's radius, so both marks carry the same visual weight. */
private const val DIAMOND_SCALE = 1.2f

/** Each mark's offset from the square's centre line, in dot radii, when a day carries both marks. */
private const val MARK_PAIR_OFFSET = 1.4f

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
 * The tile is a card (design-plan §4.3 "Every mini-month is a card"): [YearalTheme.colors]`.cardContainer`
 * fill on `MaterialTheme.shapes.medium`. Each of the 28 squares is a small [DayCell] in miniature
 * ([MiniMonthGrid]): its fill is `.miniGridCell` — **not** `.gridCell`, which is the same Material
 * role as the card and made every square invisible (fix R11) — its IFC day number sits in the upper
 * part in `.onCard`, and the marks row beneath: a `.holidayMark` diamond for a holiday (design-plan
 * §4.3 "a holiday diamond appears in the mini grid too", since [holidays] carries the data already), a
 * `.eventMark` dot for a day with an event occurrence, both side by side when a day has both (fix
 * design-pass 9: neither mark is ever dropped). A marked day's square steps up to `.miniGridCellMarked`,
 * the convention [DayCell] uses with `.gridCellMarked`. Today is marked two ways, never by colour alone
 * (docs/ARCHITECTURE.md §4 "Accessibility"): a `.todayRing` border around the whole tile when it
 * contains the real today (a regular day or, for June, Leap Day), and inside the grid a `.todayRing`
 * ring around that exact square with its number bold in `.todayText`, exactly as [DayCell] marks it.
 *
 * @param month the month this tile shows.
 * @param today the real today as a Gregorian date, or `null` to mark nothing.
 * @param eventDates dates with at least one event occurrence, from `ObserveAgendaUseCase.presence`.
 * @param onClick invoked when the tile — including its Leap Day indicator — is tapped.
 * @param modifier applied to the tile; it fills the width the grid cell gives it.
 * @param formatter supplies the month name, the day numbers and the day names spoken in the description.
 * @param holidays dates with a holiday this month, keyed the same way [eventDates] is (a presence
 * set, not a name map — the mini grid draws a mark, never a label). Defaults to empty so every
 * existing caller is unaffected.
 */
@Composable
fun YearMiniMonthTile(
    month: IfcYearMonth,
    today: LocalDate?,
    eventDates: Set<LocalDate>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    formatter: IfcDateFormatter = rememberIfcDateFormatter(),
    holidays: Set<LocalDate> = emptySet(),
) {
    val leapDay = month.trailingIntercalary as? IfcDate.LeapDay
    val regularDays =
        remember(month) { (1..IfcMonth.DAYS_PER_MONTH).map { IfcDate.Regular(month.year, month.month, it) } }
    val regularDates = remember(regularDays) { regularDays.map { it.toLocalDate() } }
    val dayNumbers = remember(regularDays, formatter) { regularDays.map { formatter.formatNumber(it.dayOfMonth) } }
    val leapDayDate = remember(leapDay) { leapDay?.toLocalDate() }
    val todayIndex = remember(regularDates, today) { today?.let(regularDates::indexOf)?.takeIf { it >= 0 } }
    val todayIsLeapDay = leapDayDate != null && today == leapDayDate
    val containsToday = todayIndex != null || todayIsLeapDay
    val eventCount =
        remember(regularDates, leapDayDate, eventDates) {
            regularDates.count { it in eventDates } + (if (leapDayDate != null && leapDayDate in eventDates) 1 else 0)
        }
    val holidayCount = remember(regularDates, holidays) { regularDates.count { it in holidays } }
    val todayDayName =
        when {
            todayIsLeapDay -> formatter.formatDay(requireNotNull(leapDay))
            todayIndex != null -> formatter.formatDay(regularDays[todayIndex])
            else -> null
        }

    val yearalColors = YearalTheme.colors
    val shape = MaterialTheme.shapes.medium
    val eventsSentence =
        eventCount.takeIf { it > 0 }?.let { count ->
            pluralStringResource(R.plurals.year_mini_month_events, count, count)
        }
    val holidaysSentence =
        holidayCount.takeIf { it > 0 }?.let { count ->
            pluralStringResource(R.plurals.year_mini_month_holidays, count, count)
        }
    val leapDaySentence = if (leapDay != null) stringResource(R.string.year_mini_month_leap_day_note) else null
    val todaySentence = todayDayName?.let { name -> stringResource(R.string.year_mini_month_today, name) }
    val description =
        listOfNotNull(formatter.monthTitle(month), leapDaySentence, eventsSentence, holidaysSentence, todaySentence)
            .joinToString(" ")

    Column(
        modifier =
            modifier
                .clip(shape)
                .background(yearalColors.cardContainer)
                .then(if (containsToday) Modifier.border(TileBorderWidth, yearalColors.todayRing, shape) else Modifier)
                .selectable(selected = false, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = description }
                .testTag(YearOverviewTestTags.MINI_MONTH_TILE_PREFIX + month.month.number)
                .padding(TilePadding),
        verticalArrangement = Arrangement.spacedBy(TileContentSpacing),
    ) {
        Text(
            text = formatter.monthName(month.month),
            style = MaterialTheme.typography.titleSmall,
            // Explicit, like YearDayTile's: the tile is a card, so its heading is onCard rather than
            // whatever LocalContentColor the host happens to provide.
            color = yearalColors.onCard,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        MiniMonthGrid(
            regularDates = regularDates,
            dayNumbers = dayNumbers,
            todayIndex = todayIndex,
            eventDates = eventDates,
            holidays = holidays,
        )
        if (leapDay != null) {
            LeapDayIndicator(
                hasEvent = leapDayDate != null && leapDayDate in eventDates,
                isToday = todayIsLeapDay,
            )
        }
    }
}

/**
 * The 4 × 7 grid of a [YearMiniMonthTile]: one [Canvas] drawing 28 squares, each with its day number
 * and marks (see the tile's KDoc for what each square shows and why it is a `Canvas` at all).
 *
 * The numbers are laid out **once per size**, not once per frame: a [BoxWithConstraints] gives the
 * square size at composition, the font size is derived from it, and the 28 [TextLayoutResult]s are
 * `remember`ed so the draw pass only blits them — measuring 28 strings inside `onDraw` would put 392
 * text layouts on every scrolled frame of the Year screen, the very cost the `Canvas` exists to avoid.
 *
 * The font size is `labelSmall`'s, capped at [NUMBER_SIZE_FRACTION] of the square so it still fits
 * at font scale 2.0 (docs/ARCHITECTURE.md §4, "font scale 2.0"): at the default scale the cap never
 * bites on a phone, and at 200% the number shrinks to fit rather than overflowing its square — the
 * tile's own title and the merged description carry the accessible reading, so a small number here
 * costs nothing that TalkBack or the large-font user relies on.
 *
 * @param regularDates the month's 28 regular days as Gregorian dates, in grid order (CLAUDE.md rule 4:
 * the marks' keys are Gregorian).
 * @param dayNumbers the formatted IFC day numbers, parallel to [regularDates].
 * @param todayIndex index into [regularDates] of the real today, or `null`.
 * @param eventDates dates with an event occurrence.
 * @param holidays dates with a holiday.
 * @param modifier applied to the grid; it fills the tile's width at the 7:4 aspect ratio.
 */
@Composable
private fun MiniMonthGrid(
    regularDates: List<LocalDate>,
    dayNumbers: List<String>,
    todayIndex: Int?,
    eventDates: Set<LocalDate>,
    holidays: Set<LocalDate>,
    modifier: Modifier = Modifier,
) {
    val yearalColors = YearalTheme.colors
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val baseStyle = MaterialTheme.typography.labelSmall
    BoxWithConstraints(modifier = modifier.fillMaxWidth().aspectRatio(MINI_GRID_ASPECT_RATIO)) {
        val squareSizePx = with(density) { minOf(maxWidth / GRID_COLUMNS, maxHeight / GRID_ROWS).toPx() }
        val numberStyle =
            baseStyle.copy(
                fontSize = fitNumberSize(baseStyle.fontSize, squareSizePx, density),
                // labelSmall's 16sp line height would centre the glyph in a box taller than the square
                // allows; the font's natural line height keeps the number where the square's centre is.
                lineHeight = TextUnit.Unspecified,
                color = yearalColors.onCard,
            )
        val todayStyle = numberStyle.copy(color = yearalColors.todayText, fontWeight = FontWeight.Bold)
        val numberLayouts =
            remember(measurer, dayNumbers, numberStyle, todayStyle, todayIndex) {
                dayNumbers.mapIndexed { index, label ->
                    measurer.measure(label, if (index == todayIndex) todayStyle else numberStyle, maxLines = 1)
                }
            }
        Canvas(modifier = Modifier.fillMaxSize().testTag(YearOverviewTestTags.MINI_GRID)) {
            val cellWidth = size.width / GRID_COLUMNS
            val cellHeight = size.height / GRID_ROWS
            val cellMin = minOf(cellWidth, cellHeight)
            val inset = cellMin * CELL_INSET_FRACTION
            val cornerRadius = CornerRadius(cellMin * CELL_CORNER_FRACTION)
            val markRadius = cellMin * MARK_RADIUS_FRACTION
            val ringWidth = TileBorderWidth.toPx()
            for (index in regularDates.indices) {
                val row = index / GRID_COLUMNS
                val column = index % GRID_COLUMNS
                val topLeft = Offset(cellWidth * column + inset, cellHeight * row + inset)
                val squareSize = Size(cellWidth - 2 * inset, cellHeight - 2 * inset)
                val centerX = topLeft.x + squareSize.width / 2
                val date = regularDates[index]
                val hasHoliday = date in holidays
                val hasEvent = date in eventDates
                drawRoundRect(
                    color = if (hasHoliday || hasEvent) yearalColors.miniGridCellMarked else yearalColors.miniGridCell,
                    topLeft = topLeft,
                    size = squareSize,
                    cornerRadius = cornerRadius,
                )
                val layout = numberLayouts[index]
                drawText(
                    textLayoutResult = layout,
                    topLeft =
                        Offset(
                            centerX - layout.size.width / 2f,
                            topLeft.y + squareSize.height * NUMBER_CENTER_FRACTION - layout.size.height / 2f,
                        ),
                )
                val markY = topLeft.y + squareSize.height * MARK_ROW_CENTER_FRACTION
                when {
                    hasHoliday && hasEvent -> {
                        drawDiamond(
                            yearalColors.holidayMark,
                            Offset(centerX - markRadius * MARK_PAIR_OFFSET, markY),
                            markRadius,
                        )
                        drawCircle(
                            yearalColors.eventMark,
                            markRadius,
                            Offset(centerX + markRadius * MARK_PAIR_OFFSET, markY),
                        )
                    }

                    hasHoliday -> {
                        drawDiamond(yearalColors.holidayMark, Offset(centerX, markY), markRadius)
                    }

                    hasEvent -> {
                        drawCircle(yearalColors.eventMark, markRadius, Offset(centerX, markY))
                    }
                }
                if (index == todayIndex) {
                    drawRoundRect(
                        color = yearalColors.todayRing,
                        topLeft = Offset(topLeft.x + ringWidth / 2, topLeft.y + ringWidth / 2),
                        size = Size(squareSize.width - ringWidth, squareSize.height - ringWidth),
                        cornerRadius = cornerRadius,
                        style = Stroke(width = ringWidth),
                    )
                }
            }
        }
    }
}

/**
 * The day number's font size for a square [squareSizePx] wide: [base] (`labelSmall`, already in sp so
 * the user's font scale applies when it is drawn) unless that would exceed [NUMBER_SIZE_FRACTION] of
 * the square, in which case the size that fits. Both sides are compared in sp, so the comparison
 * already accounts for the font scale [density] carries.
 */
private fun fitNumberSize(
    base: TextUnit,
    squareSizePx: Float,
    density: Density,
): TextUnit {
    val fit = with(density) { (squareSizePx * NUMBER_SIZE_FRACTION).toSp() }
    return if (base.isSp && base.value <= fit.value) base else fit
}

/** A filled diamond centred on [center] whose half-diagonal is [DIAMOND_SCALE] × [radius]. */
private fun DrawScope.drawDiamond(
    color: Color,
    center: Offset,
    radius: Float,
) {
    val half = radius * DIAMOND_SCALE
    val path =
        Path().apply {
            moveTo(center.x, center.y - half)
            lineTo(center.x + half, center.y)
            lineTo(center.x, center.y + half)
            lineTo(center.x - half, center.y)
            close()
        }
    drawPath(path, color)
}

/**
 * The non-interactive note that Leap Day belongs to June (spec §7.2): the same intercalary icon as
 * [IntercalaryBand], the label, an event dot and a hollow today ring — never colour alone — but no
 * click target of its own, so June's tile stays one semantics node (see [YearMiniMonthTile]).
 *
 * Drawn as a small pill on [YearalTheme.colors]`.intercalaryContainer` (design-plan §4.3/§8 decision 4
 * — see [YearDayTile]'s KDoc for why the fill returned), the same [PillShape] the full-width
 * [IntercalaryBand] uses, scaled down to sit inside a mini-month tile.
 */
@Composable
private fun LeapDayIndicator(
    hasEvent: Boolean,
    isToday: Boolean,
) {
    val yearalColors = YearalTheme.colors
    Row(
        modifier =
            Modifier
                .clip(PillShape)
                .background(yearalColors.intercalaryContainer)
                .padding(horizontal = LeapDayPillHorizontalPadding, vertical = LeapDayPillVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(IntercalaryRowSpacing),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_intercalary),
            contentDescription = null,
            tint = yearalColors.intercalary,
            modifier =
                Modifier
                    .size(IntercalaryIconSize)
                    .then(
                        if (isToday) {
                            Modifier.border(TileBorderWidth, yearalColors.todayRing, MaterialTheme.shapes.small)
                        } else {
                            Modifier
                        },
                    ),
        )
        Text(
            text = stringResource(R.string.intercalary_leap_day),
            style = MaterialTheme.typography.bodySmall,
            color = yearalColors.onIntercalaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        DayMarks(eventCount = if (hasEvent) 1 else 0, hasHoliday = false)
    }
}

/**
 * The Year overview's fourteenth tile: Year Day, the day that belongs to no month (spec §2.4;
 * FEATURES C6). A sibling of [YearMiniMonthTile] rather than a reuse of [IntercalaryBand] — the band is
 * built to span a month grid's seven columns, and this tile borrows the mini-month's own vocabulary
 * instead: the same `titleSmall` heading, the same today border, and the same [DayMarks] row, so the
 * year reads as one grid.
 *
 * **The intercalary fill is back.** It was removed on 2026-09-19 because a filled, full-width pill
 * dropped into the year's tile grid "put a shape and a colour on screen that nothing around it
 * shared" — at the time, no other component used that accent, so the tile alone looked like a mistake.
 * The owner's later call (design-plan §8 decision 4, 2026-09-23) was that the *fill* was never the
 * problem, only that nothing around it matched it: "I never wanted it gone, just styling to be more
 * in line." Now that [IntercalaryBand], the Day detail header and [LeapDayIndicator] all share
 * [YearalTheme.colors]`.intercalaryContainer`, the same fill here reads as one consistent accent
 * rather than an outlier, so it returns: [YearalTheme.colors]`.intercalaryContainer` on
 * `MaterialTheme.shapes.medium`, content in `.onIntercalaryContainer`, the intercalary icon tinted
 * `.intercalary`.
 *
 * It is still visibly *not* a month: the intercalary icon and the fill itself carry that, and the
 * Gregorian date beneath says which real day it is. Shape, fill and glyph together, never colour
 * alone (CLAUDE.md rule 3).
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
    val yearalColors = YearalTheme.colors
    val shape = MaterialTheme.shapes.medium
    val description = formatter.dayDescription(yearDay, isToday, if (hasEvent) 1 else 0, null)

    Column(
        modifier =
            modifier
                .clip(shape)
                .background(yearalColors.intercalaryContainer)
                .then(if (isToday) Modifier.border(TileBorderWidth, yearalColors.todayRing, shape) else Modifier)
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
                tint = yearalColors.intercalary,
                modifier = Modifier.size(IntercalaryIconSize),
            )
            Text(
                text = formatter.formatDay(yearDay),
                style = MaterialTheme.typography.titleSmall,
                color = yearalColors.onIntercalaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = formatter.intercalarySubtitle(yearDay),
            style = MaterialTheme.typography.bodySmall,
            color = yearalColors.onIntercalaryContainer,
            maxLines = YEAR_DAY_SUBTITLE_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
        )
        DayMarks(eventCount = if (hasEvent) 1 else 0, hasHoliday = false)
    }
}

/** Lines the Year Day tile's Gregorian subtitle may take before it ellipsises. */
private const val YEAR_DAY_SUBTITLE_MAX_LINES = 2
