package io.github.chrisjmendoza.yearal.feature.settings.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.chrisjmendoza.yearal.core.designsystem.theme.Dimens
import io.github.chrisjmendoza.yearal.core.designsystem.theme.IfcTheme
import io.github.chrisjmendoza.yearal.core.designsystem.theme.PillShape
import io.github.chrisjmendoza.yearal.core.designsystem.theme.YearalColors
import io.github.chrisjmendoza.yearal.core.designsystem.theme.YearalTheme
import io.github.chrisjmendoza.yearal.core.domain.settings.ColorPalette
import io.github.chrisjmendoza.yearal.core.domain.settings.ColorSource
import io.github.chrisjmendoza.yearal.core.domain.settings.ThemeMode
import io.github.chrisjmendoza.yearal.core.domain.settings.UserSettings
import io.github.chrisjmendoza.yearal.feature.settings.R
import io.github.chrisjmendoza.yearal.core.designsystem.R as DesignSystemR

// Values live in Dimens (docs/design-plan.md §3.1) where a token already exists; the strip's own cell
// size and mark sizes are smaller than the real month grid's, so they get their own constants.
private val PreviewCellSize = 28.dp
private val PreviewMarkSize = 6.dp
private val PreviewCellSpacing = Dimens.SpaceXs
private val PreviewPillIconSize = 16.dp

/** 1-based day the strip marks as "today" (a ring, bold). Any day works; the middle reads best. */
private const val PREVIEW_TODAY_DAY = 4

/** 1-based day the strip marks with a holiday diamond. */
private const val PREVIEW_HOLIDAY_DAY = 6

/** 1-based day the strip marks with an event dot. */
private const val PREVIEW_EVENT_DAY = 2

/**
 * A live preview of the currently selected appearance settings (`docs/design-plan.md` §4.8, ROADMAP
 * wave 3 J3): a miniature 7-day row plus an intercalary pill, so a user sees the palette (or Material
 * You) and theme mode they are choosing before leaving Settings, exactly like the six palette swatches
 * above it re-theme as they are tapped.
 *
 * Wraps its content in its own nested [IfcTheme] resolved from [settings] — `darkTheme` from
 * [UserSettings.themeMode] (falling back to [isSystemInDarkTheme] for [ThemeMode.SYSTEM]), `dynamicColor`
 * from [UserSettings.colorSource], and [UserSettings.palette]/[UserSettings.pureBlack] passed straight
 * through — rather than reading the ambient [YearalTheme], so the strip always shows *this* combination
 * of settings even while the rest of the screen (and the preview swatches, which read their own palette
 * argument directly) still reflects whatever theme the host `Activity` resolved when the screen opened.
 *
 * @param settings the settings to preview; every field but [UserSettings.weekdayDisplay] and the widget
 * fields affects what is drawn.
 * @param modifier applied to the strip's root.
 */
@Composable
fun PalettePreviewStrip(
    settings: UserSettings,
    modifier: Modifier = Modifier,
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme =
        when (settings.themeMode) {
            ThemeMode.SYSTEM -> systemDark
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    IfcTheme(
        darkTheme = darkTheme,
        dynamicColor = settings.colorSource == ColorSource.DYNAMIC,
        palette = settings.palette,
        pureBlack = settings.pureBlack,
    ) {
        PalettePreviewStripContent(
            paletteLabel = stringResource(settings.palette.labelRes()),
            modifier = modifier,
        )
    }
}

@Composable
private fun PalettePreviewStripContent(
    paletteLabel: String,
    modifier: Modifier,
) {
    val colors = YearalTheme.colors
    val description = stringResource(R.string.settings_palette_preview_description, paletteLabel)
    Column(
        modifier =
            modifier
                .clearAndSetSemantics { contentDescription = description }
                .padding(vertical = Dimens.SpaceS),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(PreviewCellSpacing)) {
            for (day in 1..DAYS_IN_PREVIEW_WEEK) {
                PreviewDayCell(
                    day = day,
                    isToday = day == PREVIEW_TODAY_DAY,
                    hasHoliday = day == PREVIEW_HOLIDAY_DAY,
                    hasEvent = day == PREVIEW_EVENT_DAY,
                    colors = colors,
                )
            }
        }
        PreviewIntercalaryPill(colors)
    }
}

/** One 28dp day square: the number, and — on [isToday]/[hasHoliday]/[hasEvent] — a shape twin of each mark. */
@Composable
private fun PreviewDayCell(
    day: Int,
    isToday: Boolean,
    hasHoliday: Boolean,
    hasEvent: Boolean,
    colors: YearalColors,
) {
    Column(
        modifier =
            Modifier
                .size(PreviewCellSize)
                .clip(MaterialTheme.shapes.small)
                .background(colors.gridCell)
                .then(
                    if (isToday) {
                        Modifier.border(Dimens.TodayRingWidth, colors.todayRing, MaterialTheme.shapes.small)
                    } else {
                        Modifier
                    },
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isToday) FontWeight.Bold else null,
            color = colors.onCard,
        )
        when {
            hasHoliday -> {
                Box(
                    modifier =
                        Modifier
                            .padding(top = 1.dp)
                            .size(PreviewMarkSize)
                            .rotate(45f)
                            .background(colors.holidayMark),
                )
            }

            hasEvent -> {
                Box(
                    modifier =
                        Modifier
                            .padding(top = 1.dp)
                            .size(PreviewMarkSize)
                            .clip(CircleShape)
                            .background(colors.eventMark),
                )
            }
        }
    }
}

/** The strip's intercalary pill, the same [PillShape] and icon as the Year Day/Leap Day band elsewhere. */
@Composable
private fun PreviewIntercalaryPill(colors: YearalColors) {
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
            modifier = Modifier.size(PreviewPillIconSize),
        )
        Text(
            text = stringResource(R.string.settings_palette_preview_intercalary_label),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onIntercalaryContainer,
        )
    }
}

private const val DAYS_IN_PREVIEW_WEEK = 7

// Previews — light, dark and 200% font scale, dynamic colour off for determinism (WORKFLOW §3).

@Preview(name = "Palette preview strip", showBackground = true)
@Composable
private fun PalettePreviewStripLightPreview() {
    IfcTheme(dynamicColor = false) {
        PalettePreviewStrip(settings = UserSettings.DEFAULT.copy(palette = ColorPalette.SOL))
    }
}

@Preview(name = "Palette preview strip — dark", showBackground = true)
@Composable
private fun PalettePreviewStripDarkPreview() {
    IfcTheme(darkTheme = true, dynamicColor = false) {
        PalettePreviewStrip(
            settings = UserSettings.DEFAULT.copy(palette = ColorPalette.NIGHT, themeMode = ThemeMode.DARK),
        )
    }
}

@Preview(name = "Palette preview strip — font 2.0", showBackground = true, fontScale = 2f)
@Composable
private fun PalettePreviewStripLargeFontPreview() {
    IfcTheme(dynamicColor = false) {
        PalettePreviewStrip(settings = UserSettings.DEFAULT.copy(palette = ColorPalette.ROSE))
    }
}
