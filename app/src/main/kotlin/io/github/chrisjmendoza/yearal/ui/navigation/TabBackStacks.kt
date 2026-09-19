package io.github.chrisjmendoza.yearal.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import io.github.chrisjmendoza.yearal.core.calendar.IfcDate
import io.github.chrisjmendoza.yearal.core.calendar.IfcYearMonth
import io.github.chrisjmendoza.yearal.core.navigation.DayKey
import io.github.chrisjmendoza.yearal.core.navigation.EventEditorKey
import io.github.chrisjmendoza.yearal.core.navigation.EventListKey
import io.github.chrisjmendoza.yearal.core.navigation.MonthKey
import io.github.chrisjmendoza.yearal.core.navigation.Navigator
import io.github.chrisjmendoza.yearal.core.navigation.TodayKey
import io.github.chrisjmendoza.yearal.intent.AppRoute
import io.github.chrisjmendoza.yearal.ui.TopLevelDestination
import java.time.LocalDate

/**
 * One back stack per top-level tab, plus the rule that the system back button leaves any other tab
 * for Today before it exits the app — the Navigation 3 "top-level back stack" recipe
 * (docs/ARCHITECTURE.md §4 "Screens and navigation"). The selected tab and every stack survive
 * configuration changes and process death.
 */
@Stable
class TabBackStacks internal constructor(
    selectedState: MutableState<TopLevelDestination>,
    private val stacks: Map<TopLevelDestination, NavBackStack<NavKey>>,
) : Navigator {
    /** The tab currently shown. */
    var selected: TopLevelDestination by selectedState
        private set

    /**
     * What [androidx.navigation3.ui.NavDisplay] renders: the selected tab's stack, prefixed by the
     * Today root when another tab is selected so that popping past a tab root lands on Today.
     */
    val backStack: List<NavKey>
        get() {
            val current = stacks.getValue(selected)
            return if (selected == TopLevelDestination.TODAY) {
                current.toList()
            } else {
                stacks.getValue(TopLevelDestination.TODAY).take(1) + current
            }
        }

    /**
     * Shows [destination], seeding its stack with [root] the first time it is opened. Re-selecting the
     * current tab pops it to its root.
     */
    fun select(
        destination: TopLevelDestination,
        root: NavKey,
    ) {
        val stack = stacks.getValue(destination)
        if (stack.isEmpty()) {
            stack.add(root)
        } else if (destination == selected) {
            while (stack.size > 1) stack.removeAt(stack.lastIndex)
        }
        selected = destination
    }

    override fun navigate(key: NavKey) {
        stacks.getValue(selected).add(key)
    }

    /**
     * Replaces [destination]'s **entire** back stack with [keys] and shows it — unlike [select], which
     * preserves whatever a tab already has, this always lands exactly on [keys] regardless of prior
     * state. Used only by intent-routed navigation ([applyRoute], ROADMAP M3 T5): a widget or
     * notification tap is a deliberate request for one specific screen, not a plain tab switch, so it
     * must not surface whatever the user happened to leave in that tab's history.
     *
     * @param keys the new stack, root first; must not be empty.
     */
    internal fun open(
        destination: TopLevelDestination,
        keys: List<NavKey>,
    ) {
        require(keys.isNotEmpty()) { "a routed back stack must have at least one entry" }
        val stack = stacks.getValue(destination)
        stack.clear()
        stack.addAll(keys)
        selected = destination
    }

    override fun goBack() {
        val stack = stacks.getValue(selected)
        when {
            stack.size > 1 -> stack.removeAt(stack.lastIndex)
            selected != TopLevelDestination.TODAY -> selected = TopLevelDestination.TODAY
        }
    }
}

/**
 * Applies [route] to [this]'s back stacks (ROADMAP M3 T5, M4 T10; `docs/ARCHITECTURE.md` §4 "Intent
 * routing"), and reports whether it actually did. Every case but [AppRoute.CurrentMonth] needs nothing
 * beyond [route] itself and always applies; [AppRoute.CurrentMonth] resolves "current" from [today]
 * (the app's own clock, via `DateTicker` — CLAUDE.md rule 2), so when `today` has not loaded yet (it is
 * `null` only until the first tick, which is immediate) this does nothing and returns `false`, so the
 * caller — [io.github.chrisjmendoza.yearal.ui.IfcApp] — knows not to discard the pending route yet and
 * retries once `today` is available, instead of silently losing it.
 *
 * [AppRoute.Default] does nothing and reports applied: [io.github.chrisjmendoza.yearal.intent.IntentRouter]
 * has already decided there is nothing to route, so the tabs stay exactly as they were (their normal
 * start destination on a cold start, or whatever the user was already looking at), and there is nothing
 * to retry.
 *
 * @return `true` once [route] has taken effect (or, for [AppRoute.Default], been correctly ignored);
 * `false` only when [AppRoute.CurrentMonth] is still waiting on [today].
 */
fun TabBackStacks.applyRoute(
    route: AppRoute,
    today: LocalDate?,
): Boolean {
    when (route) {
        AppRoute.Default -> {
            Unit
        }

        AppRoute.Today -> {
            open(TopLevelDestination.TODAY, listOf(TodayKey))
        }

        AppRoute.CurrentMonth -> {
            val month = today?.let { IfcYearMonth.from(IfcDate.from(it)) } ?: return false
            open(TopLevelDestination.CALENDAR, listOf(MonthKey(month.year, month.month.number)))
        }

        is AppRoute.Day -> {
            // Already validated by IntentRouter: converts and falls within IfcDate's supported years.
            val date = LocalDate.ofEpochDay(route.epochDay)
            val month = IfcYearMonth.from(IfcDate.from(date))
            open(
                TopLevelDestination.CALENDAR,
                listOf(MonthKey(month.year, month.month.number), DayKey(route.epochDay)),
            )
        }

        is AppRoute.EventDetail -> {
            open(TopLevelDestination.EVENTS, listOf(EventListKey, EventEditorKey(eventId = route.eventId)))
        }
    }
    return true
}

/** Creates the per-tab back stacks, remembered and saved with the composition. */
@Composable
fun rememberTabBackStacks(): TabBackStacks {
    val selected = rememberSaveable { mutableStateOf(TopLevelDestination.TODAY) }
    val stacks =
        TopLevelDestination.entries.associateWith { destination ->
            val root = destination.rootKey
            if (root == null) rememberNavBackStack() else rememberNavBackStack(root)
        }
    return remember(selected, stacks) { TabBackStacks(selected, stacks) }
}
