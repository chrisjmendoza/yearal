package io.github.chrisjmendoza.yearal.feature.settings.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.chrisjmendoza.yearal.core.calendar.IfcMonth
import io.github.chrisjmendoza.yearal.core.designsystem.theme.Dimens
import io.github.chrisjmendoza.yearal.core.designsystem.theme.IfcTheme
import io.github.chrisjmendoza.yearal.core.designsystem.theme.PillShape
import io.github.chrisjmendoza.yearal.core.designsystem.theme.YearalTheme
import io.github.chrisjmendoza.yearal.feature.settings.R
import io.github.chrisjmendoza.yearal.core.designsystem.R as DesignSystemR

/**
 * Which "perfect month" illustration [GridIllustration] draws (`docs/design-plan.md` §4.8, ROADMAP
 * wave 3 J3). No bitmap assets: every illustration is drawn from the design tokens on a [Canvas], the
 * same "4 × 7 grid is the design language" constraint the rest of the app follows (design-plan §2), so
 * an illustration always reads as the calendar's own shape rather than decoration bolted onto it.
 */
enum class GridIllustrationVariant {
    /**
     * A 13-block strip standing in for the year's 13 months, with the seventh block — Sol — picked
     * out in [YearalTheme.colors]`.intercalaryContainer`. Illustrates "what the IFC is": one extra
     * month, always in the same place, between June and July.
     */
    THIRTEEN_MONTHS,

    /**
     * The 4 × 7 dot grid of one "perfect month" with a single weekday column ringed in
     * [YearalTheme.colors]`.todayRing`, plus two caption rows giving that column's IFC (nominal) and
     * real (actual) weekday names. Illustrates why the two can differ (calendar-spec §4.1). Requires
     * [GridIllustration]'s `nominalWeekdayLabel` and `actualWeekdayLabel`.
     */
    NOMINAL_VS_ACTUAL,

    /**
     * The 4 × 7 dot grid with a Year Day pill drawn beneath it, outside the grid, in
     * [YearalTheme.colors]`.intercalaryContainer` with the [DesignSystemR.drawable.ic_intercalary]
     * icon. Illustrates that Year Day (and, by the same shape, Leap Day) belongs to no week.
     */
    YEAR_DAY,
}

// Values live in Dimens (docs/design-plan.md §3.1) where a token already exists; the rest are this
// illustration's own, since no other component shares them.
private val IllustrationPadding = Dimens.SpaceM
private val IllustrationContentSpacing = Dimens.SpaceS
private const val ILLUSTRATION_ASPECT_RATIO = 16f / 9f
private const val GRID_COLUMNS = 7
private const val GRID_ROWS = 4

/** Read from the enum rather than hard-coded (CLAUDE.md rule 1's spirit: no calendar fact typed as a literal). */
private val MONTH_STRIP_COUNT = IfcMonth.entries.size
private val SOL_INDEX = IfcMonth.SOL.number - 1
private const val DOT_RADIUS_FRACTION = 0.22f
private const val RING_STROKE_FRACTION = 0.08f
private const val MONTH_BLOCK_GAP_FRACTION = 0.18f
private val MonthBlockCorner = 3.dp
private val PillIconSize = 20.dp

/**
 * A grid-based illustration of one fact about the IFC, drawn purely from the design system's tokens —
 * no bitmap assets (`docs/design-plan.md` §4.8, ROADMAP wave 3 J3). Sized to the caller's width at a
 * 16:9-ish aspect ([ILLUSTRATION_ASPECT_RATIO]) on a [YearalTheme.colors]`.cardContainer` card shaped
 * `MaterialTheme.shapes.large`. Placed atop each first-run intro page and reused as the matching Learn
 * section's header (`docs/ARCHITECTURE.md` §4, the intro/Learn bullets).
 *
 * Colour is never the only signal (CLAUDE.md rule constraints, design-plan §2): the highlighted
 * elements in every variant also differ in shape from the rest — a ring, a capsule/pill in place of a
 * block ([MonthStrip]'s Sol), or a pill outside the grid (Year Day) — and the whole illustration is
 * described to TalkBack by [contentDescription]
 * rather than left for a screen reader to interpret dot by dot ([Modifier.clearAndSetSemantics] hides
 * the decorative canvas and any visible caption text from the accessibility tree in favour of that one
 * description).
 *
 * @param variant which fact to illustrate.
 * @param contentDescription what the illustration shows, read by TalkBack in place of its decorative
 * content; the caller supplies it (like `ExplainerInfoButton`) so this component carries no calendar
 * copy of its own.
 * @param modifier applied to the illustration's card.
 * @param nominalWeekdayLabel the worked example's IFC (nominal) weekday name, shown in the caption row
 * under [GridIllustrationVariant.NOMINAL_VS_ACTUAL]. Required (and only used) for that variant.
 * @param actualWeekdayLabel the same worked example's real (actual) weekday name. Required (and only
 * used) for [GridIllustrationVariant.NOMINAL_VS_ACTUAL].
 * @throws IllegalArgumentException if [variant] is [GridIllustrationVariant.NOMINAL_VS_ACTUAL] and
 * either weekday label is `null`.
 */
@Composable
fun GridIllustration(
    variant: GridIllustrationVariant,
    contentDescription: String,
    modifier: Modifier = Modifier,
    nominalWeekdayLabel: String? = null,
    actualWeekdayLabel: String? = null,
) {
    val colors = YearalTheme.colors
    // Named distinctly from the "contentDescription" property the semantics lambda below sets:
    // inside a lambda with a receiver, an unqualified name resolves to the receiver's own member
    // first, so reusing the parameter's name there would read back an empty description instead of
    // this composable's own parameter (the same reason `IntercalaryBand` calls its local `description`).
    val description = contentDescription
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(ILLUSTRATION_ASPECT_RATIO)
                .clip(MaterialTheme.shapes.large)
                .background(colors.cardContainer)
                .clearAndSetSemantics { this.contentDescription = description }
                .padding(IllustrationPadding),
        verticalArrangement = Arrangement.spacedBy(IllustrationContentSpacing),
    ) {
        when (variant) {
            GridIllustrationVariant.THIRTEEN_MONTHS -> {
                MonthStrip(modifier = Modifier.weight(1f).fillMaxWidth())
            }

            GridIllustrationVariant.NOMINAL_VS_ACTUAL -> {
                require(nominalWeekdayLabel != null && actualWeekdayLabel != null) {
                    "NOMINAL_VS_ACTUAL requires nominalWeekdayLabel and actualWeekdayLabel"
                }
                DotGrid(highlightColumn = true, modifier = Modifier.weight(1f).fillMaxWidth())
                WeekdayCaptionRow(stringResource(R.string.illustration_nominal_weekday_label), nominalWeekdayLabel)
                WeekdayCaptionRow(stringResource(R.string.illustration_actual_weekday_label), actualWeekdayLabel)
            }

            GridIllustrationVariant.YEAR_DAY -> {
                DotGrid(highlightColumn = false, modifier = Modifier.weight(1f).fillMaxWidth())
                YearDayPill()
            }
        }
    }
}

/** The 4 × 7 dot grid shared by [GridIllustrationVariant.NOMINAL_VS_ACTUAL] and `.YEAR_DAY`. */
@Composable
private fun DotGrid(
    highlightColumn: Boolean,
    modifier: Modifier = Modifier,
) {
    val dotColor = YearalTheme.colors.gridCell
    val ringColor = YearalTheme.colors.todayRing
    Canvas(modifier = modifier) {
        val cellWidth = size.width / GRID_COLUMNS
        val cellHeight = size.height / GRID_ROWS
        val dotRadius = minOf(cellWidth, cellHeight) * DOT_RADIUS_FRACTION
        val highlightedColumn = GRID_COLUMNS / 2
        for (row in 0 until GRID_ROWS) {
            for (column in 0 until GRID_COLUMNS) {
                val center = Offset(cellWidth * (column + 0.5f), cellHeight * (row + 0.5f))
                drawCircle(color = dotColor, radius = dotRadius, center = center)
                if (highlightColumn && column == highlightedColumn) {
                    drawCircle(
                        color = ringColor,
                        radius = dotRadius + dotRadius * RING_STROKE_FRACTION * 2,
                        center = center,
                        style = Stroke(width = dotRadius * RING_STROKE_FRACTION * 2),
                    )
                }
            }
        }
    }
}

/**
 * The 13-block month strip for [GridIllustrationVariant.THIRTEEN_MONTHS], Sol picked out at
 * [SOL_INDEX] — by shape as well as colour ([monthStripCornerRadius]): every other block keeps
 * [MonthBlockCorner]'s small rounding, but Sol's is drawn as a full capsule/pill, so a colour-blind or
 * greyscale-display user still sees which block is highlighted (CLAUDE.md rule 3's spirit;
 * `docs/design-plan.md` §2 "colour never alone").
 */
@Composable
private fun MonthStrip(modifier: Modifier = Modifier) {
    val blockColor = YearalTheme.colors.gridCell
    val solColor = YearalTheme.colors.intercalaryContainer
    Canvas(modifier = modifier) {
        val blockWidth = size.width / MONTH_STRIP_COUNT
        val gap = blockWidth * MONTH_BLOCK_GAP_FRACTION
        val defaultCornerRadiusPx = MonthBlockCorner.toPx()
        for (index in 0 until MONTH_STRIP_COUNT) {
            val color = if (index == SOL_INDEX) solColor else blockColor
            drawRoundRect(
                color = color,
                topLeft = Offset(blockWidth * index + gap / 2, 0f),
                size = Size(blockWidth - gap, size.height),
                cornerRadius = monthStripCornerRadius(index, size.height, defaultCornerRadiusPx),
            )
        }
    }
}

/**
 * The corner radius [MonthStrip] draws for one block, given the strip's pixel height and the default
 * (non-Sol) corner radius in pixels. [SOL_INDEX] gets half its own height — a full capsule/pill, one of
 * the shape differences this file's illustrations use elsewhere (the Year Day pill) — instead of
 * [MonthBlockCorner]'s small rounding. Extracted as a pure function, independent of [Canvas], so
 * `MonthStripGeometryTest` can assert the difference without rendering anything.
 */
internal fun monthStripCornerRadius(
    index: Int,
    blockHeightPx: Float,
    defaultCornerRadiusPx: Float,
): CornerRadius =
    if (index == SOL_INDEX) {
        CornerRadius(blockHeightPx / 2f)
    } else {
        CornerRadius(defaultCornerRadiusPx)
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

/** The Year Day pill under [GridIllustrationVariant.YEAR_DAY]'s dot grid, outside the grid entirely. */
@Composable
private fun YearDayPill() {
    val colors = YearalTheme.colors
    Row(
        modifier =
            Modifier
                .clip(PillShape)
                .background(colors.intercalaryContainer)
                .padding(horizontal = Dimens.SpaceM, vertical = Dimens.SpaceXs),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(DesignSystemR.drawable.ic_intercalary),
            contentDescription = null,
            tint = colors.onIntercalaryContainer,
            modifier = Modifier.height(PillIconSize),
        )
        Text(
            text = stringResource(R.string.illustration_year_day_pill_label),
            style = MaterialTheme.typography.labelMedium,
            color = colors.onIntercalaryContainer,
        )
    }
}

// Previews — light, dark and 200% font scale, dynamic colour off for determinism (WORKFLOW §3).
// Preview-only sample weekday names stand in for a worked example's computed values.

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
                nominalWeekdayLabel = "Sunday",
                actualWeekdayLabel = "Thursday",
            )
        }
    }
}
