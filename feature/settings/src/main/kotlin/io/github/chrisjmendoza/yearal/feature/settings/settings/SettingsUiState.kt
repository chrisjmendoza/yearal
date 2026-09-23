package io.github.chrisjmendoza.yearal.feature.settings.settings

import io.github.chrisjmendoza.yearal.core.domain.settings.ColorSource
import io.github.chrisjmendoza.yearal.core.domain.settings.UserSettings

/**
 * Where the "Delete all data" action (FEATURES W6) stands: a two-step destructive confirmation, per
 * `docs/security-and-privacy.md` §2.4, so a single mis-tap can never erase everything.
 */
enum class DeleteAllDataStep {
    /** Nothing is happening; the action row is idle. */
    NONE,

    /** The first confirmation is open: what will be erased. */
    CONFIRM_FIRST,

    /** The second, final confirmation is open: this cannot be undone. */
    CONFIRM_SECOND,

    /** The erase finished; a completion notice is shown. */
    DONE,
}

/** What the Settings screen shows (docs/FEATURES.md W1, W2, H5). */
sealed interface SettingsUiState {
    /** Before the stored settings have been read; DataStore answers within milliseconds. */
    data object Loading : SettingsUiState

    /**
     * The current preferences.
     *
     * @property settings the stored value; every control reflects it and nothing else. Holiday sets
     * (FEATURES H5) are browsed and toggled on the Holidays screen now (ROADMAP M6 T2); this screen
     * only links there, so it no longer needs the bundled pack catalogue itself.
     * @property dynamicColorSupported whether the device can honour [ColorSource.DYNAMIC]
     * (API 31+); when `false` the switch is shown disabled and [UserSettings.colorSource] is left
     * untouched.
     * @property deleteAllDataStep which step of the "Delete all data" confirmation is open, if any
     * (FEATURES W6).
     */
    data class Loaded(
        val settings: UserSettings,
        val dynamicColorSupported: Boolean,
        val deleteAllDataStep: DeleteAllDataStep = DeleteAllDataStep.NONE,
    ) : SettingsUiState
}
