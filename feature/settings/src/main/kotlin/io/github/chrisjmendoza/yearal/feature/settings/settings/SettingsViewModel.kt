package io.github.chrisjmendoza.yearal.feature.settings.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.chrisjmendoza.yearal.core.domain.event.EventRepository
import io.github.chrisjmendoza.yearal.core.domain.settings.SettingsRepository
import io.github.chrisjmendoza.yearal.core.domain.settings.ThemeMode
import io.github.chrisjmendoza.yearal.core.domain.settings.UserSettings
import io.github.chrisjmendoza.yearal.core.domain.settings.WeekdayDisplay
import io.github.chrisjmendoza.yearal.feature.settings.di.DynamicColorSupported
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State holder for the Settings screen (docs/ARCHITECTURE.md §4 "State management"; FEATURES W1, W2,
 * H5). The state mirrors [SettingsRepository.settings]; every intent is a read-modify-write through
 * [SettingsRepository.update], so the screen never holds a value the store does not.
 *
 * Browsing and toggling holiday sets (FEATURES H5) lives on the Holidays screen since ROADMAP M6 T2;
 * this screen only links there (`io.github.chrisjmendoza.yearal.feature.settings.more.MoreScreen` also
 * links to it directly from the More hub), rather than duplicating a second set of switches over the
 * same [UserSettings.enabledHolidaySets] — both screens read the identical [SettingsRepository.settings]
 * flow, so there is exactly one source of truth and nothing to keep in sync by hand.
 *
 * "Delete all data" (FEATURES W6) is a two-step [DeleteAllDataStep] intent: [requestDeleteAllData]
 * opens the first confirmation, [continueDeleteAllData] opens the final one,
 * [confirmDeleteAllData] erases [EventRepository.deleteAllData] and resets settings to
 * [UserSettings.DEFAULT] through [SettingsRepository.update] (the interface's own general-purpose
 * reset — `:core:data`'s caches and alarms are the caller's job, per the contract), and
 * [cancelDeleteAllData] / [dismissDeleteAllDataDone] back out at any point.
 *
 * @param dynamicColorSupported whether the platform offers wallpaper-derived colour (API 31+);
 * injected so tests can cover both cases.
 */
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val repository: SettingsRepository,
        @DynamicColorSupported dynamicColorSupported: Boolean,
        private val eventRepository: EventRepository,
    ) : ViewModel() {
        private val deleteAllDataStep = MutableStateFlow(DeleteAllDataStep.NONE)

        /** [SettingsUiState.Loading] until the store answers, then a [SettingsUiState.Loaded] per change. */
        val uiState: StateFlow<SettingsUiState> =
            combine(repository.settings, deleteAllDataStep) { settings, step ->
                SettingsUiState.Loaded(settings, dynamicColorSupported, step)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SettingsUiState.Loading)

        /** Opens the first "Delete all data" confirmation. */
        fun requestDeleteAllData() {
            deleteAllDataStep.value = DeleteAllDataStep.CONFIRM_FIRST
        }

        /** Moves from the first confirmation to the final, unmistakably destructive one. */
        fun continueDeleteAllData() {
            deleteAllDataStep.value = DeleteAllDataStep.CONFIRM_SECOND
        }

        /** Backs out of either confirmation without changing anything. */
        fun cancelDeleteAllData() {
            deleteAllDataStep.value = DeleteAllDataStep.NONE
        }

        /** Erases every event and calendar, resets settings to defaults, then shows completion. */
        fun confirmDeleteAllData() {
            viewModelScope.launch {
                eventRepository.deleteAllData()
                repository.update { UserSettings.DEFAULT }
                deleteAllDataStep.value = DeleteAllDataStep.DONE
            }
        }

        /** Dismisses the completion notice. */
        fun dismissDeleteAllDataDone() {
            deleteAllDataStep.value = DeleteAllDataStep.NONE
        }

        /** Stores the grid-header mode (FEATURES W1; `BOTH` is the default, ROADMAP decision #5). */
        fun setWeekdayDisplay(display: WeekdayDisplay) {
            update { it.copy(weekdayDisplay = display) }
        }

        /** Stores the light/dark/system choice (FEATURES W2). */
        fun setThemeMode(mode: ThemeMode) {
            update { it.copy(themeMode = mode) }
        }

        /**
         * Stores the Material You preference (FEATURES W2). The value is stored even on devices that
         * cannot honour it, but the screen disables the control there, so this is only reached on API 31+.
         */
        fun setDynamicColor(enabled: Boolean) {
            update { it.copy(dynamicColor = enabled) }
        }

        private fun update(transform: (UserSettings) -> UserSettings) {
            viewModelScope.launch { repository.update(transform) }
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
