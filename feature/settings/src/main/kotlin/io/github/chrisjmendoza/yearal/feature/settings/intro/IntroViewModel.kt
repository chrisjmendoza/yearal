package io.github.chrisjmendoza.yearal.feature.settings.intro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.chrisjmendoza.yearal.core.domain.settings.SettingsRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State holder for the first-run intro (`docs/FEATURES.md` L1). The intro's content is entirely static
 * ([IntroFacts]), so this holds no UI state of its own — only the one write-through action every exit
 * path (skip, finish, and the "find my IFC birthday" hook) needs.
 */
@HiltViewModel
class IntroViewModel
    @Inject
    constructor(
        private val repository: SettingsRepository,
    ) : ViewModel() {
        /**
         * Marks the intro seen so [io.github.chrisjmendoza.yearal.ui.IfcApp] never pushes it again on a
         * future launch (`docs/ARCHITECTURE.md` §4 "Screens and navigation"). Idempotent: calling it
         * again (e.g. re-opening the intro from the Learn screen and leaving it a second time) is a
         * harmless no-op write of the same value.
         */
        fun markSeen() {
            viewModelScope.launch { repository.update { it.copy(hasSeenIntro = true) } }
        }
    }
