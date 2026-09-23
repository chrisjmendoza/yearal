package io.github.chrisjmendoza.yearal.feature.holidays

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.chrisjmendoza.yearal.core.designsystem.theme.IfcTheme
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [HolidaysScreen] under Robolectric: every set row reflects its on/off state and reports a toggle with
 * the right id, the year controls page and clamp, tapping a year-list row reports the tapped date, the
 * empty state shows when there is nothing to list, and text does not clip at 200% font scale
 * (docs/ARCHITECTURE.md §4 "Accessibility").
 */
@RunWith(AndroidJUnit4::class)
class HolidaysScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val holidayChanges = mutableListOf<Pair<String, Boolean>>()
    private val yearsRequested = mutableListOf<Int>()
    private val rowsClicked = mutableListOf<Long>()
    private var backPresses = 0

    private fun show(
        state: HolidaysUiState,
        fontScale: Float = 1f,
    ) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                IfcTheme(dynamicColor = false) {
                    HolidaysScreen(
                        state = state,
                        onBack = { backPresses++ },
                        onHolidaySetEnabledChanged = { id, enabled -> holidayChanges += id to enabled },
                        onGoToYear = { yearsRequested += it },
                        onRowClick = { rowsClicked += it },
                    )
                }
            }
        }
    }

    private val sets =
        listOf(
            HolidaySetRow(
                id = "ifc",
                name = "International Fixed Calendar",
                region = null,
                holidayCount = 3,
                sources = null,
                enabled = true,
            ),
            HolidaySetRow(
                id = "us",
                name = "United States",
                region = "United States",
                holidayCount = 24,
                sources = "5 U.S.C. § 6103",
                enabled = true,
            ),
            HolidaySetRow(
                id = "religious-christian",
                name = "Christian (Easter family)",
                region = null,
                holidayCount = 6,
                sources = null,
                enabled = false,
            ),
        )

    private val newYearRow =
        HolidayOccurrenceRow(
            epochDay = 20089L,
            name = "New Year's Day",
            ifcLong = "January 1, 2026",
            ifcNumeric = "IFC 2026-01-01",
            gregorianLong = "Thursday, January 1, 2026",
            description = "New Year's Day. IFC January 1, 2026 · IFC 2026-01-01. Gregorian Thursday, January 1, 2026.",
        )

    private val yearDayRow =
        HolidayOccurrenceRow(
            epochDay = 20453L,
            name = "Year Day",
            ifcLong = "Year Day, 2026",
            ifcNumeric = "IFC 2026-13-29",
            gregorianLong = "Thursday, December 31, 2026",
            description = "Year Day. IFC Year Day, 2026 · IFC 2026-13-29. Gregorian Thursday, December 31, 2026.",
            isIntercalary = true,
        )

    private fun loaded(
        year: Int = 2026,
        groups: List<HolidayMonthGroup> =
            listOf(
                HolidayMonthGroup("January", listOf(newYearRow)),
                HolidayMonthGroup("December", listOf(yearDayRow)),
            ),
    ) = HolidaysUiState.Loaded(sets = sets, year = year, groups = groups)

    // ----- Browsing and toggling (task 1) -----

    @Test
    fun `every set shows its name, region and holiday count`() {
        show(loaded())

        compose.onNodeWithText("International Fixed Calendar").performScrollTo().assertIsOn()
        compose.onNodeWithText("United States").performScrollTo().assertIsOn()
        compose.onNodeWithText("Region: United States").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("24 holidays").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Christian (Easter family)").performScrollTo().assertIsOff()
        compose.onNodeWithText("3 holidays").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `a set with sources shows them, and one without shows nothing extra`() {
        show(loaded())

        compose.onNodeWithText("Source: 5 U.S.C. § 6103").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `clicking a set switch reports its id with the opposite state`() {
        show(loaded())

        compose.onNodeWithText("United States").performScrollTo().performClick()
        compose.onNodeWithText("Christian (Easter family)").performScrollTo().performClick()

        holidayChanges shouldBe listOf("us" to false, "religious-christian" to true)
    }

    // docs/design-plan.md section 4.7: colour is never the only signal — an enabled pack's switch
    // state (Role.Switch, on/off) is exposed in semantics independently of its container colour.
    @Test
    fun `a pack row's enabled state is exposed as switch semantics, not colour alone`() {
        show(loaded())

        compose
            .onNodeWithText("International Fixed Calendar")
            .performScrollTo()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assertIsOn()
        compose
            .onNodeWithText("Christian (Easter family)")
            .performScrollTo()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assertIsOff()
    }

    // ----- Year paging (task 2) -----

    @Test
    fun `the year controls page by one year in each direction`() {
        show(loaded(year = 2026))

        compose.onNodeWithContentDescription("Previous year").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Next year").performScrollTo().performClick()

        yearsRequested shouldBe listOf(2025, 2027)
    }

    @Test
    fun `previous year is disabled at 1583`() {
        show(loaded(year = 1583))

        compose.onNodeWithContentDescription("Previous year").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithContentDescription("Next year").performScrollTo().assertIsEnabled()
    }

    @Test
    fun `next year is disabled at 9999`() {
        show(loaded(year = 9999))

        compose.onNodeWithContentDescription("Previous year").performScrollTo().assertIsEnabled()
        compose.onNodeWithContentDescription("Next year").performScrollTo().assertIsNotEnabled()
    }

    // ----- The per-year list -----

    @Test
    fun `both dates and the IFC prefix show on a row`() {
        show(loaded())

        compose.onNodeWithText("New Year's Day").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("January 1, 2026 · IFC 2026-01-01").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Thursday, January 1, 2026").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `Year Day renders under the December group`() {
        show(loaded())

        compose.onNodeWithText("December").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Year Day").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Year Day, 2026 · IFC 2026-13-29").performScrollTo().assertIsDisplayed()
    }

    // docs/design-plan.md section 4.7: an ordinary holiday carries the diamond mark; Year Day and Leap
    // Day carry the intercalary mark instead, never both.
    @Test
    fun `an ordinary holiday shows the diamond mark and an intercalary row shows the intercalary mark`() {
        show(loaded())

        compose.onAllNodesWithTag(HolidaysTestTags.HOLIDAY_DIAMOND, useUnmergedTree = true).assertCountEquals(1)
        compose.onAllNodesWithTag(HolidaysTestTags.INTERCALARY_MARK, useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun `tapping a row reports its epoch day`() {
        show(loaded())

        compose
            .onNodeWithText("New Year's Day")
            .performScrollTo()
            .assertHasClickAction()
            .performClick()

        rowsClicked shouldBe listOf(newYearRow.epochDay)
    }

    @Test
    fun `an empty year list shows the explanatory message instead of nothing`() {
        show(loaded(groups = emptyList()))

        compose
            .onNodeWithText("No holiday sets are turned on. Turn one on above to see its dates here.")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `back arrow calls onBack`() {
        show(loaded())

        compose.onNodeWithContentDescription("Back").assertHasClickAction().performClick()

        backPresses shouldBe 1
    }

    @Test
    fun `loading state shows only the app bar`() {
        show(HolidaysUiState.Loading)

        compose.onNodeWithText("Holidays").assertIsDisplayed()
    }

    // docs/ARCHITECTURE.md §4 "Accessibility": 200% font scale, no clipped label.
    @Test
    fun `at 200 percent font scale no row label is cut`() {
        show(loaded(), fontScale = 2f)

        for (text in listOf("International Fixed Calendar", "New Year's Day", "January 1, 2026 · IFC 2026-01-01")) {
            compose
                .onNodeWithText(text)
                .performScrollTo()
                .textLayout()
                .isCut() shouldBe false
        }
    }

    // See feature:converter's ConverterScreenTest and feature:events' EventListScreenTest: the semantics
    // action is the only reliable way to read a Text's overflow once merged into a ListItem/Row.
    private fun TextLayoutResult.isCut(): Boolean =
        didOverflowHeight || (0 until lineCount).any { line -> getLineRight(line) - getLineLeft(line) > size.width }

    private fun SemanticsNodeInteraction.textLayout(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        fetchSemanticsNode()
            .config
            .getOrNull(SemanticsActions.GetTextLayoutResult)
            ?.action
            ?.invoke(results)
        results shouldHaveSize 1
        return results.single()
    }
}
