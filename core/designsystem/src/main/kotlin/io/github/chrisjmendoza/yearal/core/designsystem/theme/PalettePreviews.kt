package io.github.chrisjmendoza.yearal.core.designsystem.theme

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.chrisjmendoza.yearal.core.calendar.IfcMonth
import io.github.chrisjmendoza.yearal.core.calendar.IfcYearMonth
import io.github.chrisjmendoza.yearal.core.designsystem.calendar.MonthGrid
import io.github.chrisjmendoza.yearal.core.domain.settings.ColorPalette
import io.github.chrisjmendoza.yearal.core.domain.settings.WeekdayDisplay
import java.time.LocalDate

/**
 * A small [MonthGrid] rendered once per [ColorPalette] in both light and dark, for the Roborazzi
 * preview scanner (`docs/design-plan.md` §5.2, §6 "record the screenshot baseline"). Every preview
 * passes `dynamicColor = false` so it shows the palette named, not the wallpaper.
 */
private val PREVIEW_MONTH = IfcYearMonth(2026, IfcMonth.SOL)
private val PREVIEW_TODAY: LocalDate = LocalDate.of(2026, 6, 25)
private val PREVIEW_SELECTED: LocalDate = LocalDate.of(2026, 6, 20)
private val PREVIEW_HOLIDAYS = mapOf(LocalDate.of(2026, 7, 4) to "Independence Day")
private val PREVIEW_EVENTS = mapOf(LocalDate.of(2026, 6, 25) to 2)
private const val DARK_UI_MODE = Configuration.UI_MODE_NIGHT_YES

@Composable
private fun PalettePreview(
    palette: ColorPalette,
    darkTheme: Boolean,
) {
    IfcTheme(darkTheme = darkTheme, dynamicColor = false, palette = palette) {
        Surface {
            MonthGrid(
                month = PREVIEW_MONTH,
                today = PREVIEW_TODAY,
                selected = PREVIEW_SELECTED,
                weekdayDisplay = WeekdayDisplay.BOTH,
                eventCounts = PREVIEW_EVENTS,
                holidays = PREVIEW_HOLIDAYS,
                onDayClick = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** [ColorPalette.TEAL], light. */
@Preview(name = "Teal light")
@Composable
internal fun TealLightPalettePreview() {
    PalettePreview(ColorPalette.TEAL, darkTheme = false)
}

/** [ColorPalette.TEAL], dark. */
@Preview(name = "Teal dark", uiMode = DARK_UI_MODE)
@Composable
internal fun TealDarkPalettePreview() {
    PalettePreview(ColorPalette.TEAL, darkTheme = true)
}

/** [ColorPalette.SOL], light. */
@Preview(name = "Sol light")
@Composable
internal fun SolLightPalettePreview() {
    PalettePreview(ColorPalette.SOL, darkTheme = false)
}

/** [ColorPalette.SOL], dark. */
@Preview(name = "Sol dark", uiMode = DARK_UI_MODE)
@Composable
internal fun SolDarkPalettePreview() {
    PalettePreview(ColorPalette.SOL, darkTheme = true)
}

/** [ColorPalette.NIGHT], light. */
@Preview(name = "Night light")
@Composable
internal fun NightLightPalettePreview() {
    PalettePreview(ColorPalette.NIGHT, darkTheme = false)
}

/** [ColorPalette.NIGHT], dark. */
@Preview(name = "Night dark", uiMode = DARK_UI_MODE)
@Composable
internal fun NightDarkPalettePreview() {
    PalettePreview(ColorPalette.NIGHT, darkTheme = true)
}

/** [ColorPalette.MOSS], light. */
@Preview(name = "Moss light")
@Composable
internal fun MossLightPalettePreview() {
    PalettePreview(ColorPalette.MOSS, darkTheme = false)
}

/** [ColorPalette.MOSS], dark. */
@Preview(name = "Moss dark", uiMode = DARK_UI_MODE)
@Composable
internal fun MossDarkPalettePreview() {
    PalettePreview(ColorPalette.MOSS, darkTheme = true)
}

/** [ColorPalette.ROSE], light. */
@Preview(name = "Rose light")
@Composable
internal fun RoseLightPalettePreview() {
    PalettePreview(ColorPalette.ROSE, darkTheme = false)
}

/** [ColorPalette.ROSE], dark. */
@Preview(name = "Rose dark", uiMode = DARK_UI_MODE)
@Composable
internal fun RoseDarkPalettePreview() {
    PalettePreview(ColorPalette.ROSE, darkTheme = true)
}

/** [ColorPalette.INK], light. */
@Preview(name = "Ink light")
@Composable
internal fun InkLightPalettePreview() {
    PalettePreview(ColorPalette.INK, darkTheme = false)
}

/** [ColorPalette.INK], dark. */
@Preview(name = "Ink dark", uiMode = DARK_UI_MODE)
@Composable
internal fun InkDarkPalettePreview() {
    PalettePreview(ColorPalette.INK, darkTheme = true)
}
