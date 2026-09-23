package io.github.chrisjmendoza.yearal.feature.calendar.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import io.github.chrisjmendoza.yearal.core.designsystem.theme.YearalTheme

/** Diameter of a holiday row's leading diamond (`docs/design-plan.md` §4.1: "an 8 dp rotated square"). */
private val HolidayMarkSize = 8.dp

/**
 * The leading mark of a holiday row on Today, the Month summary and Day detail (`docs/design-plan.md`
 * §4.1, §4.2, §4.4): an 8 dp square rotated 45°, the same shape
 * [io.github.chrisjmendoza.yearal.core.designsystem.calendar.DayMarks] draws in the month grid's own
 * cells, so a holiday reads the same way everywhere it appears (CLAUDE.md rule 3: colour is never the
 * only signal — the diamond shape carries the meaning too).
 *
 * @param modifier applied to the mark itself.
 */
@Composable
fun HolidayDiamondMark(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .size(HolidayMarkSize)
                .rotate(degrees = 45f)
                .background(YearalTheme.colors.holidayMark),
    )
}
