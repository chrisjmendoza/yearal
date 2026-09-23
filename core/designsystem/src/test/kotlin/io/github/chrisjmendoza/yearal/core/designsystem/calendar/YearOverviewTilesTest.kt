package io.github.chrisjmendoza.yearal.core.designsystem.calendar

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.chrisjmendoza.yearal.core.calendar.IfcDate
import io.github.chrisjmendoza.yearal.core.calendar.IfcMonth
import io.github.chrisjmendoza.yearal.core.calendar.IfcYearMonth
import io.github.chrisjmendoza.yearal.core.designsystem.theme.IfcTheme
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

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
}
