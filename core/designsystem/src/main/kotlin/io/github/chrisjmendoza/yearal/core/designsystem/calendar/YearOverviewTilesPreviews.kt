package io.github.chrisjmendoza.yearal.core.designsystem.calendar

import android.content.res.Configuration
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.chrisjmendoza.yearal.core.calendar.IfcDate
import io.github.chrisjmendoza.yearal.core.calendar.IfcMonth
import io.github.chrisjmendoza.yearal.core.calendar.IfcYearMonth
import io.github.chrisjmendoza.yearal.core.designsystem.theme.IfcTheme
import java.time.LocalDate

// The card fill, restored intercalary fill and 8dp marks (design-plan §4.3) all depend on colour, so
// this matrix captures light/dark at 1.0 and 2.0 font scale (design-plan §6 "font-scale previews").
// Dynamic colour is off so the brand palette, not the wallpaper, is what is captured.

private const val LARGE_FONT = 2f
private const val DARK = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL

/** The Year overview tile matrix (docs/design-plan.md §4.3); applied to the preview below. */
@Preview(name = "light 1.0")
@Preview(name = "light 2.0", fontScale = LARGE_FONT)
@Preview(name = "dark 1.0", uiMode = DARK)
@Preview(name = "dark 2.0", uiMode = DARK, fontScale = LARGE_FONT)
internal annotation class YearOverviewTileMatrix

/**
 * A plain October tile, a June tile with the Leap Day indicator (marked with an event), and the Year
 * Day tile, side by side: today on October 12, 2026, a holiday and an event in June, an event on
 * Year Day.
 */
@YearOverviewTileMatrix
@Composable
internal fun YearOverviewTilesPreview() {
    IfcTheme(dynamicColor = false) {
        Surface {
            Row(modifier = Modifier.padding(16.dp)) {
                YearMiniMonthTile(
                    month = IfcYearMonth(2026, IfcMonth.OCTOBER),
                    today = LocalDate.of(2026, 10, 12),
                    eventDates = setOf(LocalDate.of(2026, 10, 12)),
                    holidays = setOf(LocalDate.of(2026, 10, 19)),
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
                YearMiniMonthTile(
                    month = IfcYearMonth(2028, IfcMonth.JUNE),
                    today = null,
                    eventDates = setOf(LocalDate.of(2028, 6, 17)),
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
                YearDayTile(
                    yearDay = IfcDate.YearDay(2026),
                    isToday = false,
                    hasEvent = true,
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
