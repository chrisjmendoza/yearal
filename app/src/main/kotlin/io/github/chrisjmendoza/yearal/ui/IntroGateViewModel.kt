package io.github.chrisjmendoza.yearal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.chrisjmendoza.yearal.core.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Answers exactly one question for [IfcApp]: has the first-run intro (`docs/FEATURES.md` L1) been shown
 * yet? Kept separate from [io.github.chrisjmendoza.yearal.MainViewModel] because that ViewModel's own
 * `settings` starts at [io.github.chrisjmendoza.yearal.core.domain.settings.UserSettings.DEFAULT] before
 * the store has actually been read — indistinguishable, by value, from a genuine "not seen" — which
 * would show the intro as a brief flash to every returning user on every cold start. [hasSeenIntro]
 * starts `null` instead, so [IfcApp] can wait for the real, persisted value before deciding whether to
 * push [io.github.chrisjmendoza.yearal.core.navigation.IntroKey].
 */
@HiltViewModel
class IntroGateViewModel
    @Inject
    constructor(
        settingsRepository: SettingsRepository,
    ) : ViewModel() {
        /** `null` until the store has been read at least once; the persisted value after that. */
        val hasSeenIntro: StateFlow<Boolean?> =
            settingsRepository.settings
                .map { it.hasSeenIntro }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
