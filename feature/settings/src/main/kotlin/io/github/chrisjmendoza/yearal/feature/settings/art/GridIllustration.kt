package io.github.chrisjmendoza.yearal.feature.settings.art

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.chrisjmendoza.yearal.core.calendar.IfcDate
import io.github.chrisjmendoza.yearal.core.calendar.IfcMonth
import io.github.chrisjmendoza.yearal.core.calendar.IfcYearMonth
import io.github.chrisjmendoza.yearal.core.designsystem.calendar.WeekdayHeaders
import io.github.chrisjmendoza.yearal.core.designsystem.format.IfcDateFormatter
import io.github.chrisjmendoza.yearal.core.designsystem.format.rememberIfcDateFormatter
import io.github.chrisjmendoza.yearal.core.designsystem.theme.Dimens
import io.github.chrisjmendoza.yearal.core.designsystem.theme.IfcTheme
import io.github.chrisjmendoza.yearal.core.designsystem.theme.PillShape
import io.github.chrisjmendoza.yearal.core.designsystem.theme.YearalTheme
import io.github.chrisjmendoza.yearal.core.domain.settings.WeekdayDisplay
import io.github.chrisjmendoza.yearal.feature.settings.R
import java.time.format.TextStyle
import io.github.chrisjmendoza.yearal.core.designsystem.R as DesignSystemR

/**
 * Which "perfect month" illustration [GridIllustration] draws (`docs/design-plan.md` §4.8, ROADMAP
 * wave 3 J3; redrawn for clarity on 2026-09-27 after the owner's device test found the unlabelled
 * versions left "room for confusion"). No bitmap assets: every illustration is built from the design
 * tokens and the app's own grid components, the same "4 × 7 grid is the design language" constraint
 * the rest of the app follows (design-plan §2), so an illustration always reads as the calendar's own
 * shape rather than decoration bolted onto it. Every element a first-time user could misread is
 * labelled: month initials over the month blocks, the Month screen's own weekday headers over the dot
 * grid, day numbers where the text refers to a specific day, and the month's name over the Year Day grid.
 */
enum class GridIllustrationVariant {
    /**
     * A 13-block strip standing in for the year's 13 months, each block under its locale's month
     * initial (J F M A M J S J A S O N D in English), with the seventh block — Sol — picked out in
     * [YearalTheme.colors]`.intercalaryContainer`, drawn as a pill, and named beneath. Illustrates
     * "what the IFC is": one extra month, always in the same place, between June and July.
     */
    THIRTEEN_MONTHS,

    /**
     * One IFC month as the 4 × 7 dot grid, under the Month screen's own two header rows
     * ([WeekdayHeaders], `BOTH`: the IFC weekdays and that month's actual weekdays), with the column
     * holding [GridIllustration]'s `example` day ringed in [YearalTheme.colors]`.todayRing` and its four
     * dots numbered, plus two caption rows naming that column's IFC (nominal) and real (actual)
     * weekday. Illustrates why the two differ (calendar-spec §4.1). Requires `example`.
     */
    NOMINAL_VS_ACTUAL,

    /**
     * December as the 4 × 7 dot grid — its name above, `1` and `28` on the first and last dots — with
     * two pills drawn beneath it, outside the grid: Year Day, filled in
     * [YearalTheme.colors]`.intercalaryContainer` with the [DesignSystemR.drawable.ic_intercalary]
     * icon, and Leap Day in outline only, since it exists only in leap years (CLAUDE.md rule 6).
     * Illustrates that both belong to no week.
     */
    YEAR_DAY,
}

// Values live in Dimens (docs/design-plan.md §3.1) where a token already exists; the rest are this
// illustration's own, since no other component shares them.
private val IllustrationPadding = Dimens.SpaceM
private val IllustrationContentSpacing = Dimens.SpaceS
private const val GRID_COLUMNS = 7
private const val GRID_ROWS = 4

/** Read from the enum rather than hard-coded (CLAUDE.md rule 1's spirit: no calendar fact typed as a literal). */
private val SOL_INDEX = IfcMonth.SOL.number - 1
private val MonthBlockHeight = 72.dp
private const val MONTH_BLOCK_WIDTH_FRACTION = 0.72f
private val MonthBlockCorner = 3.dp

/**
 * Dots are sized in `sp`, not `dp`, so they grow with the user's font scale and the day number inside
 * never outgrows its dot (design-plan §2: 200% font scale never clips).
 */
private val DotDiameter = 30.sp
private val DotRingWidth = 2.dp
private val DotRowSpacing = Dimens.SpaceXs
private val PillIconSize = 20.dp
private val PillOutlineWidth = 1.dp

/**
 * A grid-based illustration of one fact about the IFC, drawn purely from the design system's tokens and
 * components — no bitmap assets (`docs/design-plan.md` §4.8). A card on
 * [YearalTheme.colors]`.cardContainer` shaped `MaterialTheme.shapes.large`, sized to the caller's width
 * and to its own content (never a fixed aspect ratio, so a large font scale grows the card rather than
 * squeezing the drawing). Placed atop each first-run intro page and reused as the matching Learn
 * section's header (`docs/ARCHITECTURE.md` §4, the intro/Learn bullets).
 *
 * Built from Compose layout rather than a `Canvas`, so the labels and the shapes they label share one
 * coordinate system — and mirror together under a right-to-left locale.
 *
 * Colour is never the only signal (design-plan §2): the highlighted elements in every variant also
 * differ in shape from the rest — a pill in place of a block ([monthBlockShape]'s Sol), a ring, a pill
 * outside the grid — and the whole illustration is described to TalkBack by [contentDescription]
 * rather than left for a screen reader to interpret dot by dot ([Modifier.clearAndSetSemantics] hides
 * the decorative content, the header rows' own descriptions and the caption text from the accessibility
 * tree in favour of that one description).
 *
 * The plain dots and blocks are [YearalTheme.colors]`.miniGridCell`, **not** `.gridCell`: they sit on a
 * `cardContainer` card, where `gridCell` is the same Material tier in dark mode, so every plain element
 * vanished and only the highlights showed (owner device report, 2026-09-27; the same trap as the Year
 * overview's mini-grid, design-pass fix R11). `GridIllustrationPixelTest` pins it.
 *
 * @param variant which fact to illustrate.
 * @param contentDescription what the illustration shows, read by TalkBack in place of its decorative
 * content; the caller supplies it (like `ExplainerInfoButton`) so this component carries no calendar
 * copy of its own.
 * @param modifier applied to the illustration's card.
 * @param example the worked example day for [GridIllustrationVariant.NOMINAL_VS_ACTUAL]: its month
 * drives the two header rows, its weekday column is the ringed one, and the caption rows name that
 * column's IFC and actual weekday. Required (and only used) for that variant.
 * @throws IllegalArgumentException if [variant] is [GridIllustrationVariant.NOMINAL_VS_ACTUAL] and
 * [example] is `null`.
 */
@Composable
fun GridIllustration(
    variant: GridIllustrationVariant,
    contentDescription: String,
    modifier: Modifier = Modifier,
    example: IfcDate.Regular? = null,
) {
    val colors = YearalTheme.colors
    val formatter = rememberIfcDateFormatter()
    // Named distinctly from the "contentDescription" property the semantics lambda below sets:
    // inside a lambda with a receiver, an unqualified name resolves to the receiver's own member
    // first, so reusing the parameter's name there would read back an empty description instead of
    // this composable's own parameter (the same reason `IntercalaryBand` calls its local `description`).
    val description = contentDescription
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(colors.cardContainer)
                .clearAndSetSemantics { this.contentDescription = description }
                .padding(IllustrationPadding),
        verticalArrangement = Arrangement.spacedBy(IllustrationContentSpacing),
    ) {
        when (variant) {
            GridIllustrationVariant.THIRTEEN_MONTHS -> {
                MonthStrip(formatter)
            }

            GridIllustrationVariant.NOMINAL_VS_ACTUAL -> {
                require(example != null) { "NOMINAL_VS_ACTUAL requires an example day" }
                // The 1st..7th of any month fill the nominal Sunday..Saturday columns (spec §2.3).
                val column = (example.dayOfMonth - 1) % GRID_COLUMNS
                WeekdayHeaders(month = IfcYearMonth.from(example), display = WeekdayDisplay.BOTH)
                DotGrid(ringedColumn = column, numbered = { _, col -> col == column })
                WeekdayCaptionRow(
                    stringResource(R.string.illustration_nominal_weekday_label),
                    formatter.weekdayName(requireNotNull(example.nominalDayOfWeek)),
                )
                WeekdayCaptionRow(
                    stringResource(R.string.illustration_actual_weekday_label),
                    formatter.weekdayName(example.actualDayOfWeek),
                )
            }

            GridIllustrationVariant.YEAR_DAY -> {
                Text(
                    text = formatter.monthName(IfcMonth.DECEMBER),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onCard,
                )
                DotGrid(
                    ringedColumn = null,
                    numbered = { row, col ->
                        (row == 0 && col == 0) || (row == GRID_ROWS - 1 && col == GRID_COLUMNS - 1)
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
                    IntercalaryPill(stringResource(R.string.illustration_year_day_pill_label), filled = true)
                    IntercalaryPill(stringResource(R.string.illustration_leap_day_pill_label), filled = false)
                }
            }
        }
    }
}

/**
 * The 13-block month strip for [GridIllustrationVariant.THIRTEEN_MONTHS]: each block under its month
 * initial, Sol picked out at [SOL_INDEX] by colour, by shape ([monthBlockShape]: a pill where every
 * other block is a squared-off bar, so a colour-blind or greyscale-display user still sees it —
 * design-plan §2 "colour never alone") and by name beneath it. Initials come from the locale's narrow
 * month names for the twelve namesake months and from the localized "Sol" resource for Sol, so the row
 * reads J F M A M J S J A S O N D in English and mirrors under RTL like any other [Row].
 */
@Composable
private fun MonthStrip(formatter: IfcDateFormatter) {
    val colors = YearalTheme.colors
    val locale = LocalConfiguration.current.locales[0]
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
        IfcMonth.entries.forEachIndexed { index, month ->
            val isSol = index == SOL_INDEX
            val initial =
                month.gregorianNamesake?.getDisplayName(TextStyle.NARROW_STANDALONE, locale)
                    ?: formatter.monthName(month, IfcDateFormatter.MonthNameStyle.SHORT).take(1)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
            ) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSol) colors.intercalary else colors.onCard,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(MONTH_BLOCK_WIDTH_FRACTION)
                            .height(MonthBlockHeight)
                            .clip(monthBlockShape(index))
                            .background(if (isSol) colors.intercalaryContainer else colors.miniGridCell),
                )
                // Only Sol is named: a full month name under every block would collide long before
                // 200% font scale, and the initials already give the other twelve their place.
                Text(
                    text = if (isSol) formatter.monthName(month) else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.intercalary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * The shape [MonthStrip] gives the block at [index]: Sol ([SOL_INDEX]) is a full pill, every other
 * month a bar with [MonthBlockCorner]'s small rounding — the shape difference that keeps the highlight
 * legible without colour. A pure function so `MonthStripGeometryTest` can assert it without rendering.
 */
internal fun monthBlockShape(index: Int): Shape =
    if (index ==
        SOL_INDEX
    ) {
        PillShape
    } else {
        RoundedCornerShape(MonthBlockCorner)
    }

/**
 * The 4 × 7 dot grid shared by [GridIllustrationVariant.NOMINAL_VS_ACTUAL] and `.YEAR_DAY`: 28 dots in
 * the Month grid's own Sunday-first columns, [ringedColumn] outlined in `todayRing`, and the day number
 * drawn inside every dot [numbered] says so — a sparse numbering (the example day's column, or just the
 * 1 and the 28) stays legible at 200% font where all 28 would not.
 */
@Composable
private fun DotGrid(
    ringedColumn: Int?,
    numbered: (row: Int, column: Int) -> Boolean,
) {
    val colors = YearalTheme.colors
    val dotSize = with(LocalDensity.current) { DotDiameter.toDp() }
    Column(verticalArrangement = Arrangement.spacedBy(DotRowSpacing)) {
        for (row in 0 until GRID_ROWS) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (column in 0 until GRID_COLUMNS) {
                    val ringed = column == ringedColumn
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Box(
                            modifier =
                                Modifier
                                    .size(dotSize)
                                    .clip(CircleShape)
                                    .background(colors.miniGridCell)
                                    .then(
                                        if (ringed) {
                                            Modifier.border(DotRingWidth, colors.todayRing, CircleShape)
                                        } else {
                                            Modifier
                                        },
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (numbered(row, column)) {
                                Text(
                                    text = (row * GRID_COLUMNS + column + 1).toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (ringed) colors.todayRing else colors.onCard,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** One "<label>: <value>" caption row under [GridIllustrationVariant.NOMINAL_VS_ACTUAL]'s dot grid. */
@Composable
private fun WeekdayCaptionRow(
    label: String,
    value: String,
) {
    Text(
        text = stringResource(R.string.illustration_weekday_caption, label, value),
        style = MaterialTheme.typography.labelSmall,
        color = YearalTheme.colors.onCard,
    )
}

/**
 * A pill outside [GridIllustrationVariant.YEAR_DAY]'s grid: [filled] for Year Day (every year), an
 * outline for Leap Day (leap years only) — the fill itself carries the "always vs sometimes" difference.
 */
@Composable
private fun IntercalaryPill(
    label: String,
    filled: Boolean,
) {
    val colors = YearalTheme.colors
    val contentColor = if (filled) colors.onIntercalaryContainer else colors.intercalary
    Row(
        modifier =
            Modifier
                .clip(PillShape)
                .then(
                    if (filled) {
                        Modifier.background(colors.intercalaryContainer)
                    } else {
                        Modifier.border(PillOutlineWidth, colors.intercalary, PillShape)
                    },
                ).padding(horizontal = Dimens.SpaceM, vertical = Dimens.SpaceXs),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(DesignSystemR.drawable.ic_intercalary),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.height(PillIconSize),
        )
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = contentColor)
    }
}

// Previews — light, dark and 200% font scale, dynamic colour off for determinism (WORKFLOW §3). The
// worked example is the spec's own: Gregorian Thursday, September 17, 2026 = IFC September 8, 2026.

@Preview(name = "Thirteen months", showBackground = true)
@Composable
private fun GridIllustrationThirteenMonthsPreview() {
    IllustrationPreview(GridIllustrationVariant.THIRTEEN_MONTHS)
}

@Preview(name = "Thirteen months — dark", showBackground = true)
@Composable
private fun GridIllustrationThirteenMonthsDarkPreview() {
    IllustrationPreview(GridIllustrationVariant.THIRTEEN_MONTHS, darkTheme = true)
}

@Preview(name = "Thirteen months — font 2.0", showBackground = true, fontScale = 2f)
@Composable
private fun GridIllustrationThirteenMonthsLargeFontPreview() {
    IllustrationPreview(GridIllustrationVariant.THIRTEEN_MONTHS)
}

@Preview(name = "Nominal vs actual", showBackground = true)
@Composable
private fun GridIllustrationNominalVsActualPreview() {
    IllustrationPreview(GridIllustrationVariant.NOMINAL_VS_ACTUAL)
}

@Preview(name = "Nominal vs actual — dark", showBackground = true)
@Composable
private fun GridIllustrationNominalVsActualDarkPreview() {
    IllustrationPreview(GridIllustrationVariant.NOMINAL_VS_ACTUAL, darkTheme = true)
}

@Preview(name = "Nominal vs actual — font 2.0", showBackground = true, fontScale = 2f)
@Composable
private fun GridIllustrationNominalVsActualLargeFontPreview() {
    IllustrationPreview(GridIllustrationVariant.NOMINAL_VS_ACTUAL)
}

@Preview(name = "Year Day", showBackground = true)
@Composable
private fun GridIllustrationYearDayPreview() {
    IllustrationPreview(GridIllustrationVariant.YEAR_DAY)
}

@Preview(name = "Year Day — dark", showBackground = true)
@Composable
private fun GridIllustrationYearDayDarkPreview() {
    IllustrationPreview(GridIllustrationVariant.YEAR_DAY, darkTheme = true)
}

@Preview(name = "Year Day — font 2.0", showBackground = true, fontScale = 2f)
@Composable
private fun GridIllustrationYearDayLargeFontPreview() {
    IllustrationPreview(GridIllustrationVariant.YEAR_DAY)
}

@Composable
private fun IllustrationPreview(
    variant: GridIllustrationVariant,
    darkTheme: Boolean = false,
) {
    IfcTheme(darkTheme = darkTheme, dynamicColor = false) {
        Box(modifier = Modifier.padding(Dimens.SpaceM)) {
            GridIllustration(
                variant = variant,
                contentDescription = "Preview illustration",
                example = IfcDate.Regular(2026, IfcMonth.SEPTEMBER, 8),
            )
        }
    }
}
