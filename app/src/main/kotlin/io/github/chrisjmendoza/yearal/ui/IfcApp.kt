package io.github.chrisjmendoza.yearal.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.chrisjmendoza.yearal.BuildConfig
import io.github.chrisjmendoza.yearal.MainViewModel
import io.github.chrisjmendoza.yearal.R
import io.github.chrisjmendoza.yearal.core.calendar.IfcYearMonth
import io.github.chrisjmendoza.yearal.core.calendar.toIfcDate
import io.github.chrisjmendoza.yearal.core.navigation.ConverterKey
import io.github.chrisjmendoza.yearal.core.navigation.EventEditorKey
import io.github.chrisjmendoza.yearal.core.navigation.EventListKey
import io.github.chrisjmendoza.yearal.core.navigation.HolidaysKey
import io.github.chrisjmendoza.yearal.core.navigation.IntroKey
import io.github.chrisjmendoza.yearal.core.navigation.LearnKey
import io.github.chrisjmendoza.yearal.core.navigation.MonthKey
import io.github.chrisjmendoza.yearal.core.navigation.MoreKey
import io.github.chrisjmendoza.yearal.core.navigation.PrivacyKey
import io.github.chrisjmendoza.yearal.core.navigation.SettingsKey
import io.github.chrisjmendoza.yearal.core.navigation.TodayKey
import io.github.chrisjmendoza.yearal.core.navigation.YearKey
import io.github.chrisjmendoza.yearal.feature.calendar.month.MonthRoute
import io.github.chrisjmendoza.yearal.feature.calendar.today.TodayRoute
import io.github.chrisjmendoza.yearal.feature.calendar.year.YearRoute
import io.github.chrisjmendoza.yearal.feature.converter.ConverterRoute
import io.github.chrisjmendoza.yearal.feature.events.editor.EventEditorRoute
import io.github.chrisjmendoza.yearal.feature.events.list.EventListRoute
import io.github.chrisjmendoza.yearal.feature.holidays.HolidaysRoute
import io.github.chrisjmendoza.yearal.feature.settings.intro.IntroRoute
import io.github.chrisjmendoza.yearal.feature.settings.learn.LearnRoute
import io.github.chrisjmendoza.yearal.feature.settings.more.MoreRoute
import io.github.chrisjmendoza.yearal.feature.settings.privacy.PrivacyRoute
import io.github.chrisjmendoza.yearal.feature.settings.settings.SettingsRoute
import io.github.chrisjmendoza.yearal.ui.navigation.applyRoute
import io.github.chrisjmendoza.yearal.ui.navigation.rememberTabBackStacks

/**
 * The app shell: the five top-level tabs in a [NavigationSuiteScaffold] (bar on compact widths, rail
 * from medium up) and one [NavDisplay] over the selected tab's back stack
 * (docs/ARCHITECTURE.md §4 "Screens and navigation").
 *
 * **The first-run intro (`docs/FEATURES.md` L1).** [introGateViewModel] reports whether the store has
 * confirmed the intro not yet seen; the moment it does, this pushes
 * [io.github.chrisjmendoza.yearal.core.navigation.IntroKey] onto the Today tab's stack — "over" the
 * Today screen the tab already starts on, exactly as the feature describes it — never before the real
 * value has loaded (see [IntroGateViewModel]'s KDoc for why a returning user must not see a flash of it)
 * and never more than once per app process ([introOffered]). Skipping or finishing the intro marks it
 * seen and pops it back off, revealing Today underneath; the Learn screen can also push it again later
 * for a user who skipped it.
 */
@Composable
fun IfcApp(
    viewModel: MainViewModel = hiltViewModel(),
    introGateViewModel: IntroGateViewModel = hiltViewModel(),
) {
    val today by viewModel.today.collectAsStateWithLifecycle()
    val tabs = rememberTabBackStacks()

    // Intent routing (ROADMAP M3 T5, M4 T10; docs/ARCHITECTURE.md §4 "Intent routing"): MainActivity
    // handed the resolved AppRoute to the ViewModel from onCreate/onNewIntent; this applies it to the
    // tab back stacks the moment both it and (for AppRoute.CurrentMonth) `today` are available, then
    // consumes it so a later recomposition -- a settings change, a rotation -- never re-applies it.
    val pendingRoute by viewModel.pendingRoute.collectAsStateWithLifecycle()
    LaunchedEffect(pendingRoute, today) {
        val route = pendingRoute ?: return@LaunchedEffect
        if (tabs.applyRoute(route, today)) viewModel.consumeRoute()
    }

    val hasSeenIntro by introGateViewModel.hasSeenIntro.collectAsStateWithLifecycle()
    var introOffered by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(hasSeenIntro) {
        val seen = hasSeenIntro ?: return@LaunchedEffect
        if (!introOffered && !seen) {
            introOffered = true
            tabs.navigate(IntroKey)
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                item(
                    selected = tabs.selected == destination,
                    onClick = {
                        val root =
                            destination.rootKey
                                ?: today?.let { date ->
                                    val month = IfcYearMonth.from(date.toIfcDate())
                                    MonthKey(month.year, month.month.number)
                                }
                        if (root != null) tabs.select(destination, root)
                    },
                    icon = {
                        when (val icon = destination.icon) {
                            is TabIcon.Vector -> Icon(icon.imageVector, contentDescription = null)
                            is TabIcon.Resource -> Icon(painterResource(icon.id), contentDescription = null)
                        }
                    },
                    label = { Text(stringResource(destination.labelRes)) },
                )
            }
        },
    ) {
        NavDisplay(
            backStack = tabs.backStack,
            onBack = { tabs.goBack() },
            // The ViewModel decorator scopes each entry's @HiltViewModel to that entry, so a screen's
            // state is cleared when it is popped rather than living as long as the activity.
            entryDecorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
            entryProvider =
                entryProvider {
                    entry<TodayKey> { TodayRoute(navigator = tabs) }
                    entry<IntroKey> { IntroRoute(navigator = tabs) }
                    entry<MonthKey> { key -> MonthRoute(key = key, navigator = tabs) }
                    entry<YearKey> { key -> YearRoute(key = key, navigator = tabs) }
                    entry<EventListKey> { EventListRoute(navigator = tabs) }
                    entry<EventEditorKey> { key -> EventEditorRoute(key = key, navigator = tabs) }
                    entry<ConverterKey> { key -> ConverterRoute(key = key, navigator = tabs) }
                    entry<MoreKey> {
                        MoreRoute(
                            navigator = tabs,
                            appName = stringResource(R.string.app_name),
                            versionName = BuildConfig.VERSION_NAME,
                        )
                    }
                    entry<HolidaysKey> { HolidaysRoute(navigator = tabs) }
                    entry<SettingsKey> { SettingsRoute(navigator = tabs) }
                    entry<LearnKey> { LearnRoute(navigator = tabs) }
                    entry<PrivacyKey> { PrivacyRoute(navigator = tabs) }
                },
        )
    }
}
