package io.github.chrisjmendoza.yearal.feature.settings.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.chrisjmendoza.yearal.core.domain.settings.SettingsRepository
import io.github.chrisjmendoza.yearal.core.domain.settings.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * State holder for [MoreRoute]. The hub itself is static (four navigation rows plus About), but the
 * "Send feedback" row's diagnostics (ROADMAP M8 T6) need the current [UserSettings] -- colour source,
 * theme mode, weekday display and the enabled-holiday-set count -- so this exposes exactly that flow,
 * mirroring [io.github.chrisjmendoza.yearal.feature.settings.settings.SettingsViewModel]'s shape.
 * [UserSettings.DEFAULT] until the store answers once, same as every other reader of this flow.
 */
@HiltViewModel
class MoreViewModel
    @Inject
    constructor(
        repository: SettingsRepository,
    ) : ViewModel() {
        /** The current settings, for the feedback email's diagnostics only -- never event content. */
        val settings: StateFlow<UserSettings> =
            repository.settings.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                UserSettings.DEFAULT,
            )

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
