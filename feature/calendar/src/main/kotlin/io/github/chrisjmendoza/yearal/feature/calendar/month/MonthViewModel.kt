package io.github.chrisjmendoza.yearal.feature.calendar.month

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.chrisjmendoza.yearal.core.calendar.IfcDate
import io.github.chrisjmendoza.yearal.core.calendar.IfcYearMonth
import io.github.chrisjmendoza.yearal.core.domain.DateTicker
import io.github.chrisjmendoza.yearal.core.domain.event.ObserveAgendaUseCase
import io.github.chrisjmendoza.yearal.core.domain.settings.SettingsRepository
import io.github.chrisjmendoza.yearal.core.domain.settings.UserSettings
import io.github.chrisjmendoza.yearal.feature.calendar.holiday.HolidayCatalog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/**
 * State holder for the Month pager (docs/ARCHITECTURE.md §4 "State management"; FEATURES C1, C3, C4,
 * C5, C7). "Today" comes only from [DateTicker] (CLAUDE.md rule 2), so the today ring and the
 * "Today" target roll over at local midnight; the grid headers follow
 * [UserSettings.weekdayDisplay], the holiday marks [UserSettings.enabledHolidaySets], and the event
 * dots [observeAgenda], all live.
 *
 * Holidays are evaluated for the current page and its two neighbours — the three pages the pager
 * keeps warm (docs/ARCHITECTURE.md §3.4) — through the memoised [HolidayCatalog]; event counts for the
 * same three pages come from [observeAgenda], the single place events are expanded and bucketed, so
 * paging never computes a date or an occurrence itself (CLAUDE.md rule 1).
 *
 * The starting month is assisted-injected from the `MonthKey` of the entry (see [Factory]); the
 * ViewModel has no other dependency on navigation, so tests build it with the constructor. Stops
 * collecting five seconds after the last subscriber leaves.
 *
 * @param initialMonth the month the pager opens on; its page is the state's first `currentPage`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = MonthViewModel.Factory::class)
class MonthViewModel
    @AssistedInject
    constructor(
        @Assisted initialMonth: IfcYearMonth,
        dateTicker: DateTicker,
        settingsRepository: SettingsRepository,
        private val catalog: HolidayCatalog,
        private val observeAgenda: ObserveAgendaUseCase,
    ) : ViewModel() {
        /** Creates a [MonthViewModel] for the entry's month; used by `hiltViewModel(creationCallback)`. */
        @AssistedFactory
        interface Factory {
            /** @param initialMonth the month to open on, already clamped by `MonthPages.monthOf`. */
            fun create(initialMonth: IfcYearMonth): MonthViewModel
        }

        private val page = MutableStateFlow(MonthPages.pageOf(initialMonth))
        private val selected = MutableStateFlow<LocalDate?>(null)

        // Per-month event-count flows, shared and cached across page changes (see agendaCountsFor):
        // without this, every page change tore down and rebuilt the ObserveAgendaUseCase subscription
        // for all three warm months, including the two that stayed warm across a single-page swipe.
        private val monthAgendaCache = mutableMapOf<IfcYearMonth, Flow<Map<LocalDate, Int>>>()

        private val eventCountsByPage: Flow<Map<IfcYearMonth, Map<LocalDate, Int>>> =
            page.flatMapLatest { p -> eventCountsAround(p) }

        /**
         * The current [MonthUiState]. Starts with the initial page, no today and default settings;
         * the first tick, the stored settings and the event counts arrive on subscription.
         */
        val uiState: StateFlow<MonthUiState> =
            combine(
                dateTicker.today,
                settingsRepository.settings,
                page,
                selected,
                eventCountsByPage,
            ) { today, settings, page, selected, eventCounts ->
                MonthUiState(
                    currentPage = page,
                    today = today,
                    todayPage = MonthPages.pageOf(IfcYearMonth.from(IfcDate.from(today))),
                    selected = selected,
                    weekdayDisplay = settings.weekdayDisplay,
                    holidaysByMonth = holidaysAround(page, settings.enabledHolidaySets),
                    eventCountsByMonth = eventCounts,
                )
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                MonthUiState(currentPage = page.value, today = null, todayPage = null, selected = null),
            )

        /**
         * Records that the pager now shows [page] (FEATURES C1), so holidays and event counts are
         * evaluated around it. Out-of-range values are clamped to `0..MonthPages.LAST_PAGE`.
         */
        fun showPage(page: Int) {
            this.page.value = page.coerceIn(0, MonthPages.LAST_PAGE)
        }

        /**
         * Marks [date] as the selected day (FEATURES C5); the grid fills its cell or band. At compact
         * and medium widths this also names the day pushed as `DayKey`; at expanded widths it is the
         * expanded-width list-detail pane's own selection (docs/ROADMAP.md M3 T4) — the same property
         * serves both, so switching width classes mid-session never loses or duplicates a selection.
         */
        fun select(date: LocalDate) {
            selected.value = date
        }

        /**
         * Clears the selection (docs/ROADMAP.md M3 T4): the expanded-width list-detail pane's empty
         * state returns, and the grid's cell fill is removed. Invoked by the detail pane's close action
         * and by the system back gesture while a day is selected at expanded widths — at compact and
         * medium widths the selection is left alone (`DayKey`'s own back pops the sheet instead).
         */
        fun clearSelection() {
            selected.value = null
        }

        private fun holidaysAround(
            page: Int,
            enabledSetIds: Set<String>,
        ): Map<IfcYearMonth, Map<LocalDate, String>> =
            warmMonths(page).associateWith { month -> catalog.gridLabels(enabledSetIds, month.gregorianRange) }

        /**
         * The current page's warm-month event counts (ARCHITECTURE §3.4): reuses [agendaCountsFor] for
         * every warm month, so a month that stays warm across a page change keeps its existing
         * [ObserveAgendaUseCase] subscription (and its underlying Room query) alive instead of
         * cancelling and re-issuing it — moving one page forward keeps two of the three months
         * unchanged. [monthAgendaCache] is trimmed to exactly the months still warm after building this
         * flow, so it never holds more than three entries.
         */
        private fun eventCountsAround(page: Int): Flow<Map<IfcYearMonth, Map<LocalDate, Int>>> {
            val months = warmMonths(page)
            if (months.isEmpty()) {
                monthAgendaCache.clear()
                return flowOf(emptyMap())
            }
            val perMonth = months.map { month -> agendaCountsFor(month).map { counts -> month to counts } }
            monthAgendaCache.keys.retainAll(months.toSet())
            return combine(perMonth) { pairs -> pairs.toMap() }
        }

        /**
         * [month]'s event counts, shared so every caller within the warm window (currently just
         * [eventCountsAround], called once per page change) collects the same upstream
         * [ObserveAgendaUseCase.invoke] subscription rather than starting a new one. `replay = 1` gives
         * a subscriber that reattaches after a brief gap (the page moving away and back) the last known
         * value immediately, matching the interface's own "emits the current value on collection"
         * guarantee instead of forcing a fresh wait; [SharingStarted.WhileSubscribed] still lets the
         * underlying query stop once nothing reads it for [STOP_TIMEOUT_MILLIS].
         */
        private fun agendaCountsFor(month: IfcYearMonth): Flow<Map<LocalDate, Int>> =
            monthAgendaCache.getOrPut(month) {
                observeAgenda(month.gregorianRange)
                    .map { agendas -> agendas.mapValues { (_, agenda) -> agenda.entries.size } }
                    .shareIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), replay = 1)
            }

        private fun warmMonths(page: Int): List<IfcYearMonth> =
            ((page - 1)..(page + 1)).filter { it in 0..MonthPages.LAST_PAGE }.map(MonthPages::monthAt)

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
