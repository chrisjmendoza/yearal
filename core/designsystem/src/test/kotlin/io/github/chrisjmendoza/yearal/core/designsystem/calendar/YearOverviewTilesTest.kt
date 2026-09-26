package io.github.chrisjmendoza.yearal.core.designsystem.calendar

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.chrisjmendoza.yearal.core.calendar.IfcDate
import io.github.chrisjmendoza.yearal.core.calendar.IfcMonth
import io.github.chrisjmendoza.yearal.core.calendar.IfcYearMonth
import io.github.chrisjmendoza.yearal.core.designsystem.theme.IfcTheme
import io.github.chrisjmendoza.yearal.core.designsystem.theme.LightPrimary
import io.github.chrisjmendoza.yearal.core.designsystem.theme.LightTertiary
import io.github.chrisjmendoza.yearal.core.designsystem.theme.YearalTheme
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import kotlin.math.abs

/**
 * [YearMiniMonthTile] under Robolectric, written from `docs/calendar-spec.md` §2.2, §7.2 and
 * docs/ARCHITECTURE.md §4 ("Intercalary days in a 7-column grid", "Accessibility"): the merged
 * spoken description (month, Leap Day note, event count, today), the tile's own test tag, the click
 * callback, and that the whole tile — Leap Day's inline indicator included — stays a single
 * clickable node (FEATURES C6's TalkBack requirement).
 */
@RunWith(AndroidJUnit4::class)
// Native graphics: the tile's Canvas grid draws real shapes, like MonthGridTest's band measurement.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w200dp-h300dp")
class YearOverviewTilesTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(
        month: IfcYearMonth,
        today: LocalDate? = null,
        eventDates: Set<LocalDate> = emptySet(),
        holidays: Set<LocalDate> = emptySet(),
        onClick: () -> Unit = {},
    ) {
        compose.setContent {
            IfcTheme(dynamicColor = false) {
                YearMiniMonthTile(
                    month = month,
                    today = today,
                    eventDates = eventDates,
                    onClick = onClick,
                    holidays = holidays,
                )
            }
        }
    }

    private fun tile() = compose.onNodeWithTag(YearOverviewTestTags.MINI_MONTH_TILE_PREFIX + IfcMonth.OCTOBER.number)

    private val yearDay2026 = IfcDate.YearDay(2026)
    private val yearDayDescription = "Year Day, no IFC weekday. Gregorian Thursday, December 31, 2026."

    @Test
    fun `a plain month tile only speaks its title`() {
        show(IfcYearMonth(2026, IfcMonth.OCTOBER))

        tile().assert(hasContentDescription("October 2026"))
    }

    @Test
    fun `June in a common year does not mention Leap Day`() {
        val tag = YearOverviewTestTags.MINI_MONTH_TILE_PREFIX + IfcMonth.JUNE.number
        compose.setContent {
            IfcTheme(dynamicColor = false) {
                YearMiniMonthTile(
                    month = IfcYearMonth(2027, IfcMonth.JUNE),
                    today = null,
                    eventDates = emptySet(),
                    onClick = {},
                )
            }
        }

        compose.onNodeWithTag(tag).assert(hasContentDescription("June 2027"))
    }

    @Test
    fun `June in a leap year mentions Leap Day in the merged description`() {
        val tag = YearOverviewTestTags.MINI_MONTH_TILE_PREFIX + IfcMonth.JUNE.number
        compose.setContent {
            IfcTheme(dynamicColor = false) {
                YearMiniMonthTile(
                    month = IfcYearMonth(2028, IfcMonth.JUNE),
                    today = null,
                    eventDates = emptySet(),
                    onClick = {},
                )
            }
        }

        compose.onNodeWithTag(tag).assert(hasContentDescription("June 2028 Includes Leap Day."))
    }

    @Test
    fun `an event day is counted in the description`() {
        // IFC October 5, 2026 is Gregorian October 12, 2026.
        show(IfcYearMonth(2026, IfcMonth.OCTOBER), eventDates = setOf(LocalDate.of(2026, 10, 12)))

        tile().assert(hasContentDescription("October 2026 1 day with events."))
    }

    @Test
    fun `two event days are counted with the plural form`() {
        show(
            IfcYearMonth(2026, IfcMonth.OCTOBER),
            eventDates = setOf(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 13)),
        )

        tile().assert(hasContentDescription("October 2026 2 days with events."))
    }

    @Test
    fun `today inside the tile is named by its IFC day`() {
        // Gregorian October 12, 2026 is IFC October 5, 2026.
        show(IfcYearMonth(2026, IfcMonth.OCTOBER), today = LocalDate.of(2026, 10, 12))

        tile().assert(hasContentDescription("October 2026 Today is October 5."))
    }

    @Test
    fun `today elsewhere marks nothing on this tile`() {
        show(IfcYearMonth(2026, IfcMonth.OCTOBER), today = LocalDate.of(2026, 9, 17))

        tile().assert(hasContentDescription("October 2026"))
    }

    @Test
    fun `Leap Day as today is named and mentioned once`() {
        val tag = YearOverviewTestTags.MINI_MONTH_TILE_PREFIX + IfcMonth.JUNE.number
        compose.setContent {
            IfcTheme(dynamicColor = false) {
                YearMiniMonthTile(
                    month = IfcYearMonth(2028, IfcMonth.JUNE),
                    today = LocalDate.of(2028, 6, 17),
                    eventDates = emptySet(),
                    onClick = {},
                )
            }
        }

        compose.onNodeWithTag(tag).assert(hasContentDescription("June 2028 Includes Leap Day. Today is Leap Day."))
    }

    @Test
    fun `tapping the tile invokes onClick`() {
        var clicks = 0
        show(IfcYearMonth(2026, IfcMonth.OCTOBER), onClick = { clicks++ })

        tile().performClick()

        clicks shouldBe 1
    }

    // Finding #4: mergeDescendants = true must fold the month name, Leap Day note and marks into the
    // tile's one node, or TalkBack speaks the merged description and then re-announces the inner
    // Texts as separate stops.

    @Test
    fun `a mini-month tile's merged node has no separately reachable children`() {
        val tag = YearOverviewTestTags.MINI_MONTH_TILE_PREFIX + IfcMonth.JUNE.number
        // June, a leap year: the Leap Day indicator adds an extra Text and Icon to fold in.
        compose.setContent {
            IfcTheme(dynamicColor = false) {
                YearMiniMonthTile(
                    month = IfcYearMonth(2028, IfcMonth.JUNE),
                    today = null,
                    eventDates = emptySet(),
                    onClick = {},
                )
            }
        }

        compose.onNodeWithTag(tag).onChildren().assertCountEquals(0)
    }

    @Test
    fun `a leap year tile with the Leap Day indicator is still one clickable node`() {
        compose.setContent {
            IfcTheme(dynamicColor = false) {
                YearMiniMonthTile(
                    month = IfcYearMonth(2028, IfcMonth.JUNE),
                    today = null,
                    eventDates = emptySet(),
                    onClick = {},
                )
            }
        }

        compose.onAllNodes(hasClickAction()).assertCountEquals(1)
    }

    // Holidays (design-plan §4.3: the mini grid marks holidays too, since the data is already there).

    @Test
    fun `a holiday day is counted in the description`() {
        // IFC October 5, 2026 is Gregorian October 12, 2026.
        show(IfcYearMonth(2026, IfcMonth.OCTOBER), holidays = setOf(LocalDate.of(2026, 10, 12)))

        tile().assert(hasContentDescription("October 2026 1 day with a holiday."))
    }

    @Test
    fun `two holiday days are counted with the plural form`() {
        show(
            IfcYearMonth(2026, IfcMonth.OCTOBER),
            holidays = setOf(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 13)),
        )

        tile().assert(hasContentDescription("October 2026 2 days with holidays."))
    }

    @Test
    fun `event and holiday counts both appear, events before holidays`() {
        show(
            IfcYearMonth(2026, IfcMonth.OCTOBER),
            eventDates = setOf(LocalDate.of(2026, 10, 12)),
            holidays = setOf(LocalDate.of(2026, 10, 13)),
        )

        tile().assert(hasContentDescription("October 2026 1 day with events. 1 day with a holiday."))
    }

    // Fix design-pass 9: a day marked with both a holiday and an event used to draw only the diamond,
    // silently dropping the event mark. The merged description above can't tell the two cases apart —
    // its event/holiday counts are computed independently of what the Canvas actually draws, so a day
    // carrying both would already speak correctly even with the bug. Only the rendered pixels can prove
    // the corner event dot is actually there, so this renders the tile at a higher density (xxhdpi,
    // still the same w200dp-h300dp layout) for a reliable colour sample.

    @Test
    @Config(qualifiers = "w200dp-h300dp-xxhdpi")
    fun `a day with both a holiday and an event draws both marks, not just the diamond`() {
        // IFC October 5, 2026 is Gregorian October 12, 2026. October (unlike June) has no Leap Day
        // indicator competing for the tertiary/holiday colour.
        val markedDate = LocalDate.of(2026, 10, 12)
        show(IfcYearMonth(2026, IfcMonth.OCTOBER), eventDates = setOf(markedDate), holidays = setOf(markedDate))

        val pixels = tile().captureToImage().toPixelMap()

        pixels.containsColorNear(LightTertiary) shouldBe true // the holiday diamond, centred in the cell
        pixels.containsColorNear(LightPrimary) shouldBe true // the smaller event dot in the cell's corner
    }

    // Fix R11: the 28 squares were drawn in `gridCell`, the same Material role as the card they sit
    // on, so every plain square was invisible; and no square carried its day number, so the tile read
    // as loose marks floating on a blank card. Only pixels can prove either fix, so both tests below
    // capture the grid Canvas itself (its own test tag, unmerged tree) at xxhdpi.

    @Test
    @Config(qualifiers = "w200dp-h300dp-xxhdpi")
    fun `a plain square is filled in the mini-grid colour, not the card colour`() {
        var squareColor = Color.Unspecified
        var cardColor = Color.Unspecified
        compose.setContent {
            IfcTheme(dynamicColor = false) {
                squareColor = YearalTheme.colors.miniGridCell
                cardColor = YearalTheme.colors.cardContainer
                YearMiniMonthTile(
                    month = IfcYearMonth(2026, IfcMonth.OCTOBER),
                    today = null,
                    eventDates = emptySet(),
                    onClick = {},
                )
            }
        }

        val pixels = grid().captureToImage().toPixelMap()
        // Inside day 1's square, past its inset and corner radius but left of its centred number.
        val squareSide = minOf(pixels.width / GRID_COLUMNS, pixels.height / GRID_ROWS)
        val probe = (squareSide * PLAIN_FILL_PROBE_FRACTION).toInt()
        val pixel = pixels[probe, probe]

        // "Closer to" rather than isNear/not-isNear: in the light schemes the square and card tiers are
        // only a few percent apart per channel, inside the tolerance that antialiased marks need, so a
        // not-near check on the card colour cannot pass even when the square is drawn correctly.
        // ColorSchemeContrastTest already pins that the two tokens differ, so strict "closer" is decisive.
        pixel.isNear(squareColor) shouldBe true
        (pixel.distanceTo(squareColor) < pixel.distanceTo(cardColor)) shouldBe true
    }

    @Test
    @Config(qualifiers = "w200dp-h300dp-xxhdpi")
    fun `the mini grid draws its day numbers`() {
        var numberColor = Color.Unspecified
        compose.setContent {
            IfcTheme(dynamicColor = false) {
                numberColor = YearalTheme.colors.onCard
                YearMiniMonthTile(
                    month = IfcYearMonth(2026, IfcMonth.OCTOBER),
                    today = null,
                    eventDates = emptySet(),
                    onClick = {},
                )
            }
        }

        // The Canvas alone: the month title above it is the only other onCard text in the tile, and
        // it is outside this capture, so any onCard pixel here is a day number.
        grid().captureToImage().toPixelMap().containsColorNear(numberColor) shouldBe true
    }

    private fun grid() = compose.onNodeWithTag(YearOverviewTestTags.MINI_GRID, useUnmergedTree = true)

    /** Whether any pixel is within [tolerance] of [target] on every RGB channel (0f..1f each). */
    private fun PixelMap.containsColorNear(
        target: Color,
        tolerance: Float = 0.08f,
    ): Boolean {
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (this[x, y].isNear(target, tolerance)) {
                    return true
                }
            }
        }
        return false
    }

    /** The sum of absolute RGB channel differences from [target] (0f..3f). */
    private fun Color.distanceTo(target: Color): Float =
        abs(red - target.red) + abs(green - target.green) + abs(blue - target.blue)

    /** Whether this colour is within [tolerance] of [target] on every RGB channel (0f..1f each). */
    private fun Color.isNear(
        target: Color,
        tolerance: Float = 0.08f,
    ): Boolean =
        abs(red - target.red) < tolerance &&
            abs(green - target.green) < tolerance &&
            abs(blue - target.blue) < tolerance

    private companion object {
        /** Where inside a plain square to sample its fill: past the inset and corner, short of the number. */
        const val PLAIN_FILL_PROBE_FRACTION = 0.2f

        /** Material 3's minimum touch target (docs/ARCHITECTURE.md §4 "Accessibility"). */
        val MIN_TOUCH_TARGET = 48.dp

        /**
         * The narrowest a tile can be in production: the Year screen's
         * `LazyVerticalGrid(GridCells.Adaptive(160.dp))` (feature/calendar's YearScreen.kt,
         * `TileMinWidth`) guarantees every column at least this wide. Duplicated here as a literal
         * because this module cannot depend on feature/calendar — keep it in sync if that constant
         * ever changes.
         */
        val YEAR_SCREEN_NARROWEST_TILE_WIDTH = 160.dp
    }

    // YearDayTile (docs/design-plan.md §4.3/§8 decision 4: the intercalary fill returned).

    @Test
    fun `the Year Day tile speaks its description`() {
        compose.setContent {
            IfcTheme(dynamicColor = false) {
                YearDayTile(yearDay = yearDay2026, isToday = false, hasEvent = false, onClick = {})
            }
        }

        compose.onNodeWithContentDescription(yearDayDescription).assertIsDisplayed()
    }

    @Test
    fun `the Year Day tile's merged node has no separately reachable children`() {
        compose.setContent {
            IfcTheme(dynamicColor = false) {
                YearDayTile(yearDay = yearDay2026, isToday = false, hasEvent = false, onClick = {})
            }
        }

        compose.onNodeWithContentDescription(yearDayDescription).onChildren().assertCountEquals(0)
    }

    @Test
    fun `tapping the Year Day tile invokes onClick`() {
        var clicks = 0
        compose.setContent {
            IfcTheme(dynamicColor = false) {
                YearDayTile(yearDay = yearDay2026, isToday = false, hasEvent = false, onClick = { clicks++ })
            }
        }

        compose.onNodeWithContentDescription(yearDayDescription).performClick()

        clicks shouldBe 1
    }

    @Test
    fun `the Year Day tile still renders beside a mini-month tile in a two-column layout`() {
        // The Year screen lays Year Day into the 14th slot of a two-column adaptive grid on a compact
        // phone (docs/ARCHITECTURE.md §4 "Screen behaviors"); this pins that both tiles still lay out
        // and speak correctly sharing this file's w200dp Robolectric width.
        compose.setContent {
            IfcTheme(dynamicColor = false) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    YearMiniMonthTile(
                        month = IfcYearMonth(2026, IfcMonth.OCTOBER),
                        today = null,
                        eventDates = emptySet(),
                        onClick = {},
                        modifier = Modifier.weight(1f),
                    )
                    YearDayTile(
                        yearDay = yearDay2026,
                        isToday = false,
                        hasEvent = false,
                        onClick = {},
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        tile().assertIsDisplayed()
        compose.onNodeWithContentDescription(yearDayDescription).assertIsDisplayed()
    }

    // Finding #19: neither tile pins its own minimum size the way DayCell (defaultMinSize) and
    // IntercalaryBand (heightIn) do. In production that is covered by the caller instead: the Year
    // screen's `LazyVerticalGrid(GridCells.Adaptive(160.dp))` (feature/calendar's YearScreen.kt,
    // `TileMinWidth`) guarantees every tile at least 160dp wide, and each tile's own title, grid/rows
    // and padding put its natural height well past 48dp at that width. This pins both tiles at that
    // narrowest production width in both directions; no change to either composable was needed. (This
    // module cannot depend on feature/calendar to reference TileMinWidth directly, so the 160dp floor
    // is duplicated here as a literal — keep it in sync if that constant ever changes.)

    @Test
    fun `a mini-month tile clears 48dp at the Year screen's narrowest tile width`() {
        compose.setContent {
            IfcTheme(dynamicColor = false) {
                YearMiniMonthTile(
                    // June in a leap year is the tallest tile: it adds the Leap Day indicator row.
                    month = IfcYearMonth(2028, IfcMonth.JUNE),
                    today = null,
                    eventDates = emptySet(),
                    onClick = {},
                    modifier = Modifier.width(YEAR_SCREEN_NARROWEST_TILE_WIDTH),
                )
            }
        }

        val node = compose.onNodeWithTag(YearOverviewTestTags.MINI_MONTH_TILE_PREFIX + IfcMonth.JUNE.number)
        node.assertHeightIsAtLeast(MIN_TOUCH_TARGET)
        node.assertWidthIsAtLeast(MIN_TOUCH_TARGET)
    }

    @Test
    fun `the Year Day tile clears 48dp at the Year screen's narrowest tile width`() {
        compose.setContent {
            IfcTheme(dynamicColor = false) {
                YearDayTile(
                    yearDay = yearDay2026,
                    isToday = false,
                    hasEvent = false,
                    onClick = {},
                    modifier = Modifier.width(YEAR_SCREEN_NARROWEST_TILE_WIDTH),
                )
            }
        }

        val node = compose.onNodeWithContentDescription(yearDayDescription)
        node.assertHeightIsAtLeast(MIN_TOUCH_TARGET)
        node.assertWidthIsAtLeast(MIN_TOUCH_TARGET)
    }
}
