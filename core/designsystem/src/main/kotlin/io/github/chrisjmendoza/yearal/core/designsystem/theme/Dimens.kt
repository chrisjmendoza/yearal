package io.github.chrisjmendoza.yearal.core.designsystem.theme

import androidx.compose.ui.unit.dp

/**
 * The app's spacing and sizing scale (`docs/design-plan.md` §3.1 "Spacing"). One place to tune every
 * gap, padding and mark size in the calendar UI instead of the private `dp` literals each composable
 * used to declare for itself. Values are unchanged from what they replace, so adopting [Dimens] moves
 * no pixel: [DayCellGap], [DayCellPadding] and [TodayRingWidth] came from `DayCell.kt`;
 * [MarkSize], [MarkSpacing] and [MarkRowHeight] from `DayMarks.kt`; [DayCellMinSize] from `DayCell.kt`
 * (docs/ARCHITECTURE.md §4 "Accessibility", the 48dp touch target floor).
 */
public object Dimens {
    /** Extra-small spacing: 4dp. */
    public val SpaceXs = 4.dp

    /** Small spacing: 8dp. */
    public val SpaceS = 8.dp

    /** Medium spacing: 12dp. */
    public val SpaceM = 12.dp

    /** Large spacing: 16dp. */
    public val SpaceL = 16.dp

    /** Extra-large spacing: 24dp. */
    public val SpaceXl = 24.dp

    /** Double extra-large spacing: 32dp. */
    public val SpaceXxl = 32.dp

    /**
     * The gap between adjacent day cells in the month grid: 1dp, not more, since seven columns at
     * 360dp give 51dp each (docs/ARCHITECTURE.md §4) and the touch target inside the gap must stay at
     * [DayCellMinSize].
     */
    public val CellGap = 1.dp

    /** The inner padding of a day cell, between its border and the day number/marks column. */
    public val CellPadding = 4.dp

    /** Width of the ring drawn around today's cell or band. */
    public val TodayRingWidth = 2.dp

    /** Diameter of a holiday diamond or event dot under a day number. */
    public val MarkSize = 6.dp

    /** Horizontal gap between adjacent marks in [MarkSize]'s row. */
    public val MarkSpacing = 3.dp

    /** Height reserved for the marks row, kept even when a day has no marks. */
    public val MarkRowHeight = 8.dp

    /** Minimum touch target of a day cell (docs/ARCHITECTURE.md §4 "Accessibility"). */
    public val DayCellMinSize = 48.dp
}
