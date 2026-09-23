package io.github.chrisjmendoza.yearal.feature.holidays

import android.content.Context
import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.chrisjmendoza.yearal.core.calendar.IfcDate
import io.github.chrisjmendoza.yearal.core.calendar.IfcMonth
import io.github.chrisjmendoza.yearal.core.calendar.IfcYearMonth
import io.github.chrisjmendoza.yearal.core.designsystem.format.IfcDateFormatter
import io.github.chrisjmendoza.yearal.core.designsystem.picker.DatePickerRange
import io.github.chrisjmendoza.yearal.core.domain.DateTicker
import io.github.chrisjmendoza.yearal.core.domain.holiday.HolidayEngine
import io.github.chrisjmendoza.yearal.core.domain.holiday.HolidayOccurrence
import io.github.chrisjmendoza.yearal.core.domain.holiday.HolidaySet
import io.github.chrisjmendoza.yearal.core.domain.settings.SettingsRepository
import io.github.chrisjmendoza.yearal.core.domain.settings.UserSettings
import io.github.chrisjmendoza.yearal.core.holidays.BundledHolidayPacks
import io.github.chrisjmendoza.yearal.core.holidays.HolidayPackLoader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

/**
 * State holder for the Holidays screen (docs/ARCHITECTURE.md §4 "State management"; ROADMAP M6 T2;
 * FEATURES H1, H2, H3, H5, H7).
 *
 * **Browsing and toggling** (task 1) reads the full bundled catalogue — [BundledHolidayPacks.all]
 * loaded once per instance, the same one-parse-per-process-lifetime policy `HolidayCatalog` and
 * `SettingsViewModel` already use — and writes a toggle straight through [SettingsRepository.update].
 * `:feature:settings`'s own Settings screen no longer duplicates these switches (this screen is now the
 * one place to turn a set on or off); both screens read the identical [SettingsRepository.settings]
 * flow, so there is exactly one source of truth and nothing to keep in sync by hand.
 *
 * **The per-year list** (task 2) evaluates only the *enabled* sets with [HolidayEngine] over the whole
 * Gregorian year — which is exactly the IFC year, since the IFC day-of-year equals the Gregorian one
 * (`docs/ARCHITECTURE.md` §3.1) — and groups the results by IFC month. [year] follows
 * [DateTicker] until the user pages away from it with [goToYear], exactly like
 * [io.github.chrisjmendoza.yearal.feature.converter.ConverterViewModel]'s `followsToday`; unlike the
 * Year overview's own `YearViewModel` (whose shown year is fixed once its screen opens), a still-default
 * Holidays screen left open across midnight on December 31 moves into the next year on its own, because
 * nothing else marks "today" on this screen.
 *
 * All evaluation happens on [Dispatchers.Default], off the caller's thread, even though
 * [HolidayEngine.occurrences] is itself microsecond-cheap and memoised (`docs/ARCHITECTURE.md` §3.3):
 * `docs/ARCHITECTURE.md` §4 asks every ViewModel to keep the calling thread free regardless.
 *
 * @param context only for [Resources]: the "(observed)" suffix and the sources line are formatted from
 * this module's own string resources (CLAUDE.md rule 9), the same pattern
 * [io.github.chrisjmendoza.yearal.feature.calendar.holiday.HolidayCatalog] uses.
 */
@HiltViewModel
class HolidaysViewModel
    internal constructor(
        private val settingsRepository: SettingsRepository,
        private val loader: HolidayPackLoader,
        private val engine: HolidayEngine,
        dateTicker: DateTicker,
        private val formatter: IfcDateFormatter,
        @ApplicationContext context: Context,
        /**
         * Where pack parsing and holiday evaluation run, off the caller's thread.
         *
         * Injectable **only so a test can pass its own dispatcher**: with the hard-coded
         * [Dispatchers.Default] the upstream work ran on a real thread pool while the test drove virtual
         * time, so whether the state arrived before Turbine gave up was a real-time race — it passed
         * alone and failed in a loaded full-suite run (docs/WORKFLOW.md §2). Hilt always supplies
         * [Dispatchers.Default] through the [Inject] constructor below.
         */
        private val workDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        @Inject
        constructor(
            settingsRepository: SettingsRepository,
            loader: HolidayPackLoader,
            engine: HolidayEngine,
            dateTicker: DateTicker,
            formatter: IfcDateFormatter,
            @ApplicationContext context: Context,
        ) : this(settingsRepository, loader, engine, dateTicker, formatter, context, Dispatchers.Default)

        private val resources: Resources = context.resources

        // Loaded lazily, and once, so a process that never opens this screen never parses a pack
        // (the same policy as HolidayCatalog and PackHolidaySetProvider).
        private val allSets: List<HolidaySet> by lazy { BundledHolidayPacks.all.map(loader::loadBundled) }

        /** `null` means "follow [DateTicker]"; a value means the user paged away from the default. */
        private val chosenYear = MutableStateFlow<Int?>(null)

        private val year: Flow<Int> =
            combine(chosenYear, dateTicker.today) { chosen, today ->
                chosen ?: clampYear(IfcDate.from(today).year)
            }.distinctUntilChanged()

        /** [HolidaysUiState.Loading] until the store and the ticker have both answered, then a [HolidaysUiState.Loaded] per change. */
        val uiState: StateFlow<HolidaysUiState> =
            combine(settingsRepository.settings, year) { settings, y -> settings to y }
                .map { (settings, y) -> buildLoaded(settings, y) }
                .flowOn(workDispatcher)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HolidaysUiState.Loading)

        /**
         * Adds [id] to or removes it from [UserSettings.enabledHolidaySets]. The same setting Settings
         * itself reads, so both screens agree the moment either one changes it.
         */
        fun setHolidaySetEnabled(
            id: String,
            enabled: Boolean,
        ) {
            viewModelScope.launch {
                settingsRepository.update { current ->
                    val sets = if (enabled) current.enabledHolidaySets + id else current.enabledHolidaySets - id
                    current.copy(enabledHolidaySets = sets)
                }
            }
        }

        /**
         * Shows [newYear], clamped into [DatePickerRange]. Once called, [year] no longer follows
         * [DateTicker] for the life of this ViewModel.
         */
        fun goToYear(newYear: Int) {
            chosenYear.value = clampYear(newYear)
        }

        private fun buildLoaded(
            settings: UserSettings,
            year: Int,
        ): HolidaysUiState.Loaded {
            val locale = Locale.getDefault()
            val setRows = allSets.map { set -> set.toRow(locale, enabled = set.id in settings.enabledHolidaySets) }
            val enabledSets = allSets.filter { it.id in settings.enabledHolidaySets }
            val occurrences =
                if (enabledSets.isEmpty()) emptyList() else engine.occurrences(enabledSets, wholeYearRange(year))
            val groups =
                occurrences
                    .groupBy { IfcMonth.of(IfcDate.from(it.date).monthNumber) }
                    .map { (month, monthOccurrences) ->
                        HolidayMonthGroup(formatter.monthName(month), monthOccurrences.map { it.toRow(locale) })
                    }
            return HolidaysUiState.Loaded(sets = setRows, year = year, groups = groups)
        }

        private fun HolidaySet.toRow(
            locale: Locale,
            enabled: Boolean,
        ): HolidaySetRow =
            HolidaySetRow(
                id = id,
                name = nameFor(locale.toLanguageTag()),
                region =
                    region?.let { code ->
                        // "und-US" is a valid tag with no language; an unknown region has no display
                        // name, so the code itself is shown instead (the same fallback SettingsViewModel
                        // used to apply for this exact conversion).
                        Locale.forLanguageTag("und-$code").getDisplayCountry(locale).ifEmpty { code }
                    },
                holidayCount = holidays.size,
                sources = sources.takeIf { it.isNotEmpty() }?.joinToString(listSeparator()),
                enabled = enabled,
            )

        private fun HolidayOccurrence.toRow(locale: Locale): HolidayOccurrenceRow {
            val ifcDate = IfcDate.from(date)
            val tag = locale.toLanguageTag()
            val names = holiday.name
            val baseName = holiday.nameFor(if (tag in names) tag else locale.language)
            val name = if (observed) format(R.string.holidays_observed, baseName) else baseName
            val ifcLong = formatter.formatLong(ifcDate)
            val ifcNumeric = formatter.formatNumeric(ifcDate)
            val gregorianLong = formatter.formatGregorianLong(date)
            val ifcLine = format(R.string.holidays_row_ifc_line, ifcLong, ifcNumeric)
            return HolidayOccurrenceRow(
                epochDay = date.toEpochDay(),
                name = name,
                ifcLong = ifcLong,
                ifcNumeric = ifcNumeric,
                gregorianLong = gregorianLong,
                description = format(R.string.holidays_row_description, name, ifcLine, gregorianLong),
                isIntercalary = ifcDate.isIntercalary,
            )
        }

        private fun listSeparator(): String = resources.getString(R.string.holidays_list_separator)

        // Formats with the injected locale, exactly as IfcDateFormatter.format does internally, rather
        // than Resources.getString(id, args), so numerals stay consistent with the rest of the screen.
        private fun format(
            pattern: Int,
            vararg args: Any,
        ): String = String.format(Locale.getDefault(), resources.getString(pattern), *args)

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }

/** [year], or the nearest year inside [DatePickerRange] (ARCHITECTURE "Reconciled decisions" 6). */
private fun clampYear(year: Int): Int = year.coerceIn(DatePickerRange.MIN_YEAR, DatePickerRange.MAX_YEAR)

/**
 * The Gregorian range of every date IFC year [year] covers: January 1 through the year's own Year Day
 * (spec §7.2), built from `:core:calendar` and nothing else (CLAUDE.md rule 1) — the same construction
 * `YearViewModel.wholeYearRange` uses. December's `trailingIntercalary` is never `null` (every year has
 * a Year Day), so the `!!` here is total.
 */
private fun wholeYearRange(year: Int): ClosedRange<LocalDate> {
    val firstDay = IfcYearMonth(year, IfcMonth.JANUARY).firstDay.toLocalDate()
    val yearDay = IfcYearMonth(year, IfcMonth.DECEMBER).trailingIntercalary!!.toLocalDate()
    return firstDay..yearDay
}
