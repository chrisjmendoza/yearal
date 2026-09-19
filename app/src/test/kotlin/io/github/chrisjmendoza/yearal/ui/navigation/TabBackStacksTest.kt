package io.github.chrisjmendoza.yearal.ui.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import io.github.chrisjmendoza.yearal.core.navigation.ConverterKey
import io.github.chrisjmendoza.yearal.core.navigation.DayKey
import io.github.chrisjmendoza.yearal.core.navigation.EventEditorKey
import io.github.chrisjmendoza.yearal.core.navigation.EventListKey
import io.github.chrisjmendoza.yearal.core.navigation.MonthKey
import io.github.chrisjmendoza.yearal.core.navigation.MoreKey
import io.github.chrisjmendoza.yearal.core.navigation.TodayKey
import io.github.chrisjmendoza.yearal.intent.AppRoute
import io.github.chrisjmendoza.yearal.ui.TopLevelDestination
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.time.LocalDate

// The per-tab back stack rules of docs/ARCHITECTURE.md §4 "Screens and navigation", checked without a
// composition: NavBackStack and mutableStateOf work on the plain JVM.
class TabBackStacksTest {
    private fun tabs(): TabBackStacks {
        val stacks =
            TopLevelDestination.entries.associateWith { destination ->
                val root = destination.rootKey
                if (root == null) NavBackStack<NavKey>() else NavBackStack<NavKey>(root)
            }
        return TabBackStacks(mutableStateOf(TopLevelDestination.TODAY), stacks)
    }

    @Test
    fun `starts on Today with only its root`() {
        val tabs = tabs()
        tabs.selected shouldBe TopLevelDestination.TODAY
        tabs.backStack shouldContainExactly listOf(TodayKey)
    }

    @Test
    fun `another tab is shown above the Today root and back returns to Today`() {
        val tabs = tabs()
        tabs.select(TopLevelDestination.EVENTS, EventListKey)
        tabs.backStack shouldContainExactly listOf(TodayKey, EventListKey)
        tabs.goBack()
        tabs.selected shouldBe TopLevelDestination.TODAY
        tabs.backStack shouldContainExactly listOf(TodayKey)
    }

    @Test
    fun `each tab keeps its own stack while another tab is shown`() {
        val tabs = tabs()
        val month = MonthKey(2026, 10)
        tabs.select(TopLevelDestination.CALENDAR, month)
        tabs.navigate(DayKey(20_713))
        tabs.select(TopLevelDestination.CONVERT, ConverterKey())
        tabs.backStack shouldContainExactly listOf(TodayKey, ConverterKey())
        tabs.select(TopLevelDestination.CALENDAR, MonthKey(2027, 1))
        // The root given on a later selection is ignored: the tab already has a stack.
        tabs.backStack shouldContainExactly listOf(TodayKey, month, DayKey(20_713))
    }

    @Test
    fun `back pops within a tab before leaving it`() {
        val tabs = tabs()
        tabs.select(TopLevelDestination.CALENDAR, MonthKey(2026, 10))
        tabs.navigate(DayKey(20_713))
        tabs.goBack()
        tabs.selected shouldBe TopLevelDestination.CALENDAR
        tabs.backStack shouldContainExactly listOf(TodayKey, MonthKey(2026, 10))
        tabs.goBack()
        tabs.selected shouldBe TopLevelDestination.TODAY
    }

    @Test
    fun `re-selecting the current tab pops it to its root`() {
        val tabs = tabs()
        tabs.select(TopLevelDestination.MORE, MoreKey)
        tabs.navigate(DayKey(1))
        tabs.navigate(DayKey(2))
        tabs.select(TopLevelDestination.MORE, MoreKey)
        tabs.backStack shouldContainExactly listOf(TodayKey, MoreKey)
    }

    @Test
    fun `back at the Today root does nothing`() {
        val tabs = tabs()
        tabs.goBack()
        tabs.selected shouldBe TopLevelDestination.TODAY
        tabs.backStack shouldContainExactly listOf(TodayKey)
    }

    // -- ROADMAP M3 T5, M4 T10: applyRoute -------------------------------------------------------------

    @Test
    fun `AppRoute Default touches nothing, whatever the tabs already show`() {
        val tabs = tabs()
        tabs.select(TopLevelDestination.EVENTS, EventListKey)

        val applied = tabs.applyRoute(AppRoute.Default, today = null)

        applied shouldBe true
        tabs.selected shouldBe TopLevelDestination.EVENTS
        tabs.backStack shouldContainExactly listOf(TodayKey, EventListKey)
    }

    @Test
    fun `AppRoute Today selects the Today tab`() {
        val tabs = tabs()
        tabs.select(TopLevelDestination.EVENTS, EventListKey)

        tabs.applyRoute(AppRoute.Today, today = null) shouldBe true

        tabs.selected shouldBe TopLevelDestination.TODAY
        tabs.backStack shouldContainExactly listOf(TodayKey)
    }

    @Test
    fun `AppRoute CurrentMonth opens the Calendar tab on today's month`() {
        val tabs = tabs()

        val applied = tabs.applyRoute(AppRoute.CurrentMonth, today = LocalDate.of(2026, 9, 17))

        applied shouldBe true
        tabs.selected shouldBe TopLevelDestination.CALENDAR
        tabs.backStack shouldContainExactly listOf(TodayKey, MonthKey(2026, 10)) // IFC September 2026
    }

    @Test
    fun `AppRoute CurrentMonth is not applied while today has not loaded yet`() {
        val tabs = tabs()

        val applied = tabs.applyRoute(AppRoute.CurrentMonth, today = null)

        applied shouldBe false
        tabs.selected shouldBe TopLevelDestination.TODAY
    }

    @Test
    fun `AppRoute CurrentMonth replaces a stale Calendar stack, not just switches to it`() {
        val tabs = tabs()
        tabs.select(TopLevelDestination.CALENDAR, MonthKey(2020, 1))
        tabs.navigate(DayKey(1))

        tabs.applyRoute(AppRoute.CurrentMonth, today = LocalDate.of(2026, 9, 17))

        tabs.backStack shouldContainExactly listOf(TodayKey, MonthKey(2026, 10))
    }

    @Test
    fun `AppRoute Day opens the Calendar tab on that day's month, then the day itself`() {
        val tabs = tabs()
        val epochDay = LocalDate.of(2026, 9, 17).toEpochDay()

        val applied = tabs.applyRoute(AppRoute.Day(epochDay), today = null)

        applied shouldBe true
        tabs.selected shouldBe TopLevelDestination.CALENDAR
        tabs.backStack shouldContainExactly listOf(TodayKey, MonthKey(2026, 10), DayKey(epochDay))
    }

    @Test
    fun `AppRoute Day replaces a stale Calendar stack entirely`() {
        val tabs = tabs()
        tabs.select(TopLevelDestination.CALENDAR, MonthKey(2020, 1))
        tabs.navigate(DayKey(999))
        val epochDay = LocalDate.of(2026, 9, 17).toEpochDay()

        tabs.applyRoute(AppRoute.Day(epochDay), today = null)

        tabs.backStack shouldContainExactly listOf(TodayKey, MonthKey(2026, 10), DayKey(epochDay))
    }

    @Test
    fun `AppRoute EventDetail opens the Events tab on the list, then the editor`() {
        val tabs = tabs()

        val applied = tabs.applyRoute(AppRoute.EventDetail(42L), today = null)

        applied shouldBe true
        tabs.selected shouldBe TopLevelDestination.EVENTS
        tabs.backStack shouldContainExactly listOf(TodayKey, EventListKey, EventEditorKey(eventId = 42L))
    }
}
